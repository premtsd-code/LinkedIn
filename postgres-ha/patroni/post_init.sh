#!/bin/bash
# Called by Patroni exactly once, right after a cluster's primary is
# bootstrapped. $1 is a superuser connection string to the local postgres.
#
# Which databases/owners to create comes from the PG_INIT_DATABASES env var,
# space-separated "dbname:owner:password" entries, e.g.
#   PG_INIT_DATABASES="postDB:post_svc:password notificationDB:notification_svc:password"
# Each owner only gets rights on its own database, so services sharing a
# cluster still can't touch each other's tables.
set -euo pipefail

for spec in ${PG_INIT_DATABASES:?PG_INIT_DATABASES must be set}; do
  IFS=':' read -r db owner pass <<< "$spec"
  echo "post_init: creating database '$db' owned by '$owner'"
  psql "$1" -v ON_ERROR_STOP=1 <<EOSQL
CREATE USER "$owner" WITH PASSWORD '$pass';
CREATE DATABASE "$db" OWNER "$owner";
REVOKE ALL ON DATABASE "$db" FROM PUBLIC;
GRANT ALL PRIVILEGES ON DATABASE "$db" TO "$owner";
EOSQL
done
