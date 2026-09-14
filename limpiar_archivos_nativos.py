#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de limpieza exhaustiva de residuos nativos de Rust y C++ para Sonora.
Elimina carpetas 'target', cachés intermedias de CMake y archivos basura de compilación.
"""

import os
import shutil
import sys

def purgar_directorio(ruta):
    """Elimina un directorio y todo su contenido si existe."""
    if os.path.exists(ruta):
        try:
            shutil.rmtree(ruta, ignore_errors=False)
            print(f" [✓] Eliminado con éxito: {ruta}")
            return True
        except Exception as e:
            print(f" [!] Error al eliminar {ruta}: {e}")
            try:
                # Intento forzado con shutil ignore_errors
                shutil.rmtree(ruta, ignore_errors=True)
                print(f" [✓] Eliminado (forzado): {ruta}")
                return True
            except Exception as e2:
                print(f" [X] Fallo crítico al purgar {ruta}: {e2}")
                return False
    return False

def purgar_archivos_patron(directorio_raiz, nombres_o_extensiones):
    """Busca y elimina archivos que coincidan con nombres específicos o extensiones."""
    contador = 0
    for raiz, dirs, archivos in os.walk(directorio_raiz):
        # Evitar tocar .git
        if ".git" in raiz:
            continue
        for archivo in archivos:
            coincide = False
            for patron in nombres_o_extensiones:
                if patron.startswith(".") and archivo.endswith(patron):
                    coincide = True
                    break
                elif archivo == patron:
                    coincide = True
                    break
            if coincide:
                ruta_completa = os.path.join(raiz, archivo)
                try:
                    os.remove(ruta_completa)
                    contador += 1
                except Exception:
                    pass
    return contador

def main():
    print("==========================================================")
    print("Iniciando purga en Python de archivos basura de Rust y C++")
    print("==========================================================")

    # 1. Purgar carpetas target de rust_core
    rutas_a_eliminar = [
        "rust_core/target",
        ".cxx",
        "app/.cxx",
        ".externalNativeBuild",
        "app/.externalNativeBuild",
        "app/build/intermediates/cxx",
        "app/build/intermediates/cmake"
    ]

    for ruta in rutas_a_eliminar:
        if os.path.isdir(ruta):
            purgar_directorio(ruta)

    # 2. Buscar cualquier otra carpeta llamada target fuera de .git
    for raiz, dirs, _ in os.walk("."):
        if ".git" in raiz:
            continue
        for d in list(dirs):
            if d == "target" or d == "CMakeFiles":
                purgar_directorio(os.path.join(raiz, d))

    # 3. Purgar archivos temporales de Rust y CMake
    patrones_basura = [
        ".rustc_info.json",
        "CACHEDIR.TAG",
        ".cargo-ok",
        ".cargo-lock",
        "CMakeCache.txt",
        ".ninja_deps",
        ".ninja_log"
    ]
    eliminados = purgar_archivos_patron(".", patrones_basura)
    print(f" [✓] Archivos temporales purgados: {eliminados}")

    # 4. Purgar archivos .so si se especifica el argumento --purgar-so o --all
    if len(sys.argv) > 1 and sys.argv[1] in ["--purgar-so", "--all"]:
        print("-> Purgando binarios compilados .so y carpetas jniLibs...")
        purgar_directorio("app/src/main/jniLibs")
        so_eliminados = purgar_archivos_patron(".", [".so"])
        print(f" [✓] Binarios .so eliminados para proteger el repositorio: {so_eliminados}")

    # Verificación final
    if not os.path.exists("rust_core/target"):
        print(" [✓] Verificación: rust_core/target no existe. Limpieza completada.")
    else:
        print(" [!] Advertencia: rust_core/target aún existe.")

    print("==========================================================")

if __name__ == "__main__":
    main()
