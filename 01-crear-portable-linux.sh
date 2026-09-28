#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

./00-verificar-requisitos-linux.sh

echo
echo "============================================================"
echo "  1/3 - Compilando Ahorcado 1.0.1 con Maven"
echo "============================================================"
chmod +x ./mvnw
./mvnw clean package -DskipTests

rm -rf target/portable-linux
mkdir -p target/portable-linux

echo
echo "============================================================"
echo "  2/3 - Creando aplicacion portable autocontenida"
echo "============================================================"

jpackage \
  --type app-image \
  --name Ahorcado \
  --app-version 1.0.1 \
  --vendor "Mario Borelli" \
  --copyright "Copyright (c) 1996-2026 Mario Borelli" \
  --description "Juego de Ahorcado" \
  --icon "src/main/resources/images/app-icon.png" \
  --module-path "target/Ahorcado-1.0.1.jar:target/dependency" \
  --module "com.llorieb.ahorcado/com.llorieb.ahorcado.Main" \
  --dest target/portable-linux

echo
echo "============================================================"
echo "  3/3 - Creando TAR.GZ portable para distribucion"
echo "============================================================"

ARCH="$(uname -m)"
TAR_NAME="Ahorcado-1.0.1-linux-${ARCH}.tar.gz"
tar -C target/portable-linux -czf "target/portable-linux/${TAR_NAME}" Ahorcado

echo
echo "============================================================"
echo "  PORTABLE LINUX CREADO CORRECTAMENTE"
echo "============================================================"
echo
echo "Ejecutable:"
echo "  target/portable-linux/Ahorcado/bin/Ahorcado"
echo
echo "Paquete portable para GitHub/web:"
echo "  target/portable-linux/${TAR_NAME}"
echo
echo "Probar el ejecutable antes de crear el instalador .deb."
