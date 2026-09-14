#!/usr/bin/env bash
# ==============================================================================
# SCRIPT DE GENERACIÓN AUTOMÁTICA DE FIRMA DEBUG PARA SONORA
# ==============================================================================
# Propósito:
#   Genera un almacén de claves 'debug.keystore' nuevo y autofirmado desde cero,
#   sin requerir interacción del usuario, contraseñas manuales ni secretos de CI.
#   Garantiza que los flujos de GitHub Actions o compilaciones locales de depuración
#   cuenten inmediatamente con una firma válida compatible con Android y Gradle.
#
# Parámetros esperados por Gradle en 'app/build.gradle.kts':
#   - Archivo:       debug.keystore
#   - Contraseña:    android
#   - Alias:         androiddebugkey
#   - Contraseña de clave: android
# ==============================================================================

set -euo pipefail

# Obtener directorio raíz del repositorio (donde reside este script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_KEYSTORE="${SCRIPT_DIR}/debug.keystore"

echo "=========================================================="
echo " [Sonora] Verificando y generando firma Debug desde cero  "
echo "=========================================================="

# 1. Localizar la herramienta 'keytool' del JDK
KEYTOOL_BIN=""
if command -v keytool >/dev/null 2>&1; then
    KEYTOOL_BIN="keytool"
elif [[ -n "${JAVA_HOME:-}" ]] && [[ -x "${JAVA_HOME}/bin/keytool" ]]; then
    KEYTOOL_BIN="${JAVA_HOME}/bin/keytool"
else
    # Buscar en ubicaciones comunes de JDK en entornos Linux / GitHub Runners
    for possible_path in \
        /usr/lib/jvm/java-17-openjdk-*/bin/keytool \
        /usr/lib/jvm/temurin-17-*/bin/keytool \
        /usr/lib/jvm/default-java/bin/keytool; do
        if [[ -x "${possible_path}" ]]; then
            KEYTOOL_BIN="${possible_path}"
            break
        fi
    done
fi

if [[ -z "${KEYTOOL_BIN}" ]]; then
    echo "[-] ERROR CRÍTICO: No se encontró la herramienta 'keytool' de Java."
    echo "    Asegúrate de tener instalado OpenJDK 17 o configurar JAVA_HOME."
    exit 1
fi

echo "[+] Herramienta keytool detectada: ${KEYTOOL_BIN}"

# 2. Comprobar si ya existe el almacén de claves
FORCE_RECREATE=false
if [[ "${1:-}" == "--force" ]] || [[ "${CI:-}" == "true" ]] || [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
    FORCE_RECREATE=true
fi

if [[ -f "${TARGET_KEYSTORE}" ]]; then
    if [[ "${FORCE_RECREATE}" == "true" ]]; then
        echo "[*] Entorno CI o bandera --force detectada: eliminando firma previa para crear una limpia desde cero..."
        rm -f "${TARGET_KEYSTORE}"
    else
        echo "[✓] El archivo debug.keystore ya existe en: ${TARGET_KEYSTORE}"
        echo "    Si deseas forzar una nueva generación desde cero, ejecuta: ./generar_firma_debug.sh --force"
        exit 0
    fi
fi

# 3. Generación no interactiva del almacén de claves 'debug.keystore'
echo "[*] Generando 'debug.keystore' desde cero con algoritmo RSA 2048..."
"${KEYTOOL_BIN}" -genkeypair \
    -v \
    -keystore "${TARGET_KEYSTORE}" \
    -storepass "android" \
    -alias "androiddebugkey" \
    -keypass "android" \
    -keyalg "RSA" \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" \
    -noprompt

# 4. Validar la integridad del certificado generado
if [[ -f "${TARGET_KEYSTORE}" ]]; then
    echo "[*] Verificando integridad de la firma recién generada..."
    "${KEYTOOL_BIN}" -list \
        -keystore "${TARGET_KEYSTORE}" \
        -storepass "android" \
        -alias "androiddebugkey" > /dev/null 2>&1

    echo "=========================================================="
    echo " [✓] ¡Firma Debug generada con éxito!"
    echo "     Ubicación: ${TARGET_KEYSTORE}"
    echo "     Alias:     androiddebugkey"
    echo "     Algoritmo: RSA 2048 bits (Validez: 10000 días)"
    echo "=========================================================="
    exit 0
else
    echo "[-] ERROR: Falló la creación del archivo 'debug.keystore'."
    exit 1
fi
