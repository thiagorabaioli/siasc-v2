#!/bin/bash
# Backup diário do siac-postgres via pg_dump, dentro do próprio container.
# Uso: scripts/backup.sh (correr com o utilizador que tem acesso a docker)
# Cron sugerido (não instalado automaticamente):
#   0 3 * * * /home/rpi5-server/siac/scripts/backup.sh >> ~/backups_local/siac/backup.log 2>&1
set -euo pipefail

CONTAINER=siac-postgres
BACKUP_DIR="${HOME}/backups_local/siac"
RETENTION_DAYS=14
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
DEST="${BACKUP_DIR}/siac-${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

if ! docker ps --filter "name=^/${CONTAINER}$" --filter "status=running" --format '{{.Names}}' | grep -qx "$CONTAINER"; then
    echo "[$(date -Iseconds)] ${CONTAINER} não está a correr, backup cancelado" >&2
    exit 1
fi

docker exec "$CONTAINER" pg_dump -U "${POSTGRES_USER:-siac_admin}" -d "${POSTGRES_DB:-siac}" | gzip > "$DEST"

echo "[$(date -Iseconds)] backup criado em ${DEST}"

find "$BACKUP_DIR" -name 'siac-*.sql.gz' -mtime "+${RETENTION_DAYS}" -delete
