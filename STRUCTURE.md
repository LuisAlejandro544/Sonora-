# Estructura del Proyecto Sonora

Este documento detalla la organización del código fuente, los submódulos de compilación y la arquitectura modular de **Sonora**.

---

## 📁 Árbol de Archivos Principal

```
├── app/                                  # Módulo principal de Android
│   ├── build.gradle.kts                  # Configuración Gradle, NDK y dependencias de la app
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml       # Declaración de componentes, servicios y permisos
│   │       ├── cpp/                      # Código nativo en C++ (Motor DSP & Motor Vocal)
│   │       │   ├── CMakeLists.txt        # Configuración de compilación CMake (Clang -O3)
│   │       │   ├── sonora_dsp.cpp        # Filtros Biquad, limitador Soft-Clip y ecualización de 10 bandas
│   │       │   ├── sonora_vocal.h        # Cabecera del motor vocal (Time-Scale Modification & Formantes)
│   │       │   └── sonora_vocal.cpp      # Procesamiento PCM vocal, preservación de formantes y Anti-Ardilla
│   │       ├── jniLibs/                  # Binarios generados (.so) ignorados en Git por .gitignore
│   │       │   ├── arm64-v8a/            # Arquitectura ARM 64 bits (libsonora_rust.so generado)
│   │       │   ├── armeabi-v7a/          # Arquitectura ARM 32 bits (libsonora_rust.so generado)
│   │       │   ├── x86/                  # Arquitectura x86 32 bits (libsonora_rust.so generado)
│   │       │   └── x86_64/               # Arquitectura x86 64 bits (libsonora_rust.so generado)
│   │       ├── java/com/example/         # Código fuente Kotlin
│   │       │   ├── MainActivity.kt       # Actividad principal y punto de entrada Compose
│   │       │   ├── data/                 # Capa de datos y persistencia local
│   │       │   │   ├── local/            # Base de datos Room SQLite (SonoraDatabase, SonoraDao, TrackEntity)
│   │       │   │   ├── storage/          # Almacenamiento desacoplado y compresor WebP
│   │       │   │   │   ├── SonoraStorageManager.kt    # Gestor de 4 carpetas (canciones, webp, metadatos, json)
│   │       │   │   │   └── WebpLosslessCompressor.kt  # Compresión WebP Lossless a máxima calidad
│   │       │   │   ├── importer/         # Importación manual SAF, extracción nativa y arte procedural
│   │       │   │   │   ├── AudioImporter.kt           # Procesa audio, invoca Rust y guarda registros
│   │       │   │   │   └── ProceduralArtGenerator.kt  # Generador procedural matemático de carátulas (sin IA)
│   │       │   │   └── repository/       # Patrón Repository para base de datos y archivos
│   │       │   │       └── MusicRepository.kt
│   │       │   ├── player/               # Capa de reproducción de audio y servicio
│   │       │   │   ├── PlaybackManager.kt         # ExoPlayer + MediaSessionService con Gapless Playback
│   │       │   │   ├── PlaybackState.kt           # Estados reactivos de reproducción (incluye flag isGaplessEnabled)
│   │       │   │   ├── equalizer/                 # Ecualizador de 10 bandas y DSP C++
│   │       │   │   │   ├── EqualizerModel.kt      # Modelos de datos y frecuencias de 10 bandas
│   │       │   │   │   ├── EqualizerManager.kt    # Gestor persistente en Room del ecualizador
│   │       │   │   │   └── Sonora10BandAudioProcessor.kt # AudioProcessor Media3 para ExoPlayer
│   │       │   │   └── vocal/                     # Motor Vocal C++ y corrección de formantes
│   │       │   │       ├── VocalEngineModels.kt   # Estado reactivo, presets y configuraciones vocales
│   │       │   │       ├── VocalEngineManager.kt  # Gestor singleton del motor de voz conectado a JNI
│   │       │   │       └── SonoraVocalAudioProcessor.kt # AudioProcessor Media3 para ExoPlayer
│   │       │   ├── sonora/nativeengine/  # Capa de integración nativa (JNI)
│   │       │   │   ├── SonoraCppBridge.kt        # Puente JNI con libsonora_dsp.so (EQ y Voz C++)
│   │       │   │   ├── SonoraRustBridge.kt       # Puente JNI con libsonora_rust.so (FFT, dBFS, Metadatos)
│   │       │   │   └── NativeEngineManager.kt    # Detección de ABI (32/64b) y benchmarks
│   │       │   └── ui/                   # Capa de presentación (Jetpack Compose)
│   │       │       ├── MainScreen.kt     # Estructura de navegación con barra inferior (Inicio, Biblioteca, Listas, Ajustes)
│   │       │       ├── components/       # Componentes de interfaz reutilizables
│   │       │       │   ├── Semi3DCard.kt # Tarjetas con relieve visual y sombras multicapa
│   │       │       │   ├── MostPlayedSection.kt # Podio de canciones más reproducidas con insignias semi-3D
│   │       │       │   ├── AudioVisualizer.kt # Visualizador rítmico de ondas
│   │       │       │   ├── MiniPlayerBar.kt   # Barra persistente de reproducción con favorito animado
│   │       │       │   ├── CommonComponents.kt # Items de pista con menú contextual y opciones
│   │       │       │   └── EditMetadataDialog.kt # Diálogo flotante para edición de título, artista y álbum
│   │       │       ├── player/           # Vistas de reproducción
│   │       │       │   ├── FullScreenPlayer.kt # Pantalla completa con controles, favorito, Ecualizador y Laboratorio Vocal
│   │       │       │   └── MiniPlayerBar.kt   # Barra persistente inferior
│   │       │       ├── screens/          # Pantallas independientes de la app
│   │       │       │   ├── HomeScreen.kt       # Pantalla de inicio con podio y accesos directos
│   │       │       │   ├── LibraryScreen.kt    # Explorador y gestor de canciones locales con edición
│   │       │       │   ├── PlaylistsScreen.kt  # Creación y administración de listas
│   │       │       │   ├── EqualizerScreen.kt  # Ecualizador gráfico de 10 bandas (integrado en el reproductor)
│   │       │       │   ├── VocalLabScreen.kt   # Laboratorio Vocal C++ (velocidad de voz, Anti-Ardilla y formantes)
│   │       │       │   └── SettingsScreen.kt   # Ajustes (Gapless, tipografía propia), diagnósticos y benchmark nativo
│   │       │       └── theme/            # Sistema de diseño, paleta, tipografías M3 y aislamiento de fontScale (1.0f)
│   │       └── res/                      # Recursos Android (strings, vectores, iconos)
├── rust_core/                            # Subproyecto nativo independiente en Rust
│   ├── Cargo.toml                        # Configuración del crate 'sonora_rust'
│   └── src/
│       └── lib.rs                        # FFT, dBFS, Hash FNV-1a y extractor de metadatos/carátula
├── gradle/                               # Configuración y versión de dependencias
├── limpiar_archivos_nativos.sh           # Script Shell para purgar directorios temporales target/.cxx
├── limpiar_archivos_nativos.py           # Script Python para purga forzada y eliminación de basura
├── .gitignore                            # Exclusiones de Git para binarios y temporales
├── README.md                             # Documentación general y guía de instalación
├── ROADMAP.md                            # Plan de trabajo y futuras versiones
├── STRUCTURE.md                          # Este archivo descriptivo de la estructura
├── AI_CONTEXT.md                         # Contexto arquitectónico para agentes de IA
└── AGENTS.md                             # Instrucciones del sistema para desarrollo asistido
```

