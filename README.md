# Gestión de Reportes - Institución Educativa CSR

## Descripción general

Esta aplicación móvil está diseñada para optimizar la gestión de reportes y el mantenimiento en el Colegio Seminario Redentorista. Su objetivo es centralizar la información, reemplazar procesos manuales e informales y reducir la pérdida de datos durante la atención de incidencias.

La plataforma permite registrar solicitudes y consultar el seguimiento de cada reporte. También está pensada para facilitar la comunicación entre la comunidad educativa y el personal encargado de atenderlos.

## Tecnologías

- **Aplicación móvil:** Kotlin y Jetpack Compose.
- **Autenticación y datos en la nube:** Firebase Authentication y Cloud Firestore.
- **Persistencia local:** Room.
- **Red y servicios:** Retrofit, OkHttp y Kotlin Coroutines.
- **Interfaz:** Material 3.
- **Servicios adicionales:** Firebase AI.

## Requisitos

- Android Studio compatible con la configuración Gradle del proyecto.
- JDK 11.
- Android SDK 36.
- Un emulador Android o dispositivo físico con Android 7.0 (API 24) o superior.
- Configuración de Firebase para las funciones que utilizan sus servicios.

## Instalación y ejecución

1. Clona el repositorio:

   ```bash
   git clone https://github.com/matias1020305603-stack/Gestion-de-reportes.git
   ```

2. Abre Android Studio y selecciona **Open**. Elige la carpeta `Gestion-de-reportes`.

3. Espera a que Android Studio sincronice el proyecto y descargue las dependencias de Gradle.

4. Para conectar Firebase, registra la aplicación Android en tu proyecto de Firebase y coloca el archivo `google-services.json` en la carpeta `app/`. Configura en Firebase los servicios que utilice la aplicación, como Authentication y Cloud Firestore.

5. Si vas a usar las funciones que requieren claves de API, crea un archivo `.env` en la raíz del proyecto siguiendo como referencia `.env.example`. No publiques claves privadas en el repositorio.

6. Selecciona un emulador o conecta un dispositivo Android y pulsa **Run** en Android Studio. También puedes compilar desde la terminal:

   ```bash
   ./gradlew assembleDebug
   ```

## Créditos

- **Desarrolladores:** Matías Castaño Ospina y Manolo Bañol Giraldo.
- **Asesor técnico:** Edwin Fabian Rodriguez Valbuena.
- **Institución:** Centro de Formación Redentorista - Técnico Laboral en Manejo de Herramientas para Codificación de Software.
- **Ubicación y año:** Manizales, 2026.
