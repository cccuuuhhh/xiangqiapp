# CMake build configuration for Pikafish JNI Bridge
# 话唠棋王 — B+ 方案 Native 层
cmake_minimum_required(VERSION 3.22.1)

project("pikafish_jni")

# Android 日志库
find_library(log-lib log)

# Pikafish 引擎库 (预编译，放入 jniLibs)
add_library(pikafish SHARED IMPORTED)
set_target_properties(pikafish PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libpikafish.so
)

# JNI 桥接层
add_library(pikafish_bridge SHARED
    pikafish_jni.cpp
)

target_include_directories(pikafish_bridge PRIVATE
    ${CMAKE_SOURCE_DIR}
)

target_link_libraries(pikafish_bridge
    ${log-lib}
    pikafish
)
