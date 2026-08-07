# ============================================================================
# CMake Build Configuration — 话唠棋王 Native 层
# ============================================================================
# 编译 Pikafish 引擎（静态库）+ 进程内包装器 + JNI 桥接层
# ============================================================================
cmake_minimum_required(VERSION 3.22.1)
project("pikafish_bridge")

# ============================================================================
# Android 日志库
# ============================================================================
find_library(log-lib log)

# ============================================================================
# Pikafish 引擎源文件（静态库）
# ============================================================================

# Pikafish 源码根目录
set(PIKAFISH_SRC_DIR ${CMAKE_SOURCE_DIR}/../../../vendor/pikafish/src)

# 核心源文件（排除 main.cpp — 由 wrapper 替代）
set(PIKAFISH_CORE_SRCS
    ${PIKAFISH_SRC_DIR}/attacks.cpp
    ${PIKAFISH_SRC_DIR}/benchmark.cpp
    ${PIKAFISH_SRC_DIR}/bitboard.cpp
    ${PIKAFISH_SRC_DIR}/engine.cpp
    ${PIKAFISH_SRC_DIR}/evaluate.cpp
    ${PIKAFISH_SRC_DIR}/memory.cpp
    ${PIKAFISH_SRC_DIR}/misc.cpp
    ${PIKAFISH_SRC_DIR}/movegen.cpp
    ${PIKAFISH_SRC_DIR}/movepick.cpp
    ${PIKAFISH_SRC_DIR}/position.cpp
    ${PIKAFISH_SRC_DIR}/score.cpp
    ${PIKAFISH_SRC_DIR}/search.cpp
    ${PIKAFISH_SRC_DIR}/thread.cpp
    ${PIKAFISH_SRC_DIR}/timeman.cpp
    ${PIKAFISH_SRC_DIR}/tt.cpp
    ${PIKAFISH_SRC_DIR}/tune.cpp
    ${PIKAFISH_SRC_DIR}/uci.cpp
    ${PIKAFISH_SRC_DIR}/ucioption.cpp
)

# NNUE 源文件
set(PIKAFISH_NNUE_SRCS
    ${PIKAFISH_SRC_DIR}/nnue/network.cpp
    ${PIKAFISH_SRC_DIR}/nnue/nnue_accumulator.cpp
    ${PIKAFISH_SRC_DIR}/nnue/nnue_misc.cpp
    ${PIKAFISH_SRC_DIR}/nnue/features/full_threats.cpp
    ${PIKAFISH_SRC_DIR}/nnue/features/half_ka_v2_hm.cpp
)

# External / 压缩库源文件
set(PIKAFISH_EXTERNAL_SRCS
    ${PIKAFISH_SRC_DIR}/external/common/debug.cpp
    ${PIKAFISH_SRC_DIR}/external/common/entropy_common.cpp
    ${PIKAFISH_SRC_DIR}/external/common/error_private.cpp
    ${PIKAFISH_SRC_DIR}/external/common/fse_decompress.cpp
    ${PIKAFISH_SRC_DIR}/external/common/pool.cpp
    ${PIKAFISH_SRC_DIR}/external/common/threading.cpp
    ${PIKAFISH_SRC_DIR}/external/common/xxhash.cpp
    ${PIKAFISH_SRC_DIR}/external/common/zstd_common.cpp
    ${PIKAFISH_SRC_DIR}/external/decompress/huf_decompress.cpp
    ${PIKAFISH_SRC_DIR}/external/decompress/zstd_ddict.cpp
    ${PIKAFISH_SRC_DIR}/external/decompress/zstd_decompress.cpp
    ${PIKAFISH_SRC_DIR}/external/decompress/zstd_decompress_block.cpp
)

# 通用入口点（ARM64）
set(PIKAFISH_ENTRY_SRCS
    ${PIKAFISH_SRC_DIR}/universal/entry_arm64.cpp
)

# 合并所有源文件
set(PIKAFISH_ALL_SRCS
    ${PIKAFISH_CORE_SRCS}
    ${PIKAFISH_NNUE_SRCS}
    ${PIKAFISH_EXTERNAL_SRCS}
    ${PIKAFISH_ENTRY_SRCS}
)

# Pikafish 头文件搜索路径
set(PIKAFISH_INCLUDE_DIRS
    ${PIKAFISH_SRC_DIR}
    ${PIKAFISH_SRC_DIR}/nnue
    ${PIKAFISH_SRC_DIR}/nnue/features
    ${PIKAFISH_SRC_DIR}/external/common
    ${PIKAFISH_SRC_DIR}/external/decompress
    ${PIKAFISH_SRC_DIR}/universal
)

# ============================================================================
# Pikafish 静态库
# ============================================================================
add_library(pikafish_static STATIC ${PIKAFISH_ALL_SRCS})

target_include_directories(pikafish_static PRIVATE ${PIKAFISH_INCLUDE_DIRS})

# ARM64 编译标志（对应 Makefile: ARCH=armv8）
target_compile_definitions(pikafish_static PRIVATE
    IS_64BIT
    USE_PREFETCH
    USE_POPCNT
    USE_NEON
    NO_PEXT
)

# C++ 标准 + 优化标志
target_compile_options(pikafish_static PRIVATE
    -std=c++17
    -O3
    -fno-exceptions
    -fno-rtti
    -flto=thin
)

# ============================================================================
# Pikafish Wrapper 库（进程内引擎管理）
# ============================================================================
add_library(pikafish_wrapper STATIC
    ${CMAKE_SOURCE_DIR}/../../../vendor/pikafish_wrapper.cpp
)

target_include_directories(pikafish_wrapper PRIVATE
    ${PIKAFISH_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/../../../vendor
)

target_compile_definitions(pikafish_wrapper PRIVATE
    IS_64BIT
    USE_PREFETCH
    USE_POPCNT
    USE_NEON
    NO_PEXT
)

target_compile_options(pikafish_wrapper PRIVATE
    -std=c++17
    -O2
)

# ============================================================================
# JNI Bridge 共享库（最终产物：libpikafish_bridge.so）
# ============================================================================
add_library(pikafish_bridge SHARED
    pikafish_jni.cpp
)

target_include_directories(pikafish_bridge PRIVATE
    ${PIKAFISH_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/../../../vendor
)

target_compile_options(pikafish_bridge PRIVATE
    -std=c++17
    -O2
)

# 链接：JNI Bridge → Wrapper → Pikafish Static → log
target_link_libraries(pikafish_bridge
    pikafish_wrapper
    pikafish_static
    ${log-lib}
)
