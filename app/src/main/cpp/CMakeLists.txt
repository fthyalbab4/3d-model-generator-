cmake_minimum_required(VERSION 3.22.1)

project("ai_inference_engine")

# Enable C++17 for modern memory and template features
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Architecture-specific optimization flags for Android NDK
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -march=armv8-a+simd")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -mfpu=neon")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")
endif()

# Add native library target
add_library(
    ai_inference_engine
    SHARED
    native-lib.cpp
    MemoryManager.cpp
    HardwareAccelerator.cpp
    GGUFParser.cpp
    TripoSRForward.cpp
    MarchingCubesNDK.cpp
    GaussianSplatEngine.cpp
)

# Find required Android NDK and system libraries
find_library(
    log-lib
    log
)

find_library(
    vulkan-lib
    vulkan
)

# Link libraries to our native engine
target_link_libraries(
    ai_inference_engine
    ${log-lib}
    ${vulkan-lib}
)
