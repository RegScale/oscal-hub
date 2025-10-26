#!/usr/bin/env bash
#
# PostgreSQL Backup Script for OSCAL Tools
#
# This script creates a timestamped backup of the PostgreSQL database
# and optionally uploads it to cloud storage.
#
# Usage:
#   ./backup-database.sh [options]
#
# Options:
#   -h, --help          Show this help message
#   -c, --compress      Compress the backup (default: yes)
#   -r, --retention N   Keep last N backups (default: 7)
#   -u, --upload        Upload to cloud storage (requires setup)
#
# Environment Variables:
#   DB_HOST            Database host (default: localhost)
#   DB_PORT            Database port (default: 5432)
#   DB_NAME            Database name (default: oscal_dev)
#   DB_USERNAME        Database username (default: oscal_user)
#   BACKUP_DIR         Backup directory (default: ./backups)
#   CLOUD_BACKUP_BUCKET  S3/Azure bucket for backups (optional)

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Defaults
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-oscal_dev}"
DB_USERNAME="${DB_USERNAME:-oscal_user}"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups/database}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
COMPRESS=true
UPLOAD=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_help() {
    head -n 20 "$0" | grep "^#" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check for pg_dump
    if ! command -v pg_dump &> /dev/null; then
        log_error "pg_dump not found. Please install PostgreSQL client tools."
        exit 1
    fi

    # Check if database is accessible
    if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" > /dev/null 2>&1; then
        log_error "Cannot connect to database at $DB_HOST:$DB_PORT"
        log_info "Trying Docker container..."

        # Try via Docker if local connection fails
        if docker-compose exec -T postgres pg_isready > /dev/null 2>&1; then
            log_info "Found database in Docker container"
            USE_DOCKER=true
        else
            log_error "Database is not accessible"
            exit 1
        fi
    fi

    log_info "Prerequisites check passed"
}

create_backup() {
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="$BACKUP_DIR/oscal_backup_${timestamp}.sql"

    log_info "Creating backup: $backup_file"

    # Create backup directory if it doesn't exist
    mkdir -p "$BACKUP_DIR"

    # Perform backup
    if [ "${USE_DOCKER:-false}" = true ]; then
        # Backup via Docker
        log_info "Backing up via Docker container..."
        docker-compose exec -T postgres pg_dump -U "$DB_USERNAME" "$DB_NAME" > "$backup_file"
    else
        # Direct backup
        log_info "Backing up database $DB_NAME..."
        PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" "$DB_NAME" > "$backup_file"
    fi

    # Check if backup was successful
    if [ ! -s "$backup_file" ]; then
        log_error "Backup file is empty or was not created"
        exit 1
    fi

    local file_size=$(du -h "$backup_file" | cut -f1)
    log_info "Backup created successfully (Size: $file_size)"

    # Compress if requested
    if [ "$COMPRESS" = true ]; then
        log_info "Compressing backup..."
        gzip "$backup_file"
        backup_file="${backup_file}.gz"
        local compressed_size=$(du -h "$backup_file" | cut -f1)
        log_info "Backup compressed (Size: $compressed_size)"
    fi

    echo "$backup_file"
}

cleanup_old_backups() {
    log_info "Cleaning up backups older than $RETENTION_DAYS days..."

    local deleted_count=0
    while IFS= read -r -d '' file; do
        rm "$file"
        ((deleted_count++))
        log_info "Deleted old backup: $(basename "$file")"
    done < <(find "$BACKUP_DIR" -name "oscal_backup_*.sql*" -type f -mtime +$RETENTION_DAYS -print0)

    if [ $deleted_count -eq 0 ]; then
        log_info "No old backups to clean up"
    else
        log_info "Deleted $deleted_count old backup(s)"
    fi
}

upload_to_cloud() {
    local backup_file="$1"

    if [ -z "${CLOUD_BACKUP_BUCKET:-}" ]; then
        log_warn "CLOUD_BACKUP_BUCKET not set. Skipping cloud upload."
        return 0
    fi

    log_info "Uploading to cloud storage: $CLOUD_BACKUP_BUCKET"

    # Check which cloud provider (AWS S3 or Azure Blob)
    if [ -n "${AWS_ACCESS_KEY_ID:-}" ]; then
        # AWS S3
        if command -v aws &> /dev/null; then
            aws s3 cp "$backup_file" "s3://$CLOUD_BACKUP_BUCKET/backups/$(basename "$backup_file")"
            log_info "Uploaded to AWS S3 successfully"
        else
            log_warn "AWS CLI not found. Skipping S3 upload."
        fi
    elif [ -n "${AZURE_STORAGE_CONNECTION_STRING:-}" ]; then
        # Azure Blob Storage
        if command -v az &> /dev/null; then
            az storage blob upload \
                --container-name "$CLOUD_BACKUP_BUCKET" \
                --file "$backup_file" \
                --name "backups/$(basename "$backup_file")"
            log_info "Uploaded to Azure Blob Storage successfully"
        else
            log_warn "Azure CLI not found. Skipping Azure upload."
        fi
    else
        log_warn "No cloud provider credentials found. Skipping cloud upload."
    fi
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            ;;
        -c|--compress)
            COMPRESS=true
            shift
            ;;
        -r|--retention)
            RETENTION_DAYS="$2"
            shift 2
            ;;
        -u|--upload)
            UPLOAD=true
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            ;;
    esac
done

# Main execution
main() {
    log_info "=== OSCAL Tools Database Backup ==="
    log_info "Database: $DB_NAME@$DB_HOST:$DB_PORT"
    log_info "Backup directory: $BACKUP_DIR"
    log_info "Retention: $RETENTION_DAYS days"
    echo

    check_prerequisites

    backup_file=$(create_backup)

    cleanup_old_backups

    if [ "$UPLOAD" = true ]; then
        upload_to_cloud "$backup_file"
    fi

    log_info ""
    log_info "=== Backup Complete ==="
    log_info "Backup file: $backup_file"
    log_info "To restore: ./scripts/backup/restore-database.sh $backup_file"
}

main
