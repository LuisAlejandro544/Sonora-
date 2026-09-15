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
   - La barra de navegación inferior aloja 4 destinos ergonómicos principales (`Inicio`, `Biblioteca`, `Playlists` y `Ajustes`).
   - **Ajustes Técnicos Desacoplados por Pantallas Independientes**:
     - La pantalla de `SettingsScreen` actúa como menú orquestador que dirige a sub-pantallas autónomas ubicadas en `com.example.ui.screens.settings`:
       - `SettingsAudioScreen`: Gapless, ExoPlayer, Notificación Nativa y formatos.
       - `SettingsNativeScreen`: Diagnóstico de C++17/Rust, ABI (32/64b) y benchmark interactivo.
       - `SettingsUiScreen`: Aislamiento tipográfico (1.0x), diseño semi-3D y touch targets de 48dp.
       - `SettingsStorageScreen`: Métricas de disco, sembrado demo y arquitectura de 4 carpetas.
       - `SettingsAboutScreen`: Privacidad SAF, distribución en Uptodown y versión v1.0.
     - Cada sub-pantalla dispone de barra de navegación superior con botón táctil de regreso de 48dp (`IconButton`), animación horizontal `AnimatedContent` y soporte integrado para el gesto o botón `BackHandler` del teléfono.
   - El *Ecualizador Gráfico DSP de 10 bandas* (`EqualizerScreen`) está integrado directamente dentro del reproductor a pantalla completa (`FullScreenPlayer`) mediante un botón de acceso directo con transición animada y botón de retorno, evitando saturar la barra de navegación del móvil.
   - Componentes de alta densidad informativa (como `MostPlayedSection` con podio de alta rotación y contadores de reproducción) deben modularizarse en submódulos en `com.example.ui.components` manteniendo cada archivo por debajo de 500 líneas.
3. **Gestión de Playlists, Favoritos y Collage Dinámico de Carátulas**:
   - **Lista de Favoritos Unificada**: Al pulsar el botón de corazón, la canción se agrega de inmediato a la lista especial de Canciones Favoritas (`FAVORITES_PLAYLIST_ID = -1L`).
   - **Collage de Carátulas Inteligente**: `PlaylistCoverCollage` compone portadas dinámicas basadas en la cantidad de pistas: 1 foto si hay 1 tema, 2 fotos horizontales si hay 2 temas, y 3 fotos (1 panel grande + 2 cuadrantes) si hay 3 o más temas (no se satura con más imágenes si hay más pistas).
   - **Carátulas Personalizadas en WebP Lossless**: Las carátulas de playlists seleccionadas por el usuario se procesan mediante `WebpLosslessCompressor` a formato WebP Lossless a máxima calidad sin pérdida y se almacenan en `sonora/portadas_webp/`.
   - **Fondo Sólido y Opaco en el Reproductor**: El reproductor a pantalla completa (`FullScreenPlayer`) utiliza un fondo 100% opaco y oscuro (`SonoraBackground`), eliminando cualquier transparencia o solapamiento con vistas anteriores y realzando el volumen semi-3D de la carátula y los controles.
4. **Aislamiento Tipográfico Propio (fontScale = 1.0f)**:
   - Para evitar que los ajustes de accesibilidad o escala de fuente que el usuario tenga configurados globalmente en su teléfono colapsen la diagramación de la app, Sonora tiene su propio tamaño de letra calibrado.
   - Se inyecta un `Density` inmutable con `fontScale = 1.0f` a través de `CompositionLocalProvider(LocalDensity provides customSonoraDensity)` en `SonoraTheme`.
   - Garantiza que los faders del ecualizador de 10 bandas, contadores de tiempo y los objetivos táctiles ergonómicos mínimos de 48dp mantengan siempre proporciones perfectas e inmunes a distorsiones externas.

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
3. **`metadatos/`**: Archivos de texto legible con el nombre de la pista, artista y álbum para consulta directa y respaldo. Se sincronizan atómicamente al editar metadatos.
4. **`registros_json/`**: Archivos `.json` individuales que enlazan las 3 partes anteriores para correlación cruzada, respaldo y reconstrucción de la base de datos local en caso de ser necesario. Se actualizan concurrentemente con cualquier edición.

---

## 🎵 Arquitectura de DSP, ExoPlayer y Estado Reactivo

1. **Cadena de Procesamiento de Audio ExoPlayer**:
   - `Sonora10BandAudioProcessor` implementa la interfaz `AudioProcessor` de Media3 y procesa directamente los buffers PCM de 16 bits en coma flotante mediante el puente JNI nativo en C++ (`libsonora_dsp.so`).
   - El ecualizador cuenta con 10 bandas de frecuencia ISO, preamplificador y filtro no lineal *Soft-Clipping* (`tanh`).
   - `SonoraVocalAudioProcessor` añade el **Laboratorio Vocal C++** (`sonora_vocal.cpp`) a la cadena de audio de ExoPlayer: time-scale modification para acelerar o decelerar la voz del cantante (0.50x a 2.00x) sin alterar la velocidad ni el tempo instrumental de la canción, con filtro Anti-Ardilla de preservación de formantes acústicos y aislamiento estéreo M/S.
   - Los cambios de ganancia, EQ y velocidad vocal se aplican en caliente sin interrupciones ni chasquidos en la reproducción.
