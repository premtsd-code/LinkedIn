# HA PostgreSQL: etcd + Patroni + HAProxy (two clusters)

Replaces the single `linkedin-db` container with **two independent Patroni
clusters** on a shared etcd quorum — each service group gets its own primary,
its own failover, and its own database + credentials.

```
      user:  5432 write / 5433 read      engagement: 5434 write / 5435 read
  user-service ─────────┐                  ┌───────── post-service
                        ▼                  ▼          notification-service
                  ┌──────────────────────────────┐
                  │            HAProxy           │   stats :7000
                  │  /primary + /replica checks (Patroni REST :8008)
                  └────┬──────────────────┬──────┘
              scope: user-pg      scope: engagement-pg
            ┌─────────┬─────────┐ ┌─────────┬─────────┐
            │ user1   │ user2   │ │ eng1    │ eng2    │   PG16 + Patroni
            │(primary)│(sync    │ │(primary)│(sync    │   independent
            │ userDB  │ replica)│ │ postDB +│ replica)│   leaders/failover
            └────┬────┴────┬────┘ │ notifDB │         │
                 │         │      └────┬────┴────┬────┘
                 ▼         ▼           ▼         ▼
            ┌─────────────────────────────────────────┐
            │        etcd1    etcd2    etcd3          │  shared Raft quorum
            └─────────────────────────────────────────┘
```

## Why this grouping?

- **`user-pg` — user-service alone.** Auth is on the critical path of every
  request; a feed spike or notification fan-out burst must never compete with
  login queries for the same primary. Identity data stays relational forever.
- **`engagement-pg` — post + notification.** Both are event-driven activity
  data: tolerant of replica lag, notification is even rebuildable from Kafka.
  They share a *cluster* but have **separate databases and users**
  (`postDB`/`post_svc`, `notificationDB`/`notification_svc`) — neither can
  touch the other's tables, so the microservice boundary holds.
- **NoSQL exit ramp:** both engagement services are future NoSQL candidates
  (notifications → MongoDB, feed → Cassandra). Swap-readiness lives at the
  app boundary (own DB, own credentials, data access only via repositories,
  cross-service access only via REST/Kafka) — when one moves, drop its
  database from the cluster; the cluster keeps serving the other.
- One Patroni image + one `patroni.yml` serves both clusters; the grouping is
  entirely env vars in compose (`PATRONI_SCOPE`, `PG_INIT_DATABASES`).

## The moving parts

- **etcd (×3)** — one Raft quorum, shared by both clusters. Each Patroni
  scope holds its *own* leader key with a TTL; keys are namespaced by scope,
  so the clusters elect leaders independently. Tolerates 1 etcd failure.
- **Patroni (×2 per cluster)** — supervises each Postgres. Leader renews its
  key; if it dies, the replica promotes itself (independently per cluster).
- **HAProxy** — one instance, one port pair per cluster. It asks Patroni's
  REST API: `GET /primary` → 200 only on that cluster's leader (write ports),
  `GET /replica` → 200 only on streaming replicas (read ports).

## Replication mode: async (+ app-level read-your-writes)

Replication is **asynchronous**: commits return after the local WAL fsync and
the replica catches up on its own (typically well under 100ms here). Fastest
writes; a dead replica never stalls the primary.

Consistency comes from the app layer: `ReadWriteRoutingDataSource` sends
read-only transactions to the read port and everything else to the write
port, and once a request writes, its later reads are pinned to the primary
(`DataSourceContextHolder`) — so a request always sees its own writes.

Known trade-offs of async:
- A *different* request reading from the replica immediately after a write
  may briefly see stale data (bounded by replication lag).
- On failover, commits not yet streamed to the replica are lost — bounded by
  `maximum_lag_on_failover` (1MB of WAL).

To switch to synchronous replication later (zero data-loss failover +
cross-request read-your-writes at the cost of a per-commit round-trip):

```bash
patronictl edit-config <scope> --force \
  -s synchronous_mode=true -s synchronous_node_count=1 \
  -s postgresql.parameters.synchronous_commit=remote_apply
```

## Try breaking it

```bash
alias pctl-user='docker exec patroni-user1 /opt/patroni/bin/patronictl -c /etc/patroni/patroni.yml'
alias pctl-eng='docker exec patroni-eng1 /opt/patroni/bin/patronictl -c /etc/patroni/patroni.yml'

pctl-user list   # user-pg cluster state
pctl-eng list    # engagement-pg cluster state
# Who is primary according to HAProxy?  -> http://localhost:7000

# 1. Kill the engagement primary; user-pg is completely unaffected
docker stop patroni-eng1          # assuming eng1 is that cluster's leader
watch 'docker exec patroni-eng2 /opt/patroni/bin/patronictl -c /etc/patroni/patroni.yml list'
docker start patroni-eng1         # rejoins as sync replica (pg_rewind if needed)

# 2. Planned switchover (per cluster)
pctl-user switchover

# 3. Kill one etcd node — nothing happens (quorum 2/3 holds).
#    Kill two — BOTH clusters demote to read-only: no quorum, no leader locks.
docker stop etcd1 etcd2

# 4. Verify routing per cluster
psql "postgresql://user_svc:password@localhost:5432/userDB" -c "select pg_is_in_recovery();"  # f
psql "postgresql://user_svc:password@localhost:5433/userDB" -c "select pg_is_in_recovery();"  # t
psql "postgresql://post_svc:password@localhost:5434/postDB" -c "select pg_is_in_recovery();"  # f

# 5. Verify isolation: post_svc cannot connect to notificationDB
psql "postgresql://post_svc:password@localhost:5434/notificationDB" -c "select 1;"  # permission denied
```

## Migrating the old data

Old single-node data stays in the `linkedin-db-data` volume. Start the stack,
let each service boot once (Hibernate creates its schema in its own DB), then:

```bash
./scripts/migrate-db-to-patroni.sh   # splits linkedinDB per service
```

Then delete the `linkedin-db` service block from `docker-compose.yml`.

## Notes / limits

- Credentials are hardcoded to match the rest of this compose file — fine for
  a demo, not for production.
- A cluster's read port goes dark if its replica is down; the write port
  keeps working. Point `DB_READER_URL` at the write port as a manual fallback.
- Kafka consumer threads (notification-service) aren't covered by the
  request-scoped filter; after their first write they conservatively stick to
  the primary. Correctness is unaffected.
- The `bootstrap:` section of `patroni.yml` only applies on each cluster's
  first init. Later changes: `patronictl edit-config` (per scope).
