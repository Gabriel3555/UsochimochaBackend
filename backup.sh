#!/bin/bash

# ============================================================
# BACKUP USOCHICAMOCHA — VPS
# Cron: 0 3 * * * /root/backup.sh >> /var/log/backup_uso.log 2>&1
# ============================================================

DATE=$(date +%Y-%m-%d_%H-%M-%S)
LOG_PREFIX="[$(date '+%Y-%m-%d %H:%M:%S')]"
BACKUP_ROOT="/var/backups/usochicamocha"
DB_USER="postgres"
RETENTION_DB=30
RETENTION_UPLOADS=7
RETENTION_CONFIG=30
ERRORS=0

# ── Directorios ───────────────────────────────────────────────
mkdir -p "$BACKUP_ROOT/db" "$BACKUP_ROOT/uploads" "$BACKUP_ROOT/config"

echo "$LOG_PREFIX ========== INICIO BACKUP =========="

# ── 1. BASE DE DATOS ─────────────────────────────────────────
for DB_NAME in usochicamochadb; do
    FILE="$BACKUP_ROOT/db/${DB_NAME}-${DATE}.sql.gz"
    echo "$LOG_PREFIX Respaldando BD: $DB_NAME..."

    if pg_dump -h localhost -U "$DB_USER" "$DB_NAME" 2>/dev/null | gzip > "$FILE"; then
        if gzip -t "$FILE" 2>/dev/null && [ $(stat -c%s "$FILE") -gt 1000 ]; then
            echo "$LOG_PREFIX OK BD $DB_NAME — $(du -sh "$FILE" | cut -f1)"
        else
            echo "$LOG_PREFIX ERROR: Backup de $DB_NAME corrupto o vacio"
            rm -f "$FILE"
            ERRORS=$((ERRORS + 1))
        fi
    else
        echo "$LOG_PREFIX ERROR: Fallo pg_dump de $DB_NAME"
        rm -f "$FILE"
        ERRORS=$((ERRORS + 1))
    fi
done

# ── 2. UPLOADS (imagenes) ─────────────────────────────────────
UPLOADS_DIR="/root/UsochimochaBackend/uploads"
UPLOADS_FILE="$BACKUP_ROOT/uploads/uploads-${DATE}.tar.gz"

if [ -d "$UPLOADS_DIR" ]; then
    echo "$LOG_PREFIX Respaldando uploads..."
    if tar -czf "$UPLOADS_FILE" -C "$(dirname $UPLOADS_DIR)" "$(basename $UPLOADS_DIR)" 2>/dev/null; then
        echo "$LOG_PREFIX OK Uploads — $(du -sh "$UPLOADS_FILE" | cut -f1)"
    else
        echo "$LOG_PREFIX ERROR: Fallo backup de uploads"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo "$LOG_PREFIX AVISO: Carpeta uploads no encontrada: $UPLOADS_DIR"
fi

# ── 3. CONFIGURACION DEL SERVIDOR ────────────────────────────
CONFIG_FILE="$BACKUP_ROOT/config/config-${DATE}.tar.gz"
echo "$LOG_PREFIX Respaldando configuracion del servidor..."

tar -czf "$CONFIG_FILE" \
    /etc/systemd/system/usochicamocha-backend.service \
    /etc/nginx/sites-enabled/ \
    /root/.pgpass 2>/dev/null

if [ -f "$CONFIG_FILE" ]; then
    echo "$LOG_PREFIX OK Config — $(du -sh "$CONFIG_FILE" | cut -f1)"
else
    echo "$LOG_PREFIX AVISO: Config backup parcial"
fi

# ── 4. LIMPIEZA ───────────────────────────────────────────────
echo "$LOG_PREFIX Limpiando backups antiguos..."
find "$BACKUP_ROOT/db"      -name "*.sql.gz" -mtime +$RETENTION_DB      -delete
find "$BACKUP_ROOT/uploads" -name "*.tar.gz" -mtime +$RETENTION_UPLOADS  -delete
find "$BACKUP_ROOT/config"  -name "*.tar.gz" -mtime +$RETENTION_CONFIG   -delete

# ── 5. RESUMEN ────────────────────────────────────────────────
echo "$LOG_PREFIX Espacio usado: $(du -sh $BACKUP_ROOT | cut -f1)"
echo "$LOG_PREFIX BDs guardadas: $(ls $BACKUP_ROOT/db/*.sql.gz 2>/dev/null | wc -l) archivos"

if [ $ERRORS -eq 0 ]; then
    echo "$LOG_PREFIX BACKUP COMPLETADO SIN ERRORES"
else
    echo "$LOG_PREFIX BACKUP COMPLETADO CON $ERRORS ERROR(ES) — REVISAR"
fi

echo "$LOG_PREFIX ========== FIN BACKUP =========="
