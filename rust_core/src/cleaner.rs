//! ==============================================================================
//! MÓDULO DE SANEAMIENTO Y LIMPIEZA DE METADATOS NATIVO EN RUST (cleaner.rs)
//! ==============================================================================
//! Este módulo implementa algoritmos de alto rendimiento para:
//! 1. Corregir caracteres corruptos de codificación (Mojibake UTF-8 / Latin-1 / Windows-1252).
//! 2. Eliminar etiquetas de ripeo web, sitios de descarga, calidades y sufijos basura
//!    (ej: [www.y2mate.com], (Official Audio), (320kbps), [FLAC], snaptube, etc.).
//! 3. Descomponer inteligentemente cadenas del tipo "Artista - Título" cuando el archivo
//!    contiene la información fusionada en el título o en el nombre de archivo.
//! 4. Eliminar números de pista en el prefijo (ej: "01. ", "02 - ").
//! 5. Normalizar espacios, guiones bajos ("_") y aplicar Title Case inteligente a textos
//!    completamente en mayúsculas o minúsculas.
//! ==============================================================================

/// Lista de sufijos y etiquetas basura comunes agregadas por herramientas de ripeo web y convertidores.
const JUNK_TAGS: &[&str] = &[
    "[www.y2mate.com]",
    "(www.y2mate.com)",
    "www.y2mate.com",
    "y2mate.com",
    "[yt1s.com]",
    "(yt1s.com)",
    "yt1s.com - ",
    "yt1s.com",
    "[snaptube]",
    "(snaptube)",
    "snaptube",
    "[descargarmusica]",
    "(descargarmusica)",
    "[official audio]",
    "(official audio)",
    "[official video]",
    "(official video)",
    "[official music video]",
    "(official music video)",
    "(video oficial)",
    "[video oficial]",
    "(audio oficial)",
    "[audio oficial]",
    "(audio)",
    "[audio]",
    "(visualizer)",
    "[visualizer]",
    "(lyric video)",
    "[lyric video]",
    "(lyrics)",
    "[lyrics]",
    "(letra)",
    "[letra]",
    "(letra oficial)",
    "[letra oficial]",
    "(remastered)",
    "[remastered]",
    "(remaster)",
    "[remaster]",
    "(320kbps)",
    "[320kbps]",
    "(320 kbps)",
    "[320 kbps]",
    "(128kbps)",
    "[128kbps]",
    "(hq)",
    "[hq]",
    "(hd)",
    "[hd]",
    "(4k)",
    "[4k]",
    "(flac)",
    "[flac]",
    "(mp3)",
    "[mp3]",
    "(explicit)",
    "[explicit]",
    "(clean)",
    "[clean]",
    "(original mix)",
    "[original mix]",
    "free download",
    "descargar",
];

/// Extensiones de archivo comunes a limpiar si vienen incrustadas en el título.
const AUDIO_EXTENSIONS: &[&str] = &[
    ".mp3", ".flac", ".m4a", ".wav", ".aac", ".ogg", ".opus", ".wma",
];

/// Corrige Mojibake típico donde secuencias UTF-8 fueron mal interpretadas como ISO-8859-1 o Windows-1252.
pub fn fix_mojibake(input: &str) -> String {
    let mut result = input.to_string();

    let replacements = [
        ("Ã¡", "á"),
        ("Ã©", "é"),
        ("Ã­", "í"),
        ("Ã³", "ó"),
        ("Ãº", "ú"),
        ("Ã", "Á"),
        ("Ã‰", "É"),
        ("Ã", "Í"),
        ("Ã“", "Ó"),
        ("Ãš", "Ú"),
        ("Ã±", "ñ"),
        ("Ã‘", "Ñ"),
        ("Ã¼", "ü"),
        ("Ãœ", "Ü"),
        ("â€™", "'"),
        ("â€˜", "'"),
        ("â€œ", "\""),
        ("â€", "\""),
        ("â€”", " - "),
        ("â€“", " - "),
        ("Â«", "«"),
        ("Â»", "»"),
        ("Â¿", "¿"),
        ("Â¡", "¡"),
        ("&amp;", "&"),
        ("&quot;", "\""),
        ("&apos;", "'"),
        ("&lt;", "<"),
        ("&gt;", ">"),
    ];

    for &(bad, good) in &replacements {
        if result.contains(bad) {
            result = result.replace(bad, good);
        }
    }

    result
}

/// Elimina caracteres de control ASCII (0-31 y 127) salvo retornos normales.
fn remove_control_chars(input: &str) -> String {
    input
        .chars()
        .filter(|&c| !c.is_control() || c == ' ')
        .collect()
}

/// Elimina etiquetas basura y extensiones de audio sin importar mayúsculas/minúsculas.
pub fn strip_junk_tags(input: &str) -> String {
    let mut cleaned = input.to_string();

    // 1. Quitar extensiones de archivo
    for &ext in AUDIO_EXTENSIONS {
        if cleaned.to_lowercase().ends_with(ext) {
            let new_len = cleaned.len() - ext.len();
            cleaned.truncate(new_len);
        }
    }

    // 2. Quitar etiquetas basura
    for &tag in JUNK_TAGS {
        let lower = cleaned.to_lowercase();
        if let Some(idx) = lower.find(tag) {
            let before = &cleaned[..idx];
            let after = &cleaned[idx + tag.len()..];
            cleaned = format!("{} {}", before, after);
        }
    }

    // 3. Quitar prefijos numéricos de pista como "01. ", "01 - ", "01_ ", "1 - "
    let bytes = cleaned.as_bytes();
    let mut num_end = 0;
    while num_end < bytes.len() && bytes[num_end].is_ascii_digit() {
        num_end += 1;
    }
    if num_end > 0 && num_end <= 3 && num_end < bytes.len() {
        let rest = &cleaned[num_end..];
        let trimmed_prefix = rest.trim_start();
        if trimmed_prefix.starts_with('.')
            || trimmed_prefix.starts_with('-')
            || trimmed_prefix.starts_with('_')
        {
            let after_sep = &trimmed_prefix[1..];
            cleaned = after_sep.trim_start().to_string();
        }
    }

    cleaned
}

