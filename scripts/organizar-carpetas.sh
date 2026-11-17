#!/usr/bin/env bash
# Organiza las carpetas auxiliares preservando la estructura requerida por Android/Gradle.
# Ejecutar desde la raíz del repositorio: bash scripts/organizar-carpetas.sh
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "Error: ejecuta este script dentro de un clon Git del proyecto." >&2
  exit 1
}
cd "$ROOT"

# Verifica que no haya cambios locales que puedan confundirse con los movimientos.
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Error: hay cambios locales. Guárdalos o haz commit antes de continuar." >&2
  exit 1
fi

# Los archivos Kotlin y recursos que Gradle compila ya deben estar en app/src/main.
# Las copias cargadas en la raíz se conservan como material heredado, sin mezclarlas
# con el código que realmente compila.
if [[ -d java ]]; then
  mkdir -p legacy-upload
  if [[ -e legacy-upload/java ]]; then
    echo "Error: legacy-upload/java ya existe; no se sobrescribió." >&2
    exit 1
  fi
  git mv java legacy-upload/java
fi

if [[ -d res ]]; then
  mkdir -p legacy-upload
  if [[ -e legacy-upload/res ]]; then
    echo "Error: legacy-upload/res ya existe; no se sobrescribió." >&2
    exit 1
  fi
  git mv res legacy-upload/res
fi

# Mantener firebase.json en la raíz, donde Firebase CLI lo busca; agrupar las reglas
# y pruebas y actualizar la ruta de reglas en la configuración.
if [[ -f firestore.rules || -f firestore.test.js ]]; then
  mkdir -p database
  [[ ! -e firestore.rules ]] || git mv firestore.rules database/firestore.rules
  [[ ! -e firestore.test.js ]] || git mv firestore.test.js database/firestore.test.js
fi

if [[ -f firebase.json ]]; then
  python3 - <<'PY'
from pathlib import Path
p = Path("firebase.json")
text = p.read_text()
text = text.replace('"rules": "firestore.rules"', '"rules": "database/firestore.rules"')
p.write_text(text)
PY
  git add firebase.json
fi

# Los ZIP son paquetes de archivos de trabajo, no parte del código Android compilado.
for archive in components_and_utils.zip screens.zip viewmodel_and_data.zip; do
  if [[ -f "$archive" ]]; then
    mkdir -p legacy-upload/archives
    git mv "$archive" legacy-upload/archives/
  fi
done

echo "Organización preparada. Revisa los cambios con: git status --short"
echo "Los directorios app/src/main/java y app/src/main/res se mantienen para Gradle."
