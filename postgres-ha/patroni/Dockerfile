# Patroni-managed PostgreSQL node.
# Postgres 16 + Patroni (etcd3 support) installed in a venv, running as the postgres user.
FROM postgres:16-bookworm

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv python3-pip curl \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m venv /opt/patroni \
    && /opt/patroni/bin/pip install --no-cache-dir --upgrade pip \
    && /opt/patroni/bin/pip install --no-cache-dir "patroni[etcd3]==4.0.5" "psycopg[binary]"

COPY --chmod=644 patroni.yml /etc/patroni/patroni.yml
COPY --chmod=755 post_init.sh /etc/patroni/post_init.sh
# COPY --chmod also stamps the created parent dir; restore traversal for postgres
RUN chmod 755 /etc/patroni

# Patroni requires the data dir to be 0700 and owned by the postgres user;
# it creates /var/lib/postgresql/data/pgdata itself on bootstrap.
USER postgres

ENTRYPOINT ["/opt/patroni/bin/patroni", "/etc/patroni/patroni.yml"]