---

## 🧩 Descripción de Capas de la Aplicación

### 1. Capa de Presentación (`app/src/main/java/com/example/ui`)
- **Jetpack Compose Puro**: Sin fragmentos XML legacy.
- **Enfoque Semi-3D**: A diferencia de las interfaces planas minimalistas convencionales, Sonora implementa `Semi3DCard` y componentes con degradados dinámicos, sombreados con elevación tangible y realce de bordes luminosos.
- **Pantallas Desacopladas**: Cada pantalla principal (`HomeScreen`, `LibraryScreen`, `PlaylistsScreen`, `SettingsScreen`) y el reproductor con ecualizador integrado (`FullScreenPlayer` + `EqualizerScreen`) son componentes autónomos con su propio flujo de interacción, optimizando la ergonomía en pantallas móviles.

### 2. Capa de Reproducción (`app/src/main/java/com/example/player`)
- **AndroidX Media3**: Utiliza la API moderna de medios para garantizar compatibilidad con Android 8.0 (API 26) hasta Android 16.
- **`SonoraAudioService`**: Servicio `MediaSessionService` que mantiene la reproducción activa en segundo plano con control mediante notificación interactiva (Play, Pause, Skip, barra de progreso).

### 3. Capa de Datos (`app/src/main/java/com/example/data`)
- **Android Room**: Base de datos SQLite reactiva con soporte para Corrutinas y Flow.
- Tablas independientes para pistas locales y listas de reproducción personalizadas.

### 4. Capa de Motores Nativos (`app/src/main/cpp`, `rust_core` y `nativeengine`)
- **C++**: Procesamiento de señales de audio digital en tiempo real con latencia mínima, compilado con CMake.
- **Rust**: Transformada discreta de Fourier y hashing de audio de 64 bits de alta velocidad, compilado con `cargo-ndk`.
- **Puente JNI**: Clases Kotlin `SonoraCppBridge` y `SonoraRustBridge` vinculadas a las librerías dinámicas correspondientes sin capas de simulación.
