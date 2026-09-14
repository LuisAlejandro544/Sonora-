# Mapa de Ruta (Roadmap) de Sonora

Este documento describe la visión de desarrollo y la evolución técnica de **Sonora**, organizando las metas por fases de implementación modular.

---

## 📍 Estado Actual: Fase 1 (Completada)

- [x] **Arquitectura Base y Modularidad**:
  - Implementación con Jetpack Compose y diseño semi-3D de alta densidad visual.
  - Separación estricta de responsabilidades: navegación desacoplada, capa de datos Room y servicio de reproducción Media3.
  - Soporte de ciclo de vida completo y servicio de reproducción persistente en segundo plano (`MediaSessionService`).
- [x] **Integración de Motores Nativos (C++ y Rust)**:
  - Compilación cruzada para arquitecturas de 32 bits (`armeabi-v7a`, `x86`) y 64 bits (`arm64-v8a`, `x86_64`).
  - Motor C++ con filtros IIR Biquad y limitador Soft-Clipping (`tanh`).
  - Motor Rust con transformada FFT para espectrograma, cálculo de decibelios RMS y hash acústico de 64 bits.
  - Panel de pruebas de rendimiento nativo en la pantalla de Ajustes.
- [x] **Gestión de Entorno de Compilación y Seguridad**:
  - Requisito mínimo Android 8.0 (API 26) para soporte moderno de MediaSession y canales nativos.
  - Script automatizado de limpieza de residuos nativos (`limpiar_archivos_nativos.sh`).
  - Reglas exhaustivas de exclusión en `.gitignore` para artefactos de compilación Cargo, CMake, NDK y binarios compilados `.so` (evita filtraciones y mantiene el repositorio limpio).

---

## 🚀 Fase 2: Formatos de Alta Fidelidad y Decodificación Nativa

- [ ] **Soporte Extendido de Códecs de Audio**:
  - Soporte completo para reproducción de formatos sin pérdida: **FLAC (hasta 24 bits / 192 kHz)**, **ALAC**, **WAV (PCM 32-bit float)**.
  - Soporte optimizado de formatos de alta compresión: **Opus**, **OGG Vorbis** y **AAC**.
- [ ] **Decodificador Nativo en Rust para Formatos Específicos**:
  - Módulo nativo en Rust para decodificación y parseo de cabeceras de audio sin sobrecarga en la JVM.
- [x] **Reproducción Sin Pausas (Gapless Playback)**:
  - Eliminación de micro-silencios y retardos entre pistas consecutivas para álbumes en vivo, pistas continuas y sinfonías clásicas mediante configuración de búfer anticipado (`DefaultLoadControl`) y `pauseAtEndOfMediaItems = false` en ExoPlayer, con conmutador en la pantalla de Ajustes.
- [ ] **Alineación de Ganancia (ReplayGain v2 / EBU R128)**:
  - Normalización automática del volumen entre diferentes canciones mediante cálculo RMS nativo en Rust.

---

## 🎛️ Fase 3: Procesamiento DSP Avanzado y Ecualizador de 10 Bandas

- [x] **Expansión del Ecualizador Gráfico a 10 Bandas en C++**:
  - Transición a ecualizador de **10 bandas paramétricas** (31 Hz a 16 kHz) procesado íntegramente en C++ nativo.
  - Conexión directa a la tubería de audio de ExoPlayer con `Sonora10BandAudioProcessor` implementando `AudioProcessor` de Media3.
  - Perfiles predefinidos (Plano, Refuerzo de Graves, Refuerzo de Agudos, Rock, Pop, Jazz, Clásica, Vocal, Electrónica, Acústico).
  - Preamplificador independiente (-12 dB a +12 dB), refuerzo de graves (*Bass Boost*) y limitador no lineal analógico *Soft-Clipping* (`tanh`).
- [ ] **Virtualizador Espacial 3D y Expansión Estéreo**:
  - Algoritmo de procesamiento binaural para auriculares en el motor C++ para emular acústica de sala de conciertos.
