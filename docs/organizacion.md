# Organización de carpetas

La aplicación Android conserva la estructura que Gradle requiere:

- Código Kotlin: `app/src/main/java/`
- Recursos Android: `app/src/main/res/` y `app/src/main/assets/`
- Pruebas: `app/src/test/` y `app/src/androidTest/`

El script `scripts/organizar-carpetas.sh` ordena las carpetas y archivos auxiliares que estaban en la raíz. Mueve las copias raíz `java/` y `res/` a `legacy-upload/`, agrupa reglas y pruebas de Firestore en `database/` (mantiene `firebase.json` en la raíz y actualiza la ruta de reglas), y archiva los ZIP en `legacy-upload/archives/`.

Para ejecutar los movimientos desde un clon local:

```bash
bash scripts/organizar-carpetas.sh
```

El script se detiene si detecta cambios locales sin guardar o si encuentra conflictos de destino. Después de ejecutarlo, revisa `git status --short`, compila con `./gradlew assembleDebug` y confirma los cambios cuando estés conforme.
