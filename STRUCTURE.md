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
│   │       ├── cpp/                      # Código nativo en C++ (Motor DSP)
│   │       │   ├── CMakeLists.txt        # Configuración de compilación CMake (Clang -O3)
│   │       │   └── sonora_dsp.cpp        # Filtros Biquad, limitador Soft-Clip y ecualización
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
│   │       │   │   ├── importer/         # Importación manual SAF y extracción nativa
│   │       │   │   │   └── AudioImporter.kt           # Procesa audio, invoca Rust y guarda registros
│   │       │   │   └── repository/       # Patrón Repository para base de datos y archivos
│   │       │   │       └── MusicRepository.kt
│   │       │   ├── player/               # Capa de reproducción de audio y servicio
│   │       │   │   ├── PlaybackManager.kt         # MediaSessionService para segundo plano
│   │       │   │   └── PlaybackState.kt           # Estados reactivos de reproducción
│   │       │   ├── sonora/nativeengine/  # Capa de integración nativa (JNI)
│   │       │   │   ├── SonoraCppBridge.kt        # Puente JNI con libsonora_dsp.so
│   │       │   │   ├── SonoraRustBridge.kt       # Puente JNI con libsonora_rust.so (FFT, dBFS, Metadatos)
│   │       │   │   └── NativeEngineManager.kt    # Detección de ABI (32/64b) y benchmarks
│   │       │   └── ui/                   # Capa de presentación (Jetpack Compose)
│   │       │       ├── SonoraMainApp.kt  # Estructura de navegación principal y barra inferior
│   │       │       ├── components/       # Componentes de interfaz reutilizables
│   │       │       │   ├── Semi3DCard.kt # Tarjetas con relieve visual y sombras multicapa
│   │       │       │   ├── MostPlayedSection.kt # Podio de canciones más reproducidas con insignias semi-3D
│   │       │       │   ├── AudioVisualizer.kt # Visualizador rítmico de ondas
│   │       │       │   └── MiniPlayerBar.kt   # Barra persistente de reproducción
│   │       │       ├── screens/          # Pantallas independientes de la app
│   │       │       │   ├── NowPlayingScreen.kt # Pantalla completa de reproducción actual
│   │       │       │   ├── LibraryScreen.kt    # Explorador y gestor de canciones locales
│   │       │       │   ├── PlaylistsScreen.kt  # Creación y administración de listas
│   │       │       │   ├── EqualizerScreen.kt  # Ecualizador gráfico con presets
│   │       │       │   └── SettingsScreen.kt   # Ajustes, diagnósticos y benchmark nativo
│   │       │       └── theme/            # Sistema de diseño, paleta y tipografías M3
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
- **Pantallas Desacopladas**: Cada pantalla (`NowPlayingScreen`, `LibraryScreen`, `PlaylistsScreen`, `EqualizerScreen`, `SettingsScreen`) es un componente autónomo con su propio flujo de interacción, evitando pantallas únicas saturadas.

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
