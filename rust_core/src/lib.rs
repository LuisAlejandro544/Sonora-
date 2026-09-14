//! ==============================================================================
//! MOTOR NATIVO SONORA EN RUST (libsonora_rust.so)
//! ==============================================================================
//! Este módulo implementa operaciones críticas de audio de alto rendimiento:
//! 1. Análisis espectral FFT / banco de filtros para el visualizador rítmico (60 fps).
//! 2. Cálculo de energía acústica y nivel de decibelios (dBFS) en tiempo real.
//! 3. Generación de huellas acústicas (Audio Hash) para deduplicación y caché.
//! 4. Soporte para arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
//! ==============================================================================

use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JShortArray, JString};
use jni::sys::{jbyteArray, jfloat, jfloatArray, jint, jlong, jstring};
use std::f32::consts::PI;
use std::fs::File;
use std::io::Read;

/// Devuelve la información de versión, arquitectura nativa y estado del motor Rust.
#[no_mangle]
pub extern "system" fn Java_com_example_sonora_nativeengine_SonoraRustBridge_nativeGetRustEngineInfo(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let arch = if cfg!(target_arch = "aarch64") {
        "ARM64 (arm64-v8a - 64 bits)"
    } else if cfg!(target_arch = "arm") {
        "ARM32 (armeabi-v7a - 32 bits)"
    } else if cfg!(target_arch = "x86_64") {
        "x86_64 (64 bits)"
    } else if cfg!(target_arch = "x86") {
        "x86 (32 bits)"
    } else {
        "Genérica"
    };

    let info = format!(
        "Sonora Rust Audio Core v1.0.0 | Arquitectura: {} | Optimización: O3 Release",
        arch
    );

    let output = env.new_string(info).unwrap_or_else(|_| env.new_string("Rust Core").unwrap());
    output.into_raw()
}

/// Calcula el nivel de decibelios (dBFS) a partir de un búfer de muestras PCM de 16 bits.
/// 
/// Algoritmo:
/// - Calcula el valor cuadrático medio (RMS) de las muestras normalizadas [-1.0, 1.0].
/// - Convierte a decibelios mediante la fórmula 20 * log10(RMS).
/// - Si RMS <= 0, devuelve -96.0 dB (piso de ruido de audio de 16 bits).
#[no_mangle]
pub extern "system" fn Java_com_example_sonora_nativeengine_SonoraRustBridge_nativeCalculateDecibels(
    mut env: JNIEnv,
    _class: JClass,
    samples: JShortArray,
) -> jfloat {
    let len = match env.get_array_length(&samples) {
        Ok(l) => l as usize,
        Err(_) => return -96.0,
    };

    if len == 0 {
        return -96.0;
    }

    let mut buffer = vec![0i16; len];
    if env.get_short_array_region(&samples, 0, &mut buffer).is_err() {
        return -96.0;
    }

    let mut sum_squares: f64 = 0.0;
    for &sample in &buffer {
        let norm = sample as f64 / 32768.0;
        sum_squares += norm * norm;
    }

    let rms = (sum_squares / (len as f64)).sqrt();
    if rms < 1e-5 {
        -96.0
    } else {
        (20.0 * rms.log10()) as f32
    }
}

