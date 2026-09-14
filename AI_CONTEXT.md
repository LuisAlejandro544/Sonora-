# Contexto de Inteligencia Artificial (AI Context) - Sonora

Este documento proporciona contexto técnico, restricciones operativas y lineamientos arquitectónicos para asistentes de Inteligencia Artificial que colaboren en el mantenimiento o expansión del proyecto **Sonora**.

---

## 🎯 Perfil del Proyecto y del Usuario

- **Tipo de Aplicación**: Reproductor de música avanzado para Android con procesamiento de audio nativo.
- **Entorno del Usuario**: El usuario opera y prueba la aplicación principalmente desde un **teléfono móvil** (no desde una computadora de escritorio).
- **Canal de Distribución**: La aplicación se distribuirá de forma independiente (instalación directa vía **APK** o plataformas como **Uptodown**), no a través de Google Play Store.
- **Versión Mínima de Android**: `minSdk = 26` (Android 8.0 Oreo). Permite manejo nativo de canales de notificación y optimización del motor de audio.
- **Políticas de Privacidad y Dependencias**:
  - No se deben introducir dependencias que dependan forzosamente de los Servicios de Google Play (GMS) para funcionar.
  - No se deben integrar librerías con licencias copyleft estrictas (como GPLv3 o AGPL) que impongan la obligación de liberar código o requerimientos gravosos de atribución en la distribución del binario.
  - El peso final del APK no es una restricción limitante: la prioridad absoluta es la **estabilidad, funcionalidad al 100% y confiabilidad de las dependencias**, prefiriendo librerías robustas y probadas antes que soluciones sin dependencias frágiles.
  - **Exclusión de Binarios `.so` en Git**: Los archivos binarios `.so` compilados y `jniLibs/` están estrictamente excluidos en `.gitignore` para evitar filtraciones de código binario, prevenir desfases entre el código fuente y el binario, y evitar el bloat de Git. Las compilaciones se efectúan on-the-fly en GitHub Actions o localmente mediante `cargo-ndk`.

---

## 🏛️ Decisiones de Diseño y UI

1. **Rechazo al Minimalismo Extremo**:
   - Al usuario **no le gusta el minimalismo plano o ultra-simplista**. La interfaz debe contar con riqueza visual, profundidad, efectos semi-3D, degradados elegantes, sombras volumétricas y retroalimentación táctil clara.
2. **Modularidad de Pantallas y Componentes**:
   - Está terminantemente prohibido amontonar todas las funcionalidades en una sola pantalla única atestada.
   - Cada sección principal debe contar con su propia pantalla dedicada (`NowPlaying`, `Library`, `Playlists`, `Equalizer`, `Settings`), conectadas mediante una barra de navegación inferior intuitiva y fluida.
   - Componentes de alta densidad informativa (como `MostPlayedSection` con podio de alta rotación y contadores de reproducción) deben modularizarse en submódulos en `com.example.ui.components` manteniendo cada archivo por debajo de 500 líneas.

---

## ⚙️ Directrices Técnicas de los Motores Nativos (C++ y Rust)

1. **Inclusión Obligatoria en el Flujo de Construcción**:
   - En este proyecto, el código en **C++** y **Rust** es de primera clase y forma parte activa de la aplicación.
   - **Bajo ninguna circunstancia** deben eliminarse, omitirse o comentarse los bloques de CMake o JNI en `build.gradle.kts` o en el flujo de compilación.
   - **No usar funciones fallback de Kotlin** para reemplazar deliberadamente módulos nativos solicitados: si una función está asignada a C++ o Rust, debe ejecutarse en esa biblioteca nativa.
2. **Soporte Multi-Arquitectura (32 y 64 bits)**:
   - Toda implementación nativa debe estar preparada para compilar y operar correctamente en las siguientes 4 arquitecturas:
     - `arm64-v8a` (ARM 64-bit)
     - `armeabi-v7a` (ARM 32-bit)
     - `x86_64` (x86 64-bit)
     - `x86` (x86 32-bit)
   - Prestar atención estricta a tipos numéricos, punteros de memoria y alineación en 32 bits vs 64 bits.
3. **Limpieza de Residuos de Compilación**:
   - Las compilaciones de Rust (`cargo-ndk`) y CMake generan directorios voluminosos (`target/`, `.cxx/`, `.externalNativeBuild/`).
   - Siempre debe utilizarse el script `./limpiar_archivos_nativos.sh` (o `python3 limpiar_archivos_nativos.py`) tras generar los binarios para evitar contaminación en el espacio de trabajo.
4. **Reglas de Seguridad y Restricciones de Sistema**:
   - Si se desarrollan funciones de optimización de rendimiento o Game Booster, **NUNCA** se deben invocar ni configurar propiedades `persist.sys.*`, ya que pueden provocar inestabilidad o bootloops en dispositivos reales.

---

## 🗂️ Arquitectura de Almacenamiento Desacoplado (`android/data/com.nuestraapp/`)

Para garantizar máxima velocidad, organización y preservación de memoria:
1. **`canciones/`**: Archivos de audio (MP3, WAV, FLAC) protegidos en el sandbox de la aplicación.
2. **`webp/`**: Carátulas incrustadas extraídas con el motor Rust y comprimidas a formato **WebP Lossless** con compresión máxima (100% de fidelidad de imagen original, sin artefactos de compresión JPEG y con menor peso en disco).
3. **`metadatos/`**: Archivos de texto legible con el nombre de la pista, artista y álbum para consulta directa y respaldo.
4. **`registros_json/`**: Archivos `.json` individuales que enlazan las 3 partes anteriores para correlación cruzada, respaldo y reconstrucción de la base de datos local en caso de ser necesario.

---

## 📝 Documentación y Legibilidad del Código

- **Comentarios Explicativos en Español**: Todo archivo de código creado o editado debe incluir comentarios y encabezados que expliquen con claridad la lógica, el propósito de las funciones y la arquitectura que implementa.
- **Control de Versiones y Git**: Si existe un archivo `commit_message.txt`, su información debe permanecer siempre en **español** y no debe modificarse a menos que el usuario lo solicite explícitamente.
