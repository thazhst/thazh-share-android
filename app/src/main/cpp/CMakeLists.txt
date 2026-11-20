cmake_minimum_required(VERSION 3.22.1)

project("thazh_core" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(
    thazh_core
    SHARED
    thazh_core.cpp
)

target_compile_options(
    thazh_core
    PRIVATE
    -O3
    -ffast-math
    -fno-exceptions
    -fno-rtti
    -fvisibility=hidden
    -Wall
    -Wextra
)

find_library(
    log-lib
    log
)

target_link_libraries(
    thazh_core
    ${log-lib}
)
