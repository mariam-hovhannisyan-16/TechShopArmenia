#!/usr/bin/env bash
#
# Dumps all 7 per-service Postgres databases (see init.sql) to timestamped,
# gzipped .sql files. Run manually, or on a schedule via cron (see the
# "Database backups" section in README.md for a suggested crontab line).
#
# There is currently no other backup strategy for this data - the Postgres
# volume is the only copy. This script is deliberately simple (pg_dump per
# database, kept locally); it does not handle off-server replication,
# retention/rotation beyond the count below, or restore automation.
#
# Usage:
#   ./scripts/backup-db.sh                # dumps into ./backups relative to repo root
#   BACKUP_DIR=/mnt/backups ./scripts/backup-db.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"

BACKUP_DIR="${BACKUP_DIR:-$REPO_ROOT/backups}"
CONTAINER="${POSTGRES_CONTAINER:-techshop-postgres}"
PG_USER="${DB_USERNAME:-postgres}"
KEEP_DAYS="${BACKUP_RETENTION_DAYS:-14}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

DATABASES=(
  techshop_user
  techshop_product
  techshop_cart
  techshop_order
  techshop_notification
  techshop_wishlist
  techshop_chat
)

mkdir -p "$BACKUP_DIR"

for db in "${DATABASES[@]}"; do
  outfile="$BACKUP_DIR/${db}-${TIMESTAMP}.sql.gz"
  echo "Backing up $db -> $outfile"
  docker exec "$CONTAINER" pg_dump -U "$PG_USER" "$db" | gzip > "$outfile"
done

# Drop backups older than the retention window so this doesn't grow unbounded.
find "$BACKUP_DIR" -name "*.sql.gz" -mtime "+${KEEP_DAYS}" -delete

echo "Done. Backups in $BACKUP_DIR (retention: ${KEEP_DAYS} days)."
