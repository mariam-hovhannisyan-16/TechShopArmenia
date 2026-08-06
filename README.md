# TechShop Armenia

A Spring Boot microservices e-commerce backend: `techshop-user`, `techshop-product`, `techshop-cart`,
`techshop-order`, `techshop-notification`, `techshop-wishlist`, `techshop-chat`, plus the shared
`techshop-common` library.

## Running locally

**Use `docker-compose` exclusively. Do not run individual services from IntelliJ (or any other
IDE run configuration) while `docker-compose` is also managing them, and do not mix the two.**

```bash
docker compose up -d --build
```

That single command builds all 7 services, starts Postgres, Kafka, and Zookeeper, creates every
per-service database, and runs each service's own schema migration (Liquibase or Hibernate
`ddl-auto`) against a consistent, shared instance. All services register the internal API key,
JWT secret, and inter-service URLs the same way every time, because they're all reading the same
`docker-compose.yml` environment block.

To stop everything:

```bash
docker compose down
```

To reset to a completely clean slate (wipes the Postgres volume — all data, all databases):

```bash
docker compose down -v
docker compose up -d --build
```

Do this whenever you suspect the local Postgres volume is in a state a fresh checkout wouldn't
produce (see "Why not IntelliJ run configs" below for exactly when that happens).

### Service ports

| Service | Port |
|---|---|
| techshop-user | 8081 |
| techshop-cart | 8082 |
| techshop-order | 8083 |
| techshop-product | 8084 |
| techshop-notification | 8085 |
| techshop-wishlist | 8086 |
| techshop-chat | 8087 |
| Postgres (host-mapped) | 5433 → container 5432 |
| Kafka (host-mapped) | 9092 |

Postgres is intentionally mapped to host port **5433**, not 5432, specifically so it doesn't
collide with a Postgres instance you might already have running locally outside Docker.

### Why not IntelliJ run configurations

Running one or more services via an IntelliJ run configuration *at the same time as*
`docker-compose` causes two categories of failure, both of which come from the same root cause —
**a second, independent runtime trying to manage state that `docker-compose` already owns**:

1. **Port conflicts.** An IntelliJ run configuration binds a service to the same host port
   (8081, 8082, 8084, 8085, ...) that the equivalent `docker-compose` container already has bound.
   Spring Boot fails to start with `Web server failed to start. Port XXXX was already in use.`
   The fix isn't to change ports — it's to not run the same service twice.

2. **Missing or inconsistent databases.** `init.sql` creates all 7 per-service databases
   (`techshop_user`, `techshop_product`, `techshop_cart`, `techshop_order`,
   `techshop_notification`, `techshop_wishlist`, `techshop_chat`), but **`docker-entrypoint-initdb.d`
   scripts only run once, the first time a Postgres container initializes an empty data
   directory.** If your local `postgres_data` volume was created before `techshop_chat` or
   `techshop_wishlist` existed (or before some other schema change landed), that volume will
   never get the missing databases or the missing schema, no matter how many times you restart
   the container — `init.sql` simply doesn't run again against a non-empty volume. Running a
   service from IntelliJ against `localhost:5433` doesn't fix this either, since it's hitting the
   exact same stale volume. The only fix is `docker compose down -v` (see above) to force
   Postgres to re-initialize from scratch.

If you genuinely need to debug a single service step-by-step in IntelliJ, stop *only that
service's* container first so nothing else is fighting it for the port or the database:

```bash
docker compose stop techshop-order
```

Then run `TechShopOrderApplication` from IntelliJ with `DB_HOST=localhost`, `DB_PORT=5433`, and
`KAFKA_BOOTSTRAP_SERVERS=localhost:9092` (the `application.yml` defaults already point here) so it
talks to the *same* Postgres/Kafka containers the rest of the stack is using — not a second,
divergent local instance. Restart the container (`docker compose up -d techshop-order`) when
you're done so the stack goes back to a single source of truth.

### Schema migrations

- `techshop-order`, `techshop-chat`, `techshop-wishlist`, and `techshop-user` use **Liquibase**
  (`ddl-auto: validate` — Liquibase owns the schema, Hibernate only checks the entities match it).
  `techshop-user`'s changelog includes a guarded `createTable` changeset so it creates the
  `users` table from scratch on a fresh database, in addition to the dedupe/unique-constraint
  changesets that assume the table already exists on an older deployment.
- `techshop-product` and `techshop-cart` use plain **Hibernate `ddl-auto: update`**; so does
  `techshop-notification`.
- `techshop-product` additionally ships a small Liquibase changelog
  (`techshop-product/src/main/resources/db/changelog`) that runs *before* Hibernate's own schema
  update, solely to backfill NULLs in `products.is_new`/`products.stock` on any database that
  predates those columns being non-nullable — Hibernate can add a `NOT NULL` constraint, but it
  can't backfill existing rows first, so without this step `ddl-auto: update` fails outright
  against a table with existing NULL values in those columns.

### Health check

`scripts/health-check.sh` checks that all 7 services are accepting connections and responding to
HTTP requests (any status code counts as "up" - none of these services expose Spring Boot
Actuator, so a 401/403 from an auth-protected endpoint is still a valid sign the service is
running; only a connection failure or timeout counts as "down"). Run it manually:

```bash
./scripts/health-check.sh                    # checks localhost
HOST=<server-ip-or-domain> ./scripts/health-check.sh   # checks a remote host
```

It exits non-zero if any service failed to respond, so it can be wired into cron with a mailer -
see the comment at the top of the script for a ready-to-use crontab line.

### Database backups

There is currently no backup strategy beyond the Postgres data volume itself - if that volume is
ever lost or corrupted, all data (users, orders, products, everything) goes with it. At minimum,
back it up periodically with `scripts/backup-db.sh`, which runs `pg_dump` against each of the 7
per-service databases and writes timestamped, gzipped dumps (kept for 14 days by default) to a
local `backups/` directory - deliberately gitignored, since dumps contain real user data:

```bash
./scripts/backup-db.sh                        # dumps into ./backups
BACKUP_DIR=/mnt/backups ./scripts/backup-db.sh # dump somewhere else, e.g. a mounted volume
```

Suggested crontab entry for a nightly backup at 3am on the server:

```
0 3 * * * cd /home/ubuntu/TechShopArmenia && ./scripts/backup-db.sh >> /var/log/techshop-backup.log 2>&1
```

This is a minimum-viable starting point, not a complete disaster-recovery plan: dumps are stored
on the same host as the database they're backing up, so they don't protect against the server
itself being lost (only against a bad migration, accidental data deletion, or Postgres-level
corruption). Copying the `backups/` directory off-host (e.g. to S3) on the same schedule is the
natural next step.
