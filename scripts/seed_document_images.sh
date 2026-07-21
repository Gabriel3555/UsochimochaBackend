#!/usr/bin/env bash
# =============================================================================
# seed_document_images.sh
# Genera imágenes placeholder de documentos (SOAT / TECNOMECÁNICA) para los
# 30 vehículos/motos del seeder y produce un archivo SQL de UPDATE.
#
# Uso (desde la raíz del backend):
#   bash scripts/seed_document_images.sh
#
# Salida:
#   uploads/documents/vehicles/{id}/{TIPO}/current.jpg  — imagen por documento
#   scripts/seed_document_images_update.sql             — UPDATE para BD
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
UPLOADS_DIR="$BACKEND_DIR/uploads/documents/vehicles"
SQL_OUT="$SCRIPT_DIR/seed_document_images_update.sql"

# ---------------------------------------------------------------------------
# Función que genera una imagen de documento con ImageMagick
#   $1 = tipo     ("SOAT" | "TECNOMECANICA")
#   $2 = placa    ("OGD742")
#   $3 = fecha    ("28/02/2027")
#   $4 = estado   ("VIGENTE" | "PRÓXIMO A VENCER" | "VENCIDO")
#   $5 = destino  ruta absoluta del JPG
# ---------------------------------------------------------------------------
gen_doc() {
  local tipo="$1" placa="$2" fecha="$3" estado="$4" dest="$5"
  mkdir -p "$(dirname "$dest")"

  # Colores según tipo
  local header_color label
  if   [[ "$tipo" == "SOAT" ]];           then header_color="#005A9C"; label="SEGURO OBLIGATORIO DE ACCIDENTES DE TRÁNSITO"
  elif [[ "$tipo" == "TECNOMECANICA" ]];  then header_color="#1A6B3A"; label="REVISIÓN TÉCNICO-MECÁNICA Y DE EMISIONES"
  else                                         header_color="#555555"; label="$tipo"
  fi

  # Color de la pastilla de estado
  local badge_color
  if   [[ "$estado" == "VIGENTE" ]];             then badge_color="#1A6B3A"
  elif [[ "$estado" == "PRÓXIMO A VENCER" ]];    then badge_color="#B8860B"
  else                                                badge_color="#A00000"
  fi

  magick \
    -size 800x480 xc:white \
    \
    `# Cabecera azul/verde` \
    -fill "$header_color" \
    -draw "rectangle 0,0 800,110" \
    \
    `# Franja lateral izquierda decorativa` \
    -fill "$header_color" \
    -draw "rectangle 0,110 12,480" \
    \
    `# Línea separadora suave` \
    -fill "#DDDDDD" \
    -draw "rectangle 0,110 800,112" \
    \
    `# Título en cabecera` \
    -font "DejaVu-Sans-Bold" -pointsize 18 -fill white \
    -annotate +30+45  "REPÚBLICA DE COLOMBIA" \
    -font "DejaVu-Sans-Bold" -pointsize 15 -fill "#AADDFF" \
    -annotate +30+72  "$label" \
    -font "DejaVu-Sans-Bold" -pointsize 22 -fill "#FFD700" \
    -annotate +30+100 "MINISTERIO DE TRANSPORTE" \
    \
    `# Número de placa — grande` \
    -font "DejaVu-Sans-Bold" -pointsize 52 -fill "$header_color" \
    -annotate +50+195 "$placa" \
    \
    `# Etiqueta placa` \
    -font "DejaVu-Sans" -pointsize 11 -fill "#888888" \
    -annotate +52+215 "NÚMERO DE PLACA / MATRICULA" \
    \
    `# Fecha de vencimiento` \
    -font "DejaVu-Sans-Bold" -pointsize 14 -fill "#222222" \
    -annotate +52+270 "FECHA DE VENCIMIENTO" \
    -font "DejaVu-Sans-Bold" -pointsize 28 -fill "#222222" \
    -annotate +52+310 "$fecha" \
    \
    `# Pastilla de estado` \
    -fill "$badge_color" \
    -draw "roundRectangle 52,340 320,390 10,10" \
    -font "DejaVu-Sans-Bold" -pointsize 20 -fill white \
    -annotate +80+372 "$estado" \
    \
    `# Texto legal de pie` \
    -font "DejaVu-Sans" -pointsize 10 -fill "#AAAAAA" \
    -annotate +30+440 "Documento generado para entorno de pruebas — Usochicamocha Fleet Management" \
    -annotate +30+455 "No válido para uso oficial · Solo datos de demostración" \
    \
    -quality 88 "$dest"
}

