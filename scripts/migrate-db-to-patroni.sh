#!/usr/bin/env bash
# One-time migration: split the old single-node linkedinDB into the
# per-service databases on the two Patroni clusters (via HAProxy):
#   users/role/users_roles -> userDB          (user-pg,       haproxy:5432)
#   posts/post_likes       -> postDB          (engagement-pg, haproxy:5434)
#   notification           -> notificationDB  (engagement-pg, haproxy:5434)
#
# Prerequisites: both `linkedin-db` and the HA stack are up, and each service
# has started once against the new DBs (so Hibernate has created the schema).
# Usage: ./scripts/migrate-db-to-patroni.sh
set -euo pipefail

NETWORK="${NETWORK:-$(docker inspect haproxy --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}')}"

migrate() {
  local tables="$1" url="$2" label="$3"
  local dump_file
  dump_file="$(mktemp "/tmp/${label}-XXXX.sql")"
  local t_flags=""
  for t in $tables; do t_flags="$t_flags -t $t"; done

  echo ">> Dumping [$tables] from linkedin-db..."
  # shellcheck disable=SC2086
  docker exec linkedin-db pg_dump -U user -d linkedinDB \
    --data-only --no-owner --no-privileges --disable-triggers $t_flags > "$dump_file"

  echo ">> Restoring into $label ..."
  docker run --rm -i --network "$NETWORK" postgres:16 \
    psql "$url" -v ON_ERROR_STOP=1 < "$dump_file"
  rm -f "$dump_file"
}

migrate "users role users_roles" "postgresql://user_svc:password@haproxy:5432/userDB"                 "userDB"
migrate "posts post_likes"       "postgresql://post_svc:password@haproxy:5434/postDB"                 "postDB"
migrate "notification"           "postgresql://notification_svc:password@haproxy:5434/notificationDB" "notificationDB"

echo ">> Done. Verify row counts, then remove the linkedin-db service from docker-compose.yml."
echo ">> Note: sequences are dumped with the data; if inserts later fail with"
echo ">> duplicate keys, run: SELECT setval(pg_get_serial_sequence('<table>','id'), (SELECT max(id) FROM <table>));"
