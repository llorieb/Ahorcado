#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "============================================================"
echo "  AHORCADO 1.0.0 - BUILD LINUX COMPLETO"
echo "============================================================"

./01-crear-portable-linux.sh
./02-crear-deb.sh

echo
echo "============================================================"
echo "  BUILD LINUX FINALIZADO"
echo "============================================================"
echo
echo "Portable:"
echo "  target/portable-linux/Ahorcado/bin/Ahorcado"
echo "  target/portable-linux/Ahorcado-1.0.0-linux-*.tar.gz"
echo
echo "Instalador Debian/Ubuntu/Mint:"
echo "  target/installer-linux/*.deb"
