# Sonora - Reproductor de Audio Avanzado de Alta Fidelidad

Sonora es un reproductor de audio moderno para Android, construido con **Kotlin** y **Jetpack Compose**, respaldado por un motor híbrido de procesamiento nativo en **C++ (DSP)** y **Rust (Análisis Acústico FFT)**.

El proyecto está diseñado pensando en la libertad del usuario, rendimiento en hardware real de teléfonos móviles y distribución libre e independiente mediante archivos **APK** o tiendas de terceros como **Uptodown**, sin dependencia obligatoria de servicios privativos de Google Play.

---

## 🌟 Características Principales

- **Interfaz Rica y Moderna (Jetpack Compose)**:
  - Diseño con volumen y profundidad visual mediante componentes semi-3D y sombras dinámicas.
  - Navegación modular ergonómica: barra de navegación principal con 4 secciones clave (*Inicio*, *Biblioteca de Canciones*, *Gestión de Playlists* y *Ajustes Técnicos*).
  - **Ajustes y Configuración Técnicos Modulares por Pantallas Independientes**:
    - Para evitar interfaces amontonadas y saturadas en pantallas móviles, la sección de Ajustes se organiza en **sub-pantallas desacopladas e interactivas**:
      1. *Audio y Reproducción*: Configuración de Gapless, decodificación Media3 ExoPlayer, notificación nativa interactiva y códecs admitidos.
      2. *Aceleración Nativa*: Diagnóstico en vivo de C++17 y Rust (O3), arquitectura ABI (32 vs 64 bits) y suite de pruebas de rendimiento DSP interactiva.
      3. *Interfaz y Ergonomía*: Aislamiento tipográfico 1.0x, filosofía Semi-3D y controles ergonómicos de 48dp para uso con una sola mano.
      4. *Almacenamiento Local*: Estadísticas de espacio en disco, temas indexados, sembrado de canciones de demostración y arquitectura de 4 carpetas.
      5. *Privacidad y Acerca de*: Política estricta 100% offline, compatibilidad con tiendas independientes (Uptodown / Sideload) y versión v1.0.
    - Cada sub-pantalla cuenta con su barra superior con botón de retorno ergonómico (touch target de 48dp), animaciones horizontales fluidas y soporte nativo para el gesto o botón `BackHandler` del teléfono.
  - **Ecualizador Integrado Directamente en el Reproductor**: El *Ecualizador Gráfico DSP de 10 Bandas* cuenta con su propia interfaz completa alojada directamente dentro del reproductor a pantalla completa (*Now Playing*), con transición animada fluida y botón de retorno, evitando saturar la barra de navegación inferior en dispositivos móviles.
  - **Aislamiento Tipográfico Propio (`fontScale = 1.0f`)**: La aplicación cuenta con su propia escala tipográfica calibrada e invariable mediante `CompositionLocalProvider`, evitando desbordamientos o choques con el tamaño de letra del sistema que el usuario tenga en su teléfono móvil.
  - Podio dinámico de **"Más escuchadas"** en la pantalla de Inicio: insignia metálica por puesto (#1 oro, #2 plata/cian, #3 bronce), contador de reproducciones en vivo y acceso directo a cola de alta rotación.
- **Gestión Avanzada de Playlists y Favoritos Estilo Spotify**:
  - **Lista Automática de Favoritos**: Al pulsar el botón de corazón en cualquier canción, esta se incorpora de forma inmediata a la lista insignia "Canciones Favoritas", accesible desde la pantalla de listas con un solo toque y reproducción instantánea.
  - **Creación y Personalización de Listas**: Creación de listas personalizadas con nombre, descripción y buscador interactivo modal (`AddSongsToPlaylistDialog`) para incorporar canciones en segundos estilo Spotify.
  - **Collage Dinámico de Portadas (Máximo 3 Fotos)**:
    - Si la lista contiene 1 sola canción, se muestra su carátula ocupando el 100% de la portada.
    - Si contiene 2 canciones, se genera un collage dual horizontal (50% / 50%).
    - Si contiene 3 o más canciones, se genera un collage inteligente de 3 imágenes (1 foto grande en el panel izquierdo y 2 cuadrantes en el derecho). Si hay más canciones, no se satura el collage y se mantiene la composición limpia de 3 fotos.
  - **Carátulas Personalizadas en WebP Lossless**:
    - El usuario puede asignar una imagen personalizada de su galería a cualquier lista.
    - La imagen seleccionada es procesada y comprimida en formato **WebP Lossless** a máxima compresión sin pérdida de fidelidad ni degradación visual, almacenándose de forma autónoma en el almacenamiento local.
  - **Reproductor a Pantalla Completa con Fondo Sólido y Opaco**:
    - Se refinó la interfaz del reproductor para eliminar el fondo translúcido que causaba solapamientos visuales de pantallas previas, utilizando un fondo 100% opaco y oscuro (`SonoraBackground`), resaltando el relieve semi-3D de la carátula, los faders del ecualizador y los controles ergonómicos.
  - **Editor de Metadatos Integrado**: Modificación visual directa del título, artista y álbum mediante diálogo dedicado (`EditMetadataDialog`), accesible desde el menú contextual de cada canción y desde el reproductor a pantalla completa.
  - Visualizador rítmico de audio en tiempo real impulsado por cálculo de frecuencias.

- **Reproducción Sin Pausas (Gapless Playback) & Auto-Play**:
  - **Gapless Nativo**: Precarga y encadenamiento continuo de pistas consecutivas sin micro-silencios ni retardos de decodificación, activable/desactivable en Ajustes. Ideal para grabaciones en vivo, sesiones de mezclas y álbumes conceptuales.
  - **Auto-Reproducción Inmediata al Importar**: Al importar canciones desde el almacenamiento o al añadir los temas de demostración, el reproductor inicia inmediatamente la reproducción de la primera pista importada y actualiza la cola activa sin requerir toques manuales adicionales.

- **Reproducción Continua en Segundo Plano & Notificación Nativa (Media3 & ExoPlayer)**:
  - **Servicio en Primer Plano `SonoraMediaService` (`MediaSessionService`)**: Mantiene la reproducción activa cuando el usuario sale de la aplicación, minimiza la ventana o bloquea la pantalla de su teléfono.
  - **Notificación Nativa del Sistema Android**:
    - Integración profunda mediante `DefaultMediaNotificationProvider` conectado directamente al `MediaSession` de ExoPlayer.
    - Muestra la **carátula del álbum en alta resolución** (generada desde WebP sin pérdida o procedural matemática).
    - Metadatos en tiempo real de la pista (título, artista y duración).
    - **Controles de transporte interactivos**: Botones de *Pista Anterior*, *Reproducir/Pausar* y *Pista Siguiente*.
    - Barra de navegación temporal interactiva (*scrubber*) en la notificación y compatibilidad completa con el reproductor nativo del panel de medios de Android 13+ y pantalla de bloqueo.
    - **Ahorro Inteligente de Batería**: Si la reproducción se detiene y la aplicación es eliminada de la lista de tareas recientes, el servicio se detiene de forma autónoma (`stopSelf()`) para evitar consumo residual de energía.

- **Motor Híbrido Nativo de Alto Rendimiento**:
  - **C++ (`libsonora_dsp.so`) & Conexión Directa a ExoPlayer**:
    - **Ecualizador de 10 Bandas Paramétrico**: Procesamiento en tiempo real con frecuencias ISO (31 Hz, 62 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 16 kHz) con ganancia de -12 dB a +12 dB.
    - **Laboratorio Vocal C++ (Time-Scale Modification & Anti-Ardilla)**: Modificación independiente de la cadencia y velocidad de la voz del cantante (0.50x a 2.00x) en tiempo real sin acelerar la canción instrumental ni alterar el tempo global. Incluye algoritmo de preservación de formantes acústicos ("Anti-Ardilla") para evitar tonos agudos no deseados, aislamiento del canal central (M/S) y presets vocales de alta definición.
    - Integración directa en la tubería de audio de ExoPlayer mediante procesadores `AudioProcessor` personalizados (`Sonora10BandAudioProcessor` y `SonoraVocalAudioProcessor`) acoplados a la fábrica `DefaultRenderersFactory` de Media3.
    - Algoritmo de filtrado digital IIR Biquad paramétrico basado en el estándar de Robert Bristow-Johnson (Cookbook EQ).
    - Limitador y saturador analógico no lineal *Soft-Clipping* (`tanh`) para prevenir distorsión digital al aplicar refuerzo dinámico de graves (*Bass Boost*) y preamplificación.
  - **Rust (`libsonora_rust.so`)**:
    - **Limpieza y Sanitización Nativa de Metadatos (`cleaner.rs`)**: Módulo de alta velocidad que depura en milisegundos títulos, artistas y álbumes corruptos con solo dar un toque ("Limpiar con Rust"). Elimina automáticamente marcas de agua web (`y2mate`, `mp3clan`, `.cc`, `www.`), sufijos de calidad (`[320kbps]`, `[Official Video]`, `(Lyrics)`), índices numéricos de pistas (`01 - `, `track01`) y espacios/guiones basura, formateando los textos limpios en JSON para su persistencia atómica.
    - Extracción nativa ultrarrápida de metadatos (título, artista, álbum) en etiquetas ID3v2 (MP3) y bloques Vorbis (FLAC).
    - Extracción directa de carátulas incrustadas (`APIC` / `PICTURE`) en memoria para su conversión.
    - Análisis espectral de baja latencia con ventana Hamming y banco de filtros logarítmico para el visualizador a 60 FPS.
    - Medidor RMS de energía acústica y cálculo de decibelios en tiempo real (dBFS).
    - Generación de hashes acústicos ultrarrápidos FNV-1a de 64 bits para deduplicación de pistas e indexación en caché.

- **Generador Inteligente de Playlists por Artista (Detección Automática de 3+ Canciones)**:
  - La aplicación monitoriza continuamente la biblioteca de audio: si detecta **3 o más canciones de un mismo artista**, crea automáticamente una lista de reproducción dedicada para ese artista (identificada con una insignia especial `Artista` en la interfaz).
  - **Sincronización Dinámica Continua**: Cada vez que se agregan, importan o sanitizan nuevas canciones pertenecientes a un artista que ya cuenta con su lista automática, el sistema las incorpora instantáneamente sin duplicar pistas.
  - Las listas automáticas cuentan con collage dinámico de carátulas y controles completos de reproducción y gestión.

- **Generador de Carátulas Procedurales de Peso Cero (Cero Dependencia de IA Externa)**:
  - Cuando se importa una pista sin carátula incrustada, Sonora genera automáticamente una carátula vectorial procedural única y determinista basada en el hash del título y artista.
  - Generación instantánea en milisegundos sin consumir conexión a internet ni requerir modelos de IA pesados: patrones geométricos dinámicos, gradientes poligonales de alto contraste y textura concéntrica de microsurcos de disco de vinilo en WebP Lossless.

- **Arquitectura de Almacenamiento Modular Desacoplada (`android/data/com.nuestraapp/`)**:
  - `canciones/`: Almacén aislado de pistas de audio importadas.
  - `webp/`: Carátulas comprimidas en formato **WebP a máxima compresión sin pérdida de calidad (Lossless)**, reduciendo almacenamiento sin degradar la portada.
  - `metadatos/`: Archivos legibles de texto plano con el nombre de la canción, artista y álbum sincronizados automáticamente al editar cualquier campo.
  - `registros_json/`: Archivos `.json` conectores que vinculan cada archivo de audio, su carátula WebP y su archivo de metadatos para trazabilidad e indexación total. Sincronización bidireccional al editar metadatos.

- **Soporte Multi-Arquitectura Completo**:
  - Compilación nativa optimizada para procesadores de **64 bits** (`arm64-v8a`, `x86_64`) y de **32 bits** (`armeabi-v7a`, `x86`).

- **Reproducción Continua en Segundo Plano (Media3 & ExoPlayer)**:
  - Servicio de audio en segundo plano (`MediaSessionService`) con soporte completo de controles en la barra de notificaciones del sistema.
  - Gestión fluida del ciclo de vida de audio, pausa automática al desconectar auriculares y control de volumen por software.

- **Persistencia Local Segura (Room Database)**:
  - Base de datos SQLite local mediante Android Room para almacenamiento offline de pistas, listas de reproducción y configuración del ecualizador.

---

## 🏗️ Arquitectura del Sistema

```
┌────────────────────────────────────────────────────────────┐
│                    Capa de Presentación                    │
│      Jetpack Compose UI (Material 3 + Estilo Semi-3D)       │
├─────────────────────────────┬──────────────────────────────┤
│       Capa de Datos         │    Capa de Reproducción      │
│      Room SQLite DB         │    Media3 ExoPlayer Service  │
├─────────────────────────────┴──────────────────────────────┤
│                   Puentes JNI (Kotlin)                     │
│        SonoraCppBridge        │       SonoraRustBridge     │
├───────────────────────────────┼────────────────────────────┤
│         Motor C++             │         Motor Rust         │
│     libsonora_dsp.so          │     libsonora_rust.so      │
│  • Filtros Biquad IIR         │  • Transformada FFT        │
│  • Limitador Soft-Clip        │  • Medidor dBFS RMS        │
│  • Procesamiento PCM          │  • Hash FNV-1a de 64 bits  │
└───────────────────────────────┴────────────────────────────┘
```

---

## 📱 Requisitos y Compatibilidad

- **Sistema Operativo**: Android 8.0 (API 26) o superior (permite gestión nativa de canales de notificación y soporte de audio moderno).
- **Arquitecturas Soportadas**:
  - `arm64-v8a` (Dispositivos móviles modernos de 64 bits)
  - `armeabi-v7a` (Dispositivos móviles de 32 bits)
  - `x86_64` (Emuladores y tablets de 64 bits)
  - `x86` (Emuladores y dispositivos de 32 bits)
- **NDK Requerido**: Android NDK r27b (`27.2.12479018`).
- **CMake**: 3.22.1+.
- **Rust Toolchain**: 1.85+ con toolchain `cargo-ndk`.

---

## 🔒 Política de Seguridad: Exclusión de Binarios `.so` en Git

Para proteger la integridad del proyecto y la privacidad del usuario:
1. **Sin Binarios Compilados en el Repositorio**: Los archivos `.so` y la carpeta `app/src/main/jniLibs/` están estrictamente excluidos en `.gitignore`. Esto previene la filtración de binarios compilados, evita posibles fugas de información y mantiene el repositorio de Git ligero para conexiones móviles.
2. **Compilación Limpia Bajo Demanda**: Las librerías de Rust (`libsonora_rust.so`) y C++ (`libsonora_dsp.so`) se compilan bajo demanda a partir del código fuente. En el pipeline de **GitHub Actions**, el runner compila los `.so` en tiempo real para las 4 arquitecturas antes de ensamblar el APK final.

---

## 🚀 Compilación y Construcción

### 1. Compilación del Motor Rust
Para generar las librerías compartidas de Rust para todas las ABIs:
```bash
export ANDROID_NDK_HOME="/opt/android/sdk/ndk/27.2.12479018"
cd rust_core
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o ../app/src/main/jniLibs build --release
cd ..
```

### 2. Compilación del APK Completo (Gradle + C++ CMake)
```bash
gradle assembleDebug
```
El APK resultante incluirá automáticamente tanto las librerías compiladas de C++ como las de Rust dentro del paquete final.

### 3. Generación Automática de Firma Debug
Para generar una firma `debug.keystore` fresca y autónoma desde cero sin depender de configuraciones previas:
```bash
chmod +x ./generar_firma_debug.sh
./generar_firma_debug.sh --force
```

### 4. Compilación Automatizada en GitHub Actions (CI/CD sin Caché)
El repositorio incluye el workflow `.github/workflows/compilar_apk_debug.yml`:
- Descarga el código completo del proyecto.
- Instala herramientas nativas de C++ (CMake, Ninja, NDK 27.2) y Rust (targets 32/64 bits, cargo-ndk).
- Compila `libsonora_rust.so` para `arm64-v8a`, `armeabi-v7a`, `x86_64` y `x86`.
- Genera la firma `debug.keystore` desde cero con `./generar_firma_debug.sh --force`.
- Compila el APK Debug limpiamente sin caché (`--no-build-cache --no-daemon`).
- Expone el archivo `Sonora-AudioVibe-Debug-APK` como artefacto descargable directo en GitHub para instalar en el móvil.

### 5. Limpieza Exhaustiva de Archivos Temporales y Cachés Nativas
Para purgar directorios temporales de Rust (`target/`, cachés intermedias) y artefactos de compilación de CMake/Ninja:
```bash
# Mediante script Shell:
./limpiar_archivos_nativos.sh

# O mediante script Python directo:
python3 limpiar_archivos_nativos.py
```

---

## 🛡️ Privacidad y Distribución

- **100% Offline y Privado**: No rastrea datos del usuario, no requiere conexión a internet para reproducir música local y no contiene telemetría externa.
- **Sin Dependencias de Servicios de Google Play**: Puede instalarse y ejecutarse libremente en cualquier dispositivo Android mediante sideloading, tiendas alternativas como Uptodown o gestores de paquetes independientes.
