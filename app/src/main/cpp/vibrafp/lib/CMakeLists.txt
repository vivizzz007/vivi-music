cmake_minimum_required(VERSION 3.22)
project(vibra_fp LANGUAGES CXX)

# ========== Options ==========
option(ENABLE_LTO "Enable thin-LTO compile/link flags" ON)

# Optional: user can pass -DFFTW3_PATH=/path/to/install-android-fftw
# Expected layout if FFTW3_PATH is provided:
#   <FFTW3_PATH>/<abi>/include/   -> headers
#   <FFTW3_PATH>/<abi>/lib/libfftw3.a  -> static lib
if(NOT DEFINED FFTW3_PATH)
    set(FFTW3_PATH "" CACHE PATH "Path to FFTW install root (optional)")
endif()

message(STATUS "Project source dir: ${CMAKE_SOURCE_DIR}")

# ========== Sources ==========
set(LIBVIBRA_SOURCES
        vibra.cpp
        vibra_jni.cpp
        algorithm/signature.cpp
        algorithm/frequency.cpp
        algorithm/signature_generator.cpp
        audio/wav.cpp
        audio/downsampler.cpp
)

add_library(vibra_fp SHARED ${LIBVIBRA_SOURCES})

target_include_directories(vibra_fp
        PRIVATE
        ${CMAKE_SOURCE_DIR}/../include
        ${CMAKE_SOURCE_DIR}
        ${CMAKE_SOURCE_DIR}/algorithm
        ${CMAKE_SOURCE_DIR}/audio
        ${CMAKE_SOURCE_DIR}/utils
)

# ========== FFTW detection and import ==========
set(FFTW3_INCLUDE_DIR "")
set(FFTW3_STATIC_LIB "")

if(FFTW3_PATH)
    # fallback layout
    set(FFTW3_STATIC_LIB "${FFTW3_PATH}/lib/libfftw3.a")
    set(FFTW3_INCLUDE_DIR "${FFTW3_PATH}/include")
    # prefer per-ABI layout if building for Android and file exists
    if(ANDROID AND ANDROID_ABI)
        set(CANDIDATE_LIB "${FFTW3_PATH}/${ANDROID_ABI}/lib/libfftw3.a")
        if(EXISTS "${CANDIDATE_LIB}")
            set(FFTW3_STATIC_LIB "${CANDIDATE_LIB}")
            set(FFTW3_INCLUDE_DIR "${FFTW3_PATH}/${ANDROID_ABI}/include")
        endif()
    endif()
endif()

# If FFTW3_PATH not provided, try the repository-local default layout:
# <CMAKE_SOURCE_DIR>/third_party/fftw-android/<ANDROID_ABI>/lib/libfftw3.a
if(ANDROID AND (NOT FFTW3_STATIC_LIB OR NOT EXISTS "${FFTW3_STATIC_LIB}"))
    set(LOCAL_THIRD "${CMAKE_SOURCE_DIR}/../third_party/fftw-android")
    if(DEFINED ANDROID_ABI)
        set(LOCAL_LIB "${LOCAL_THIRD}/${ANDROID_ABI}/lib/libfftw3.a")
        if(EXISTS "${LOCAL_LIB}")
            set(FFTW3_STATIC_LIB "${LOCAL_LIB}")
            set(FFTW3_INCLUDE_DIR "${LOCAL_THIRD}/${ANDROID_ABI}/include")
        endif()
    endif()
endif()

# Fallback: Build from source using FetchContent if not found
if(NOT FFTW3_STATIC_LIB OR NOT EXISTS "${FFTW3_STATIC_LIB}")
    message(STATUS "FFTW3 not found locally. Fetching and building from source...")
    include(FetchContent)
    
    # Use the locally downloaded tarball if available
    set(FFTW_LOCAL_TARBALL "${CMAKE_SOURCE_DIR}/../third_party/fftw-3.3.10.tar.gz")
    FetchContent_Declare(
      fftw3
      URL "${FFTW_LOCAL_TARBALL}"
      URL_HASH MD5=8ccbf6a5ea78a16dbc3e1306e234cc5c
    )
    
    # Configure FFTW options for a minimal static build
    set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
    set(BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(ENABLE_THREADS ON CACHE BOOL "" FORCE)
    set(ENABLE_OPENMP OFF CACHE BOOL "" FORCE)
    
    FetchContent_MakeAvailable(fftw3)
endif()

## Create an IMPORTED target or link to the built target
if(FFTW3_STATIC_LIB AND EXISTS "${FFTW3_STATIC_LIB}")
    add_library(fftw3_static STATIC IMPORTED GLOBAL)
    set_target_properties(fftw3_static PROPERTIES
            IMPORTED_LOCATION "${FFTW3_STATIC_LIB}"
            INTERFACE_INCLUDE_DIRECTORIES "${FFTW3_INCLUDE_DIR}"
    )
    message(STATUS "Using FFTW (imported static): ${FFTW3_STATIC_LIB}")
    target_link_libraries(vibra_fp PRIVATE fftw3_static)
elseif(TARGET fftw3)
    message(STATUS "Using FFTW (built from source)")
    # When building from source using FetchContent, we need to point to the header location
    target_include_directories(vibra_fp PRIVATE ${fftw3_SOURCE_DIR}/api)
    target_link_libraries(vibra_fp PRIVATE fftw3)
else()
    message(FATAL_ERROR "FFTW3 static library not found and failed to build from source.")
endif()

# ========== C++ standard ==========
set_target_properties(vibra_fp PROPERTIES CXX_STANDARD 11 CXX_STANDARD_REQUIRED YES)

# ========== Android system libs ==========
if (ANDROID)
    target_link_libraries(vibra_fp PRIVATE log m)
endif()

# ========== Compiler / Linker options ==========
# Common options (Release vs Debug)
target_compile_options(vibra_fp PRIVATE
        $<$<CONFIG:Release>:-O2>
        $<$<NOT:$<CONFIG:Release>>:-O0>
        $<$<NOT:$<CONFIG:Release>>:-g>
        $<$<CONFIG:Release>:-ffunction-sections>
        $<$<CONFIG:Release>:-fdata-sections>
        $<$<CONFIG:Release>:-fvisibility=hidden>
        $<$<CONFIG:Release>:-fomit-frame-pointer>
        $<$<CONFIG:Release>:-fPIC>
)

# Add thin-LTO flags only if requested
if(ENABLE_LTO)
    target_compile_options(vibra_fp PRIVATE
            $<$<CONFIG:Release>:-flto=thin>
    )
    target_link_options(vibra_fp PRIVATE
            $<$<CONFIG:Release>:-flto=thin>
            $<$<CONFIG:Release>:-Wl,--gc-sections>
            $<$<CONFIG:Release>:-Wl,--strip-all>
    )
else()
    target_link_options(vibra_fp PRIVATE
            $<$<CONFIG:Release>:-Wl,--gc-sections>
            $<$<CONFIG:Release>:-Wl,--strip-all>
    )
endif()

# ========== Limit exported symbols ==========
set(VIBRA_EXPORT_SCRIPT "${CMAKE_SOURCE_DIR}/vibra.sym")
if(EXISTS "${VIBRA_EXPORT_SCRIPT}")
    message(STATUS "Using version script: ${VIBRA_EXPORT_SCRIPT}")
    target_link_options(vibra_fp PRIVATE "-Wl,--version-script=${VIBRA_EXPORT_SCRIPT}")
endif()
