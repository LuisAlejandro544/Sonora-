# ==============================================================================
# CONFIGURACIÓN DE CMAKE PARA EL MOTOR NATIVO C++ DE SONORA
# ==============================================================================
# Define la compilación de la librería compartida 'libsonora_dsp.so'.
# Compatible con arquitecturas Android de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
# ==============================================================================

cmake_minimum_required(VERSION 3.22.1)

project("sonora_dsp")

# Estándar C++17 para cálculos matemáticos modernos
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Compilación de la biblioteca compartida DSP y Motor Vocal
add_library(
    sonora_dsp
    SHARED
    sonora_dsp.cpp
    sonora_vocal.cpp
)

# Localizar la biblioteca nativa de logging de Android
find_library(
    log-lib
    log
)

# Vincular librerías del sistema: logging y libm (matemática)
target_link_libraries(
    sonora_dsp
    ${log-lib}
    m
)
