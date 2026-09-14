# Instrucciones y Reglas de Trabajo para Agentes (AGENTS.md)

Este archivo contiene las directivas prioritarias y mandatorias para cualquier agente de IA que trabaje en el repositorio de **Sonora**. Las instrucciones aquí descritas se aplican de forma continua y tienen precedencia en el flujo de desarrollo.

---

## 1. Directivas de Razonamiento y Toma de Decisiones

- **Razonamiento Previo Obligatorio**: Antes de modificar cualquier archivo o ejecutar acciones, el agente debe razonar metódicamente sobre qué herramientas empleará, qué impacto tendrán los cambios y verificar que no se rompa la compatibilidad.
- **Enfoque en el Usuario Real**: El usuario final utiliza exclusivamente un **teléfono móvil** (no una PC). La experiencia de usuario, tamaños de botones (mínimo 48dp), legibilidad bajo luz solar y ergonomía con una mano son fundamentales.
- **Distribución en Tiendas Independientes (Uptodown / APK Sideload)**: La app no se publica en Google Play Store; por ende, no se deben forzar dependencias o servicios propietarios de Google Play (GMS). La app debe ser 100% autónoma y funcional offline.

---

## 2. Reglas de Código Nativo (C++ y Rust)

- **Inclusión Total en Gradle**: Si el proyecto utiliza C++, Rust u otros lenguajes nativos, deben estar estrictamente vinculados y declarados en `build.gradle.kts`, `CMakeLists.txt` y los scripts de compilación.
- **Prohibido el Fallback Arbitrario**: No sustituir las funciones nativas por soluciones fallback en Kotlin si fueron solicitadas o diseñadas para ejecutarse en C++ o Rust.
- **Compatibilidad de Arquitecturas (32 y 64 bits)**:
  - Soporte obligatorio para las 4 ABIs de Android: `arm64-v8a`, `armeabi-v7a`, `x86_64` y `x86`.
  - Asegurar que la lógica de punteros y buffers no colapse en procesadores de 32 bits.
- **Exclusión de Binarios `.so` en Git**:
  - Los archivos `.so` y la carpeta `jniLibs/` están excluidos en `.gitignore`. La compilación de librerías nativas se ejecuta on-the-fly en CI o mediante scripts locales, garantizando que no se suban binarios compilados al repositorio.
- **Limpieza de Residuos Nativos**:
  - Tras cualquier compilación de Rust o CMake que genere archivos intermedios, ejecutar siempre `./limpiar_archivos_nativos.sh` para evitar residuos en el repositorio.

---

## 3. Filosofía de Diseño y UI

- **Rechazo al Minimalismo Extremo**: El usuario no desea interfaces vacías o planos sin textura. Utilizar degradados elegantes, componentes con volumen semi-3D (`Semi3DCard`), contraste nítido y acentos luminosos (verde esmeralda, cian y tonos metálicos oscuros).
- **Diseño Multi-Pantalla Modular**: Prohibido acumular todas las opciones en una única vista revuelta. Separar claramente las pantallas (`NowPlayingScreen`, `LibraryScreen`, `PlaylistsScreen`, `EqualizerScreen`, `SettingsScreen`) con navegación fluida.
- **Desarrollo Modular**: Estructurar los archivos en módulos pequeños y cohesivos (<500 líneas por archivo) para evitar colapsos y facilitar el mantenimiento.

---

## 4. Dependencias y Licencias

- **Funcionalidad sobre Peso del APK**: Al usuario no le preocupa el tamaño en megabytes del APK, siempre que las dependencias funcionen al 100%. Priorizar librerías robustas y estables antes que reimplementaciones manuales propensas a errores.
- **Restricción de Licencias**: No incorporar librerías con licencias copyleft estrictas (como GPLv3, AGPL) que impongan la obligación de liberar código o requerimientos gravosos de atribución que condicionen el proyecto.
- **Protección de Marcas**: No utilizar nombres comerciales protegidos por derechos de autor que puedan comprometer al usuario.

---

## 5. Reglas de Seguridad y Buenas Prácticas

- **Versión Mínima de Android**: La app tiene fijado `minSdk = 26` (Android 8.0 Oreo). Todo componente o API utilizado debe ser compatible con API 26 o superior.
- **Prohibición de `persist.sys.*`**: En cualquier funcionalidad de aceleración, optimización o rendimiento, jamás modificar ni consultar propiedades de sistema que comiencen con `persist.sys.*`, protegiendo la integridad del dispositivo móvil.
- **Explicaciones en Código**: Todos los archivos de código fuente deben incluir comentarios claros en español explicando el propósito de la lógica, los métodos y la arquitectura.
- **Gestión de `commit_message.txt`**: Si existe o se consulta este archivo, su contenido debe mantenerse siempre en **español** y no debe alterarse a menos que el usuario lo solicite explícitamente.
