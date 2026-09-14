#!/usr/bin/env bash
# ==============================================================================
# SCRIPT DE LIMPIEZA EXHAUSTIVA DE RESIDUOS DE C++, CMAKE Y RUST
# ==============================================================================
# Purgado integral de directorios 'target' de Cargo, cachés de CMake,
# archivos intermedios de compiladores y artefactos temporales.
# ==============================================================================

set -e

echo "=========================================================="
echo "Iniciando purga exhaustiva de archivos basura de Rust y C++..."
echo "=========================================================="

# 1. Eliminar carpetas 'target' de Rust y archivos de metadatos de Cargo
echo "-> Purgando directorios 'target' y cachés de Rust..."
rm -rf rust_core/target
find . -type d -name "target" -not -path "*/.git/*" -exec rm -rf {} + 2>/dev/null || true
find . -type f \( -name ".rustc_info.json" -o -name "CACHEDIR.TAG" -o -name ".cargo-ok" -o -name ".cargo-lock" \) -not -path "*/.git/*" -delete 2>/dev/null || true

# 2. Eliminar directorios de compilación CXX y CMake de Android
echo "-> Purgando cachés de compilación C++ / CMake (.cxx y .externalNativeBuild)..."
rm -rf .cxx app/.cxx
rm -rf .externalNativeBuild app/.externalNativeBuild
rm -rf app/build/intermediates/cxx
rm -rf app/build/intermediates/cmake

# 3. Eliminar archivos temporales de objetos y dependencias de CMake / Ninja
echo "-> Purgando objetos compilados temporales (*.o, *.obj, *.ninja*, CMakeCache.txt)..."
find . -type f \( -name "*.o" -o -name "*.obj" -o -name "*.ninja*" -o -name "CMakeCache.txt" -o -name "compile_commands.json" \) -not -path "*/.git/*" -delete 2>/dev/null || true
find . -type d -name "CMakeFiles" -not -path "*/.git/*" -exec rm -rf {} + 2>/dev/null || true

# 4. Si se pasa el argumento --purgar-so o --all, purgar también los archivos binarios .so y jniLibs
if [[ "${1:-}" == "--purgar-so" ]] || [[ "${1:-}" == "--all" ]]; then
    echo "-> Purgando binarios compilados .so y carpetas jniLibs para evitar filtraciones..."
    rm -rf app/src/main/jniLibs
    find . -type f -name "*.so" -not -path "*/.git/*" -delete 2>/dev/null || true
    echo "   [✓] Binarios .so y carpetas jniLibs purgados con éxito."
fi

# 4. Verificación de seguridad: Si aún existe rust_core/target, ejecutar fallback en Python
if [ -d "rust_core/target" ]; then
    echo "-> [!] Directorio rust_core/target aún detectado. Ejecutando purga en Python..."
    python3 limpiar_archivos_nativos.py
fi

if [ ! -d "rust_core/target" ]; then
    echo "   [✓] rust_core/target y residuos de Rust eliminados al 100%."
fi

echo "=========================================================="
echo "[✓] Limpieza completada con éxito. Repositorio optimizado."
echo "=========================================================="