# ---------------------------------------------------------------------------
# Datos: (id_vehiculo, placa, tipo, fecha_str, estado)
# ---------------------------------------------------------------------------
declare -a DOCS=(
  # Vehículos — SOAT
  "1|OGD742|SOAT|28/02/2027|VIGENTE"
  "2|MIF301|SOAT|15/06/2026|PRÓXIMO A VENCER"
  "3|KBH055|SOAT|30/11/2025|VENCIDO"
  "4|NCP187|SOAT|15/03/2027|VIGENTE"
  "5|QHE412|SOAT|01/07/2026|PRÓXIMO A VENCER"
  "6|VLK993|SOAT|20/12/2026|VIGENTE"
  "7|RTA628|SOAT|10/04/2027|VIGENTE"
  "8|BJF714|SOAT|31/05/2026|PRÓXIMO A VENCER"
  "9|ZPA559|SOAT|15/01/2027|VIGENTE"
  "10|HDG223|SOAT|05/10/2026|VIGENTE"
  "11|XLB481|SOAT|30/08/2026|VIGENTE"
  "12|CFR076|SOAT|20/04/2026|VENCIDO"
  "13|PPZ340|SOAT|01/05/2027|VIGENTE"
  "14|TUN882|SOAT|20/09/2026|VIGENTE"
  "15|WDB164|SOAT|30/11/2026|VIGENTE"
  # Vehículos — TECNOMECANICA
  "1|OGD742|TECNOMECANICA|10/09/2026|VIGENTE"
  "2|MIF301|TECNOMECANICA|20/01/2027|VIGENTE"
  "3|KBH055|TECNOMECANICA|05/08/2026|VIGENTE"
  "4|NCP187|TECNOMECANICA|30/11/2026|VIGENTE"
  "5|QHE412|TECNOMECANICA|15/10/2026|VIGENTE"
  "6|VLK993|TECNOMECANICA|30/06/2026|PRÓXIMO A VENCER"
  "7|RTA628|TECNOMECANICA|28/02/2027|VIGENTE"
  "8|BJF714|TECNOMECANICA|01/09/2026|VIGENTE"
  "9|ZPA559|TECNOMECANICA|20/07/2026|PRÓXIMO A VENCER"
  "10|HDG223|TECNOMECANICA|15/12/2025|VENCIDO"
  "11|XLB481|TECNOMECANICA|10/11/2026|VIGENTE"
  "12|CFR076|TECNOMECANICA|10/06/2026|PRÓXIMO A VENCER"
  "13|PPZ340|TECNOMECANICA|01/12/2026|VIGENTE"
  "14|TUN882|TECNOMECANICA|15/07/2026|PRÓXIMO A VENCER"
  "15|WDB164|TECNOMECANICA|01/03/2027|VIGENTE"
  # Motos — SOAT
  "16|LSP40G|SOAT|10/08/2026|VIGENTE"
  "17|BCM20H|SOAT|05/06/2026|PRÓXIMO A VENCER"
  "18|DKN74P|SOAT|25/01/2027|VIGENTE"
  "19|EQR35J|SOAT|28/05/2026|PRÓXIMO A VENCER"
  "20|FVS91K|SOAT|15/10/2025|VENCIDO"
  "21|GXT47L|SOAT|30/04/2027|VIGENTE"
  "22|HZY62M|SOAT|20/09/2026|VIGENTE"
  "23|JBC18N|SOAT|10/07/2026|PRÓXIMO A VENCER"
  "24|KDF83P|SOAT|15/02/2027|VIGENTE"
  "25|LGH29Q|SOAT|25/08/2026|VIGENTE"
  "26|MJK54R|SOAT|01/06/2026|PRÓXIMO A VENCER"
  "27|NML70S|SOAT|01/04/2026|VENCIDO"
  "28|OPN25T|SOAT|20/03/2027|VIGENTE"
  "29|PQR11U|SOAT|30/12/2025|VENCIDO"
  "30|QSU66V|SOAT|10/10/2026|VIGENTE"
)

# ---------------------------------------------------------------------------
# Generar imágenes y construir SQL
# ---------------------------------------------------------------------------
echo "Generando imágenes en: $UPLOADS_DIR"

cat > "$SQL_OUT" <<'SQLHEADER'
-- =============================================================================
-- seed_document_images_update.sql
-- UPDATE de imagen_url para documentacion_y_elementos
-- Ejecutar DESPUÉS de seed_vehiculos_motos.sql y seed_document_images.sh
-- =============================================================================
BEGIN;
SQLHEADER

total=0
for entry in "${DOCS[@]}"; do
  IFS='|' read -r vid placa tipo fecha estado <<< "$entry"
  dest="$UPLOADS_DIR/$vid/$tipo/current.jpg"
  rel="/uploads/documents/vehicles/$vid/$tipo/current.jpg"

  echo "  → V$vid ($placa) $tipo [$estado]"
  gen_doc "$tipo" "$placa" "$fecha" "$estado" "$dest"

  # SQL UPDATE — coincide por id_vehiculo + tipo_documento
  cat >> "$SQL_OUT" <<SQL
UPDATE documentacion_y_elementos
   SET imagen_url = '$rel'
 WHERE id_vehiculo = $vid
   AND UPPER(TRIM(tipo_documento)) = '${tipo}';
SQL
  (( total++ )) || true
done

cat >> "$SQL_OUT" <<'SQLFOOTER'
COMMIT;
SQLFOOTER

echo ""
echo "✓ $total imágenes generadas en uploads/documents/vehicles/"
echo "✓ SQL escrito en: scripts/seed_document_images_update.sql"
echo ""
echo "Aplicar a la BD:"
echo "  PGPASSWORD=nala psql -h localhost -U postgres -d postgres \\"
echo "    -v ON_ERROR_STOP=1 -f scripts/seed_document_images_update.sql"
