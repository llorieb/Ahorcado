#!/usr/bin/env bash
set -u
cd "$(dirname "$0")"

echo "============================================================"
echo "  AHORCADO 1.0.0 - Verificacion de requisitos de Linux"
echo "============================================================"
echo

ERRORS=0

check_cmd() {
  local cmd="$1"
  local label="$2"
  if command -v "$cmd" >/dev/null 2>&1; then
    echo "[OK] $label: $(command -v "$cmd")"
  else
    echo "[ERROR] No se encontro $label ($cmd)."
    ERRORS=1
  fi
}

check_cmd java "Java"
if command -v java >/dev/null 2>&1; then
  java -version 2>&1 | head -n 3
fi

echo
check_cmd javac "javac"
if command -v javac >/dev/null 2>&1; then
  javac -version 2>&1
fi

echo
check_cmd jpackage "jpackage"
if command -v jpackage >/dev/null 2>&1; then
  echo "[OK] jpackage $(jpackage --version 2>&1)"
fi

echo
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "[AVISO] JAVA_HOME no esta definido. Si java/javac/jpackage son correctos,"
  echo "        el build puede funcionar igualmente."
else
  echo "[OK] JAVA_HOME=$JAVA_HOME"
fi

echo
if [[ -f ./mvnw ]]; then
  chmod +x ./mvnw 2>/dev/null || true
  echo "[OK] Maven Wrapper encontrado. No hace falta instalar Maven manualmente."
else
  echo "[ERROR] No se encontro ./mvnw."
  ERRORS=1
fi

echo
# Requisitos del empaquetado .deb con jpackage en Debian/Ubuntu.
check_cmd dpkg-deb "dpkg-deb"
check_cmd fakeroot "fakeroot"

echo
ARCH="$(uname -m 2>/dev/null || echo desconocida)"
echo "[INFO] Arquitectura detectada: $ARCH"
echo "[INFO] Distribucion: $(grep -E '^PRETTY_NAME=' /etc/os-release 2>/dev/null | cut -d= -f2- | tr -d '"' || echo desconocida)"

echo
if [[ "$ERRORS" -ne 0 ]]; then
  echo "============================================================"
  echo "  FALTAN REQUISITOS. Revisar BUILD_LINUX.md"
  echo "============================================================"
  echo
  echo "En Debian/Ubuntu/Mint, los requisitos de empaquetado se pueden instalar con:"
  echo "  sudo apt update"
  echo "  sudo apt install fakeroot"
  exit 1
fi

echo "============================================================"
echo "  TODO LISTO PARA CREAR LA VERSION LINUX"
echo "============================================================"
exit 0
