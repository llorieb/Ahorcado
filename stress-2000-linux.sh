#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
echo "Ejecutando prueba manual de 2000 partidas..."
./mvnw -Dahorcado.stress=true -Dtest=PartidasStressTest test