/// Genera un espectrograma de magnitudes dividido en `num_bands` (por ejemplo 16 o 32 barras)
/// utilizando un banco de filtros DFT optimizado con ventana Hamming.
/// 
/// Este cálculo nativo en Rust garantiza que las animaciones de ondas de la interfaz Compose
/// corran fluidas sin presionar el recolector de basura de la JVM.
#[no_mangle]
pub extern "system" fn Java_com_example_sonora_nativeengine_SonoraRustBridge_nativeComputeSpectrum(
    mut env: JNIEnv,
    _class: JClass,
    samples: JShortArray,
    num_bands: jint,
) -> jfloatArray {
    let bands_count = if num_bands <= 0 { 16 } else { num_bands as usize };
    let sample_len = match env.get_array_length(&samples) {
        Ok(l) => l as usize,
        Err(_) => 0,
    };

    let result_array = match env.new_float_array(bands_count as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    if sample_len == 0 {
        return result_array.into_raw();
    }

    // Tomar hasta 512 muestras para mantener latencia instantánea (<1ms)
    let n = sample_len.min(512);
    let mut buffer = vec![0i16; n];
    if env.get_short_array_region(&samples, 0, &mut buffer).is_err() {
        return result_array.into_raw();
    }

    // Aplicar ventana Hamming y normalizar
    let mut windowed = vec![0.0f32; n];
    for (i, &s) in buffer.iter().enumerate() {
        let multiplier = 0.54 - 0.46 * ((2.0 * PI * i as f32) / ((n - 1) as f32)).cos();
        windowed[i] = (s as f32 / 32768.0) * multiplier;
    }

    // Distribución logarítmica de frecuencias para las bandas visuales
    let mut result_bands = vec![0.0f32; bands_count];
    let step = (n / 2) as f32 / (bands_count as f32);

    for band in 0..bands_count {
        let start_bin = (band as f32 * step) as usize;
        let end_bin = (((band + 1) as f32 * step) as usize).min(n / 2).max(start_bin + 1);

        let mut band_energy = 0.0f32;
        for k in start_bin..end_bin {
            let mut real = 0.0f32;
            let mut imag = 0.0f32;
            let freq_step = 2.0 * PI * (k as f32) / (n as f32);

            for (idx, &val) in windowed.iter().enumerate() {
                let angle = freq_step * (idx as f32);
                real += val * angle.cos();
                imag -= val * angle.sin();
            }
            let magnitude = (real * real + imag * imag).sqrt();
            band_energy += magnitude;
        }

        let avg = band_energy / ((end_bin - start_bin) as f32);
        // Escalar a rango [0.0, 1.0] con compresión logarítmica suave
        let normalized = (avg * 4.0).tanh();
        result_bands[band] = normalized.clamp(0.05, 1.0);
    }

    if env.set_float_array_region(&result_array, 0, &result_bands).is_ok() {
        result_array.into_raw()
    } else {
        std::ptr::null_mut()
    }
}

/// Calcula un hash rápido FNV-1a de 64 bits sobre el búfer de audio.
/// 
/// Útil para detectar pistas duplicadas y comprobar la integridad del caché sin decodificar
/// todo el archivo en la capa de Kotlin.
#[no_mangle]
pub extern "system" fn Java_com_example_sonora_nativeengine_SonoraRustBridge_nativeComputeAudioHash(
    mut env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jlong {
    let len = match env.get_array_length(&data) {
        Ok(l) => l as usize,
        Err(_) => return 0,
    };

    if len == 0 {
        return 0;
    }

    let mut buffer = vec![0i8; len];
    if env.get_byte_array_region(&data, 0, &mut buffer).is_err() {
        return 0;
    }

    // Algoritmo FNV-1a de 64 bits
    let mut hash: u64 = 0xcbf29ce484222325;
    const FNV_PRIME: u64 = 0x100000001b3;

    for &byte in &buffer {
        hash ^= (byte as u8) as u64;
        hash = hash.wrapping_mul(FNV_PRIME);
    }

    hash as jlong
}

// ==============================================================================
// EXTRACTOR NATIVO DE METADATOS Y CARÁTULA EN RUST
// ==============================================================================

/// Estructura de metadatos extraídos de las etiquetas del archivo de audio.
struct AudioTags {
    title: String,
    artist: String,
    album: String,
    has_cover: bool,
}

/// Parsea etiquetas ID3v2 (MP3) y bloques Vorbis/Picture (FLAC) en memoria.
fn parse_audio_tags(buffer: &[u8]) -> AudioTags {
    let mut title = String::new();
    let mut artist = String::new();
    let mut album = String::new();
    let mut has_cover = false;

    // 1. Detección de cabecera ID3v2 (MP3)
    if buffer.len() >= 10 && &buffer[0..3] == b"ID3" {
        let tag_size = ((buffer[6] as usize & 0x7F) << 21)
            | ((buffer[7] as usize & 0x7F) << 14)
            | ((buffer[8] as usize & 0x7F) << 7)
            | (buffer[9] as usize & 0x7F);

        let max_len = (10 + tag_size).min(buffer.len());
        let mut pos = 10;

        while pos + 10 <= max_len {
            let frame_id = &buffer[pos..pos + 4];
            let frame_size = ((buffer[pos + 4] as usize) << 24)
                | ((buffer[pos + 5] as usize) << 16)
                | ((buffer[pos + 6] as usize) << 8)
                | (buffer[pos + 7] as usize);

            if frame_size == 0 || pos + 10 + frame_size > max_len {
                break;
            }

            let frame_data = &buffer[pos + 10..pos + 10 + frame_size];

            match frame_id {
                b"TIT2" => {
                    title = decode_text_frame(frame_data);
                }
                b"TPE1" => {
                    artist = decode_text_frame(frame_data);
                }
                b"TALB" => {
                    album = decode_text_frame(frame_data);
                }
                b"APIC" => {
                    has_cover = true;
                }
                _ => {}
            }

            pos += 10 + frame_size;
        }
    } else if buffer.len() >= 4 && &buffer[0..4] == b"fLaC" {
        // 2. Detección de cabecera nativa FLAC
        let mut pos = 4;
        let mut is_last = false;

        while !is_last && pos + 4 <= buffer.len() {
            let header = buffer[pos];
            is_last = (header & 0x80) != 0;
            let block_type = header & 0x7F;
            let length = ((buffer[pos + 1] as usize) << 16)
                | ((buffer[pos + 2] as usize) << 8)
                | (buffer[pos + 3] as usize);

            pos += 4;
            if pos + length > buffer.len() {
                break;
            }

            let block_data = &buffer[pos..pos + length];

            if block_type == 4 {
                // VORBIS_COMMENT
                parse_vorbis_comment(block_data, &mut title, &mut artist, &mut album);
            } else if block_type == 6 {
                // PICTURE
                has_cover = true;
            }

            pos += length;
        }
    }

    AudioTags {
        title,
        artist,
        album,
        has_cover,
    }
}

/// Decodifica el contenido textual de un frame ID3v2 según su byte de encoding inicial.
fn decode_text_frame(data: &[u8]) -> String {
    if data.is_empty() {
        return String::new();
    }
    let encoding = data[0];
    let payload = &data[1..];

    match encoding {
        0 => {
            // ISO-8859-1 (Latin-1)
            payload
                .iter()
                .take_while(|&&b| b != 0)
                .map(|&b| b as char)
                .collect()
        }
        1 | 2 => {
            // UTF-16 con BOM
            let u16_chars: Vec<u16> = payload
                .chunks_exact(2)
                .map(|c| u16::from_le_bytes([c[0], c[1]]))
                .take_while(|&u| u != 0 && u != 0xFEFF && u != 0xFFFE)
                .collect();
            String::from_utf16_lossy(&u16_chars)
        }
        3 => {
            // UTF-8
            String::from_utf8_lossy(payload)
                .trim_end_matches('\0')
                .to_string()
        }
        _ => String::from_utf8_lossy(payload).to_string(),
    }
}

/// Extrae campos clave de comentarios Vorbis (TITLE, ARTIST, ALBUM) en archivos FLAC.
fn parse_vorbis_comment(data: &[u8], title: &mut String, artist: &mut String, album: &mut String) {
    if data.len() < 8 {
        return;
    }
    let vendor_len = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;
    let mut pos = 4 + vendor_len;

    if pos + 4 > data.len() {
        return;
    }

    let comment_count = u32::from_le_bytes([data[pos], data[pos + 1], data[pos + 2], data[pos + 3]]) as usize;
    pos += 4;

    for _ in 0..comment_count {
        if pos + 4 > data.len() {
            break;
        }
        let len = u32::from_le_bytes([data[pos], data[pos + 1], data[pos + 2], data[pos + 3]]) as usize;
        pos += 4;

        if pos + len > data.len() {
            break;
        }

        let entry = String::from_utf8_lossy(&data[pos..pos + len]).to_string();
        pos += len;

        let upper = entry.to_uppercase();
        if upper.starts_with("TITLE=") {
            *title = entry[6..].trim().to_string();
        } else if upper.starts_with("ARTIST=") {
            *artist = entry[7..].trim().to_string();
        } else if upper.starts_with("ALBUM=") {
            *album = entry[6..].trim().to_string();
        }
    }
}

/// Extrae los bytes crudos de la imagen de carátula (JPEG / PNG / WebP) incrustada en audio.
fn extract_cover_bytes(buffer: &[u8]) -> Option<Vec<u8>> {
    // 1. Búsqueda en ID3v2 (Frame APIC)
    if buffer.len() >= 10 && &buffer[0..3] == b"ID3" {
        let tag_size = ((buffer[6] as usize & 0x7F) << 21)
            | ((buffer[7] as usize & 0x7F) << 14)
            | ((buffer[8] as usize & 0x7F) << 7)
            | (buffer[9] as usize & 0x7F);

        let max_len = (10 + tag_size).min(buffer.len());
        let mut pos = 10;

        while pos + 10 <= max_len {
            let frame_id = &buffer[pos..pos + 4];
            let frame_size = ((buffer[pos + 4] as usize) << 24)
                | ((buffer[pos + 5] as usize) << 16)
                | ((buffer[pos + 6] as usize) << 8)
                | (buffer[pos + 7] as usize);

            if frame_size == 0 || pos + 10 + frame_size > max_len {
                break;
            }

            if frame_id == b"APIC" {
                let frame_data = &buffer[pos + 10..pos + 10 + frame_size];
                // Localizar firma de imagen: JPEG (0xFF 0xD8) o PNG (0x89 0x50 0x4E 0x47)
                for i in 0..frame_data.len().saturating_sub(4) {
                    if (frame_data[i] == 0xFF && frame_data[i + 1] == 0xD8)
                        || (&frame_data[i..i + 4] == &[0x89, 0x50, 0x4E, 0x47])
                    {
                        return Some(frame_data[i..].to_vec());
                    }
                }
            }

            pos += 10 + frame_size;
        }
    } else if buffer.len() >= 4 && &buffer[0..4] == b"fLaC" {
        // 2. Búsqueda en bloque PICTURE de FLAC
        let mut pos = 4;
        let mut is_last = false;

        while !is_last && pos + 4 <= buffer.len() {
            let header = buffer[pos];
            is_last = (header & 0x80) != 0;
            let block_type = header & 0x7F;
            let length = ((buffer[pos + 1] as usize) << 16)
                | ((buffer[pos + 2] as usize) << 8)
                | (buffer[pos + 3] as usize);

            pos += 4;
            if pos + length > buffer.len() {
                break;
            }

            if block_type == 6 {
                // PICTURE block
                let block_data = &buffer[pos..pos + length];
                // Localizar JPEG o PNG
                for i in 0..block_data.len().saturating_sub(4) {
                    if (block_data[i] == 0xFF && block_data[i + 1] == 0xD8)
                        || (&block_data[i..i + 4] == &[0x89, 0x50, 0x4E, 0x47])
                    {
                        return Some(block_data[i..].to_vec());
                    }
                }
            }

            pos += length;
        }
    }

    None
}

/// Extrae metadatos (título, artista, álbum y flag de carátula) desde la ruta de un archivo de audio.
/// Devuelve un JSON estructurado con la información parseada directamente en Rust.
#[no_mangle]
pub extern "system" fn Java_com_example_sonora_nativeengine_SonoraRustBridge_nativeExtractAudioMetadata(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jstring {
    let path_str: String = match env.get_string(&file_path) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    if path_str.is_empty() {
        let err_json = "{\"error\":\"Ruta de archivo vacía\"}";
        return env.new_string(err_json).unwrap().into_raw();
    }

    // Leemos los primeros 2 MB del archivo para escanear cabeceras de metadatos
    let mut file = match File::open(&path_str) {
        Ok(f) => f,
        Err(_) => {
            let err_json = "{\"error\":\"No se pudo abrir el archivo\"}";
            return env.new_string(err_json).unwrap().into_raw();
        }
    };

    let mut buffer = vec![0u8; 2 * 1024 * 1024];
    let bytes_read = file.read(&mut buffer).unwrap_or(0);
    buffer.truncate(bytes_read);

    let tags = parse_audio_tags(&buffer);

    // Escape de comillas para JSON seguro
    let safe_title = tags.title.replace('"', "\\\"");
    let safe_artist = tags.artist.replace('"', "\\\"");
    let safe_album = tags.album.replace('"', "\\\"");

    let json_response = format!(
        "{{\"title\":\"{}\",\"artist\":\"{}\",\"album\":\"{}\",\"hasCover\":{}}}",
        safe_title, safe_artist, safe_album, tags.has_cover
    );

    match env.new_string(json_response) {
        Ok(js) => js.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Extrae los bytes binarios de la carátula incrustada (JPEG o PNG) desde el archivo de audio.
#[no_mangle]
pub extern "system" fn Java_com_example_sonora_nativeengine_SonoraRustBridge_nativeExtractCoverArt(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jbyteArray {
    let path_str: String = match env.get_string(&file_path) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    let mut file = match File::open(&path_str) {
        Ok(f) => f,
        Err(_) => return std::ptr::null_mut(),
    };

    // Leemos hasta 4 MB para asegurar la captura de carátulas en alta resolución
    let mut buffer = vec![0u8; 4 * 1024 * 1024];
    let bytes_read = file.read(&mut buffer).unwrap_or(0);
    buffer.truncate(bytes_read);

    let cover_bytes = match extract_cover_bytes(&buffer) {
        Some(bytes) => bytes,
        None => return std::ptr::null_mut(),
    };

    let byte_array = match env.new_byte_array(cover_bytes.len() as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    let signed_bytes: Vec<i8> = cover_bytes.into_iter().map(|b| b as i8).collect();
    if env.set_byte_array_region(&byte_array, 0, &signed_bytes).is_ok() {
        byte_array.into_raw()
    } else {
        std::ptr::null_mut()
    }
}