2. **Generador de Carátulas Procedurales Matemáticas (Sin IA)**:
   - `ProceduralArtGenerator` crea carátulas bitmap procedurales de altísima calidad visual para canciones que no tienen imagen incrustada.
   - Basado en un hash numérico determinista del título y artista: genera gradientes poligonales envolventes, patrones de ondas rítmicas de audio y micro-surcos concéntricos de disco de vinilo.
   - Es 100% offline, opera en menos de 10 milisegundos, no consume datos móviles, no depende de servicios o modelos de IA pesados y se almacena en WebP Lossless.
3. **Edición de Metadatos Sincronizada**:
   - Al editar el título, artista o álbum de una canción, la actualización se refleja de inmediato en Room, en el fichero de texto en `metadatos/` y en el fichero de metadatos de respaldo en `registros_json/`.
   - Si la pista editada se encuentra en reproducción activa o en la cola, `PlaybackManager` y `MusicViewModel` actualizan el objeto en memoria de forma reactiva sin requerir reinicio del reproductor.
3. **Sistema de Favoritos Instantáneo**:
   - El botón de corazón (favorito) alterna inmediatamente su estado visual en la UI (`SonoraHeartActive` en verde esmeralda vs contorno/gris) antes de que concluya la transacción en disco, proporcionando retroalimentación táctil de latencia cero con animación de escala elástica.
4. **Reproducción Sin Pausas (Gapless Playback)**:
   - Configuración avanzada de `DefaultLoadControl` con precarga anticipada (buffer de hasta 60s y prioridad de tiempo) y `pauseAtEndOfMediaItems = false` en ExoPlayer.
   - Elimina la latencia de re-inicialización del decodificador y los micro-silencios molestos en directos y mezclas contiguas.
   - Permite activar o desactivar la funcionalidad dinámicamente desde la pantalla de Ajustes.
5. **Auto-Reproducción Inmediata al Importar**:
   - Cuando el usuario añade canciones desde el selector de archivos local o inicializa los temas de demostración, Sonora inserta las pistas en Room y activa automáticamente la reproducción de la primera canción importada junto con su cola activa, eliminando fricción y facilitando la escucha directa en el móvil.
6. **Reproducción en Segundo Plano y Notificación Nativa (Media3 MediaSessionService)**:
   - `SonoraMediaService` hereda de `MediaSessionService` de AndroidX Media3 y está declarado como servicio de tipo `mediaPlayback` en `AndroidManifest.xml`.
   - Implementa un canal de notificación con `IMPORTANCE_LOW` y visibilidad pública para pantalla de bloqueo (`VISIBILITY_PUBLIC`).
   - Sincroniza metadatos (título, artista), carátula en alta definición y controles de transporte interactivos (Play/Pause, Anterior, Siguiente y Scrubber temporal).
   - Mantiene la música sonando ininterrumpidamente al salir a la pantalla de inicio o al bloquear el dispositivo móvil, liberando el servicio ordenadamente al pausar y cerrar la app para proteger la batería.
7. **Motor de Limpieza y Sanitización de Metadatos con Rust (`cleaner.rs`)**:
   - Módulo nativo en Rust con bindings JNI directos (`nativeSanitizeTrackMetadata` en `SonoraRustBridge`).
   - Diseñado para que el usuario pueda con un solo toque ("Limpiar con Rust") purgar nombres sucios, prefijos numéricos de pista (`01 - `), marcas de sitios de descarga (`y2mate`, `mp3clan`, etc.) y sufijos molestos (`[320kbps]`, `[Official Video]`, `(Lyrics)`).
   - Coordinado por `MetadataSanitizerManager`, persiste los cambios en Room SQLite y actualiza en tiempo real los registros en `metadatos/` y `registros_json/`.
8. **Generación Automática de Playlists por Artista (Detección de 3+ Canciones)**:
   - Administrado por `ArtistPlaylistManager`: cuando la biblioteca detecta 3 o más canciones de un mismo artista, genera automáticamente una lista de reproducción dedicada (`Colección automática de [Artista]`).
   - Sincronización continua en segundo plano: al añadir o sanitizar nuevas canciones del mismo artista, se asocian automáticamente a la playlist existente sin duplicar pistas. Incluye insignia `Artista` en Compose y botón de sincronización ergonómico.

---

## 📝 Documentación y Legibilidad del Código

- **Comentarios Explicativos en Español**: Todo archivo de código creado o editado debe incluir comentarios y encabezados que expliquen con claridad la lógica, el propósito de las funciones y la arquitectura que implementa.
- **Control de Versiones y Git**: Si existe un archivo `commit_message.txt`, su información debe permanecer siempre en **español** y no debe modificarse a menos que el usuario lo solicite explícitamente.
