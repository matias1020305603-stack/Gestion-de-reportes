# Aplicación Móvil para la Optimización de Gestión de Informes - Colegio Seminario Redentorista (CSR)

## Descripción General
Aplicación móvil nativa para Android respaldada por una base de datos en la nube, diseñada para optimizar la gestión, seguimiento y resolución de informes internos en el **Colegio Seminario Redentorista (CSR)** de Manizales.

Este proyecto reemplaza los procesos manuales e informales (como notas físicas o mensajes de chat) por una herramienta digital centralizada que mejora la eficiencia operativa, evita la pérdida de información, garantiza la trazabilidad de cada caso y agiliza la comunicación entre estudiantes, docentes, personal administrativo y el área de servicios generales.

### Características Principales
* **Registro y gestión de reportes:** Creación de incidencias detalladas sobre fallas o necesidades de mantenimiento institucional y asignación de tareas.
* **Seguimiento en tiempo real:** Visualización del estado exacto de cada solicitud (`Pendiente`, `En proceso`, `Solucionado`) e historial organizado por usuario.
* **Evidencia fotográfica y moderación:** Módulo para adjuntar fotografías como soporte de los reportes, acompañado de validación de seguridad de imágenes y moderación automática de texto.
* **Notificaciones y comunicación:** Alertas automáticas por correo electrónico, chat interno y feed social institucional.
* **Almacenamiento híbrido:** Sincronización en tiempo real en la nube junto con respaldo local en el dispositivo.

---

## Tecnologías, Lenguajes, Frameworks y Dependencias
* **Lenguaje de Programación:** Kotlin
* **Framework de Interfaz (UI):** Jetpack Compose (Material Design) y XML
* **Arquitectura:** MVVM (Model-View-ViewModel) con patrón Repository
* **Base de Datos en la Nube:** Firebase Cloud Firestore y Firebase App Check
* **Base de Datos Local:** Room (SQLite) con DAOs y Entidades locales
* **Sistema de Compilación:** Android Studio, Gradle (Kotlin DSL `.kts`) y catálogo de versiones (`libs.versions.toml`)
* **Pruebas y Validación:** JUnit, Robolectric y Firebase Emulator Suite (`firestore.test.js`)
* **Control de Versiones:** Git y GitHub

---

## Estructura del Repositorio
* `app/src/main/assets/` — Recursos estáticos (Política de Privacidad CSR en PDF).
* `app/src/main/java/com/example/data/` — Base de datos local (Room), modelos y repositorios de Firestore.
* `app/src/main/java/com/example/ui/` — Pantallas (Screens), Componentes, Tema y `MainViewModel`.
* `app/src/main/java/com/example/util/` — Moderación de contenido/imágenes, validadores y servicio de correo.
* `app/src/main/res/` — Recursos gráficos, íconos, avatares y configuraciones XML.
* `gradle/` — Configuración de dependencias (`libs.versions.toml`) y Gradle Wrapper.
* `firestore.rules` y `firestore.test.js` — Reglas de seguridad y pruebas de la base de datos en Cloud Firestore.
* `firebase.json` y `build.gradle.kts` — Configuración de servicios de Firebase y compilación Gradle.
* `README.md` — Documentación principal del proyecto.

---

## Instrucciones de Instalación y Ejecución Local

1. **Clonar el repositorio:**
   Ejecuta en tu terminal:
   `git clone https://github.com/matias1020305603-stack/Gestion-de-reportes.git`

2. **Abrir el proyecto en Android Studio:**
   * Inicia **Android Studio** (versión Hedgehog o superior recomendada).
   * Haz clic en **File > Open** y selecciona la carpeta raíz `Gestion-de-reportes`.
   * Espera a que **Gradle** sincronice el proyecto y descargue automáticamente todas las dependencias configuradas en `gradle/libs.versions.toml`.

3. **Configurar variables de entorno y Firebase:**
   * Crea una copia del archivo `.env.example`, renómbrala a `.env` y completa las credenciales correspondientes.
   * Asegúrate de que el archivo de configuración de Firebase (`app/src/main/res/values/firebase_applet_config.xml` o `google-services.json`) esté correctamente vinculado a la consola de Firebase.

4. **Ejecutar la aplicación:**
   * Conecta un dispositivo Android físico (con la *Depuración USB* habilitada) o inicia un emulador Android desde el **Device Manager**.
   * Presiona el botón **Run** (`Shift + F10`) en Android Studio para compilar e instalar la aplicación.

---

## Créditos y Desarrolladores
Proyecto Integrador de Aula — **Técnico Laboral en Manejo de Herramientas para Codificación de Software**  
*Centro de Formación Redentorista — Manizales, Caldas (2026)*

* **Matías Castaño Ospina** — Desarrollador
* **Manolo Bañol Giraldo** — Desarrollador

**Instructor / Asesor:** Edwin Fabián Rodríguez Valbuena