- [ ] **Reverberación Convolutiva (Convolution Reverb)**:
  - Carga de respuestas a impulsos (IR) para simular espacios físicos reales.
- [ ] **Control de Pitch y Tempo Independiente**:
  - Modificación de velocidad de reproducción sin alterar el tono musical (Time-stretching nativo).

---

## 📚 Fase 4: Gestión Inteligente de Biblioteca y Metadatos
 
- [x] **Extracción Nativa de Metadatos y Carátulas en Rust**:
  - Extractor nativo en Rust para parseo de cabeceras ID3v2 (MP3) y bloques Vorbis/Picture (FLAC), extrayendo título, artista y álbum.
  - Extracción de bytes crudos de carátula incrustada y compresión a **WebP sin pérdida de calidad (Lossless)** a máxima eficiencia.
- [x] **Arquitectura de Almacenamiento Modular Desacoplada**:
  - Separación física en 4 directorios independientes (`canciones/`, `webp/`, `metadatos/`, `registros_json/`).
  - Generación de archivos conectores `.json` para trazabilidad y registro íntegro de la biblioteca.
- [ ] **Clasificación por Detección Acústica**:
  - Análisis automático de pistas para clasificar por ritmo y nivel de energía acústica usando el puente Rust.
- [x] **Sección Dinámica de Alta Rotación ("Más escuchadas")**:
  - Seguimiento y persistencia reactiva del contador de reproducciones (`playCount`) en Room SQLite.
  - Podio semi-3D visual con insignias de posición metálicas (#1 oro, #2 cian/plata, #3 bronce), contador de reproducciones e inicio directo de cola en alta rotación.
- [ ] **Listas de Reproducción Inteligentes Adicionales**:
  - Generación de listas dinámicas avanzadas ("Favoritas del mes", "Historial semanal", "Baja rotación / Redescubrir").
- [x] **Editor de Metadatos Integrado y Favoritos Reactivos**:
  - Capacidad para editar título, artista y álbum directamente desde la pantalla del teléfono con `EditMetadataDialog`.
  - Sincronización atómica de cambios en base de datos Room, archivos de texto plano y registros `.json`.
  - Sistema de favoritos con botón de corazón reactivo instantáneo (verde esmeralda `#00E676` vs gris/contorno) con animación elástica y latencia cero.
- [x] **Reproducción Automática al Importar (Auto-Play)**:
  - Al importar archivos de audio desde el almacenamiento local del teléfono o al sembrar pistas de demostración, la aplicación inicia inmediatamente la reproducción de la primera pista importada y actualiza la cola activa sin requerir pulsaciones manuales adicionales.

---

## 🛠️ Fase 5: Experiencia Móvil, Conducción y Utilidades

- [x] **Aislamiento Tipográfico Propio (Escala Fija 1.0f)**:
  - Envoltura global de `CompositionLocalProvider(LocalDensity provides customSonoraDensity)` con escala de fuente `fontScale = 1.0f` fija.
  - Previene que las configuraciones de accesibilidad o tamaño de fuente del sistema operativo del teléfono desborden los botones ergonómicos de 48dp, controles del ecualizador de 10 bandas o frentes de onda.
- [ ] **Temporizador de Apagado Inteligente (Sleep Timer)**:
  - Apagado suave con desvanecimiento gradual (*fade out*) al finalizar un tiempo establecido o al terminar la pista actual.
- [ ] **Modo Conducción / Interfaz Simplificada de Seguridad**:
  - Pantalla alternativa de gran contraste y botones de toque amplio para su uso seguro en soportes de vehículos.
- [ ] **Control por Gestos en Pantalla de Reproducción**:
  - Deslizar horizontalmente para cambiar de pista, vertical para ajuste fino de volumen y doble toque para pausar/reanudar.
- [ ] **Exportación y Respaldo Local de Biblioteca**:
  - Respaldo en archivo JSON/SQLite de listas de reproducción y estadísticas locales para transferir a otros dispositivos sin nube.
