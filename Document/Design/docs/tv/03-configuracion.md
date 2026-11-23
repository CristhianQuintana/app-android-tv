# 03. Configuración del proyecto

**Estado:** guía por completar tras inspeccionar el repositorio.

## Requisitos
Android Studio, JDK y Gradle compatibles con el proyecto, Android SDK correspondiente, emulador o dispositivo TV autorizado, repositorio y acceso al backend de pruebas.

## Inspección antes de cambios
Registrar rama y estado Git, archivos Gradle, Gradle Wrapper, Android Manifest, Kotlin, Android Gradle Plugin, `minSdk`, `compileSdk`, `targetSdk`, Application ID, dependencias, build variants y resultado de compilación inicial. Preservar cambios existentes.

## Entornos
- Separar configuración de desarrollo/pruebas y release.
- La URL de API debe configurarse por entorno, no estar dispersa en pantallas.
- No incluir contraseñas, tokens, claves privadas ni credenciales de XUI/Astra/VOD en el repositorio o APK.
- No desactivar validación TLS para resolver errores.
- Usar HTTPS en redes no confiables.

## Comandos de verificación
Desde la raíz del proyecto, ajustando el módulo cuando corresponda:

```bash
./gradlew --version
./gradlew tasks
./gradlew test
./gradlew assembleDebug
```

En Windows:

```powershell
.\gradlew.bat --version
.\gradlew.bat tasks
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

No asumir éxito antes de ejecutar; guardar resultados saneados.

## Instalación de prueba
1. Confirmar dispositivo/emulador.
2. Generar APK de prueba.
3. Instalar desde Android Studio o con `adb install -r <ruta-apk>`.
4. Abrir y comprobar el inicio.
5. Verificar que apunta al backend de pruebas.
6. Ejecutar login, catálogo y reproducción cuando contratos estén disponibles.
7. Registrar modelo, versión Android, versión app y resultado.

## Registro pendiente
| Dato | Estado |
|---|---|
| Rama y commit | Por verificar |
| Android Studio/JDK/Gradle/Kotlin | Por verificar |
| SDKs y Application ID | Por verificar |
| Dispositivo objetivo | Por confirmar |
| Compilación inicial | Por ejecutar |
