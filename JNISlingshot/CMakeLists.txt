cmake_minimum_required(VERSION 3.16)
project(JNISlingshot)

set(CMAKE_CXX_STANDARD 20)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /EHsc /arch:AVX2")

set(BUILD_SHARED_LIBS OFF) #nah
set(BUILD_CPR_TESTS OFF) #fuck off ffs i don't need them
set(USE_SYSTEM_CURL ON) #There's linking errors if you let cpr use it's own libcurl

add_subdirectory(cpr)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

find_package(fmt CONFIG REQUIRED)

add_subdirectory(JNI)
include_directories(JNI/include)

add_library(JNISlingshot SHARED library.cpp library.h json.h base64/base64.cpp base64/base64.h key.h bswapintrin.h)
add_library(JNISlingshotLoader SHARED loader.cpp loader.h)
add_executable(JNISlingshotExe Tests.cpp base64/base64.cpp base64/base64.h bswapintrin.h)

target_link_libraries(JNISlingshot PRIVATE cpr::cpr fmt::fmt JNI)
target_link_libraries(JNISlingshotExe PRIVATE cpr::cpr fmt::fmt JNI)