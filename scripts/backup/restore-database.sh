#!/usr/bin/env bash
#
# PostgreSQL Restore Script for OSCAL Tools
#
# Usage:
#   ./restore-database.sh <backup_file>
#
# WARNING: This will OVERWRITE the current database!

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-oscal_dev}"
DB_USERNAME="${DB_USERNAME:-oscal_user}"

# Check argument
if [ $# -eq 0 ]; then
    log_error "Usage: $0 <backup_file>"
    exit 1
fi

BACKUP_FILE="$1"

# Validate backup file
if [ ! -f "$BACKUP_FILE" ]; then
    log_error "Backup file not found: $BACKUP_FILE"
    exit 1
fi

# Warning
log_warn "⚠️  WARNING: This will OVERWRITE the database: $DB_NAME"
log_warn "⚠️  All current data will be LOST!"
echo
read -p "Are you sure you want to continue? (yes/no): " -r
if [[ ! $REPLY =~ ^yes$ ]]; then
    log_info "Restore cancelled"
    exit 0
fi

log_info "Starting restore from: $BACKUP_FILE"

# Decompress if needed
TEMP_FILE=""
if [[ "$BACKUP_FILE" == *.gz ]]; then
    log_info "Decompressing backup..."
    TEMP_FILE="/tmp/oscal_restore_$(date +%s).sql"
    gunzip -c "$BACKUP_FILE" > "$TEMP_FILE"
    BACKUP_FILE="$TEMP_FILE"
fi

# Restore via Docker or direct
if docker-compose exec -T postgres pg_isready > /dev/null 2>&1; then
    log_info "Restoring via Docker..."
    cat "$BACKUP_FILE" | docker-compose exec -T postgres psql -U "$DB_USERNAME" "$DB_NAME"
else
    log_info "Restoring directly..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" "$DB_NAME" < "$BACKUP_FILE"
fi

# Cleanup
if [ -n "$TEMP_FILE" ]; then
    rm "$TEMP_FILE"
fi

log_info "✅ Restore complete!"