/// Reemplaza guiones bajos excesivos por espacios y consolida espacios múltiples.
pub fn normalize_whitespace(input: &str) -> String {
    // Si contiene guiones bajos y no parece un identificador estricto, convertirlos a espacios
    let with_spaces = if input.contains('_') && !input.contains(" ") {
        input.replace('_', " ")
    } else {
        input.to_string()
    };

    let words: Vec<&str> = with_spaces.split_whitespace().collect();
    let mut consolidated = words.join(" ");

    // Quitar guiones o puntos huérfanos en bordes
    consolidated = consolidated.trim_matches(|c: char| c == '-' || c == '.' || c == '_' || c.is_whitespace()).to_string();
    consolidated
}

/// Convierte una cadena a Title Case si está completamente en mayúsculas o completamente en minúsculas.
pub fn smart_title_case(input: &str) -> String {
    if input.is_empty() {
        return String::new();
    }

    let is_all_upper = input.chars().any(|c| c.is_alphabetic())
        && input.chars().all(|c| !c.is_alphabetic() || c.is_uppercase());
    let is_all_lower = input.chars().any(|c| c.is_alphabetic())
        && input.chars().all(|c| !c.is_alphabetic() || c.is_lowercase());

    // Solo transformar si está TODO en mayúsculas o TODO en minúsculas
    if !is_all_upper && !is_all_lower {
        return input.to_string();
    }

    let minor_words = ["de", "del", "la", "el", "los", "las", "y", "en", "con", "a", "of", "the", "and", "in", "to", "feat.", "ft."];
    let words: Vec<&str> = input.split_whitespace().collect();
    let mut result_words = Vec::new();

    for (i, &word) in words.iter().enumerate() {
        let lower_word = word.to_lowercase();
        if i > 0 && minor_words.contains(&lower_word.as_str()) {
            result_words.push(lower_word);
        } else {
            let mut chars = word.chars();
            if let Some(first) = chars.next() {
                let rest: String = chars.as_str().to_lowercase();
                result_words.push(format!("{}{}", first.to_uppercase(), rest));
            }
        }
    }

    result_words.join(" ")
}

/// Estructura con el resultado del saneamiento de metadatos.
#[derive(Debug, Clone)]
pub struct CleanedMetadata {
    pub title: String,
    pub artist: String,
    pub album: String,
    pub was_modified: bool,
}

/// Ejecuta el proceso integral de limpieza y normalización sobre los metadatos de una pista.
pub fn sanitize_track_metadata(
    raw_title: &str,
    raw_artist: &str,
    raw_album: &str,
    file_path: &str,
) -> CleanedMetadata {
    // 1. Mojibake y caracteres de control
    let mut title = remove_control_chars(&fix_mojibake(raw_title));
    let mut artist = remove_control_chars(&fix_mojibake(raw_artist));
    let mut album = remove_control_chars(&fix_mojibake(raw_album));

    // 2. Si el título está vacío, derivarlo del nombre del archivo
    if title.trim().is_empty() && !file_path.is_empty() {
        if let Some(name) = std::path::Path::new(file_path).file_stem() {
            title = name.to_string_lossy().to_string();
        }
    }

    // 3. Quitar etiquetas basura y sufijos web
    title = strip_junk_tags(&title);
    artist = strip_junk_tags(&artist);
    album = strip_junk_tags(&album);

    // 4. Descomposición inteligente de "Artista - Título"
    // Si el artista está vacío o dice "Desconocido", pero el título contiene " - "
    let artist_is_unknown = artist.trim().is_empty()
        || artist.to_lowercase().contains("desconocido")
        || artist.to_lowercase().contains("unknown");

    if artist_is_unknown && title.contains(" - ") {
        let parts: Vec<&str> = title.splitn(2, " - ").collect();
        if parts.len() == 2 && !parts[0].trim().is_empty() && !parts[1].trim().is_empty() {
            artist = parts[0].trim().to_string();
            title = parts[1].trim().to_string();
        }
    }

    // 5. Normalizar espacios
    title = normalize_whitespace(&title);
    artist = normalize_whitespace(&artist);
    album = normalize_whitespace(&album);

    // 6. Title Case inteligente si viene en ALL CAPS o all lower
    title = smart_title_case(&title);
    artist = smart_title_case(&artist);
    album = smart_title_case(&album);

    // 7. Fallbacks si quedaron vacíos
    if title.is_empty() {
        title = "Pista sin título".to_string();
    }
    if artist.is_empty() {
        artist = if !raw_artist.trim().is_empty() {
            raw_artist.trim().to_string()
        } else {
            "Artista desconocido".to_string()
        };
    }
    if album.is_empty() {
        album = if !raw_album.trim().is_empty() {
            raw_album.trim().to_string()
        } else {
            "Álbum desconocido".to_string()
        };
    }

    let was_modified = title != raw_title || artist != raw_artist || album != raw_album;

    CleanedMetadata {
        title,
        artist,
        album,
        was_modified,
    }
}
