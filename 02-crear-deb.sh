#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

./00-verificar-requisitos-linux.sh

if [[ ! -x target/portable-linux/Ahorcado/bin/Ahorcado ]]; then
  echo "[INFO] No existe todavia la imagen portable. Se creara primero."
  ./01-crear-portable-linux.sh
fi

rm -rf target/installer-linux
mkdir -p target/installer-linux

echo
echo "============================================================"
echo "  Creando instalador Debian Ahorcado 1.0.0 (release 2)"
echo "============================================================"

# El recurso Ahorcado.desktop personaliza el lanzador creado por jpackage.
# StartupWMClass debe coincidir con el WM_CLASS real que expone JavaFX para
# que GNOME asocie la ventana en ejecucion con el icono de Ahorcado.
jpackage \
  --type deb \
  --name Ahorcado \
  --app-version 1.0.0 \
  --vendor "Mario Borelli" \
  --copyright "Copyright (c) 1996-2026 Mario Borelli" \
  --description "Juego de Ahorcado" \
  --icon "src/main/resources/images/app-icon.png" \
  --app-image "target/portable-linux/Ahorcado" \
  --dest target/installer-linux \
  --resource-dir "packaging/linux" \
  --linux-package-name ahorcado \
  --linux-app-release 2 \
  --linux-app-category games \
  --linux-menu-group "Game" \
  --linux-shortcut

# Verificacion automatica del .deb: no damos el build por bueno si el
# .desktop empaquetado no contiene la asociacion de ventana correcta.
DEB_FILE="$(find target/installer-linux -maxdepth 1 -type f -name '*.deb' -print -quit)"
if [[ -z "${DEB_FILE}" ]]; then
  echo "[ERROR] jpackage termino pero no se encontro ningun archivo .deb."
  exit 1
fi

VERIFY_DIR="target/installer-linux/.verify"
rm -rf "$VERIFY_DIR"
mkdir -p "$VERIFY_DIR"
dpkg-deb -x "$DEB_FILE" "$VERIFY_DIR"
DESKTOP_FILE="$(find "$VERIFY_DIR" -type f -name '*.desktop' -print -quit)"

if [[ -z "${DESKTOP_FILE}" ]]; then
  echo "[ERROR] El paquete .deb no contiene un archivo .desktop."
  rm -rf "$VERIFY_DIR"
  exit 1
fi

if ! grep -Fxq 'StartupWMClass=com.llorieb.ahorcado.Main' "$DESKTOP_FILE"; then
  echo "[ERROR] El .desktop no contiene el StartupWMClass esperado."
  echo "        Archivo inspeccionado: $DESKTOP_FILE"
  rm -rf "$VERIFY_DIR"
  exit 1
fi

rm -rf "$VERIFY_DIR"

echo
echo "[OK] Asociacion de icono/ventana verificada:"
echo "     StartupWMClass=com.llorieb.ahorcado.Main"
echo
echo "============================================================"
echo "  INSTALADOR .DEB CREADO CORRECTAMENTE"
echo "============================================================"
echo
echo "Paquete generado:"
echo "  ${DEB_FILE}"
echo
echo "Instalacion/actualizacion de prueba:"
echo "  sudo apt install ./${DEB_FILE}"
echo
echo "Desinstalacion:"
echo "  sudo apt remove ahorcado"
