#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java was not found. Install JDK 17 and configure JAVA_HOME/PATH."
  exit 1
fi

chmod +x ./mvnw
./mvnw clean javafx:run
