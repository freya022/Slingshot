cmake_minimum_required(VERSION 3.17)
project(JNI)

set(CMAKE_CXX_STANDARD 20)

add_compile_options("/EHsc")

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

find_package(fmt CONFIG REQUIRED)

include_directories(include)
add_library(JNI src/Class.cpp src/Helper.cpp src/String.cpp)

target_link_libraries(JNI PRIVATE fmt::fmt)