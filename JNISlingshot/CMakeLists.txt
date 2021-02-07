cmake_minimum_required(VERSION 3.16)
project(JNISlingshot)

set(CMAKE_CXX_STANDARD 20)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /EHsc")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /O2 /Oy") #Maximize speed
find_package(CURL CONFIG REQUIRED)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

find_package(fmt CONFIG REQUIRED)

add_subdirectory(JNI)
include_directories(JNI/include)

add_library(JNISlingshot SHARED library.cpp library.h json.h base64/base64.cpp base64/base64.h stb_image_write.h key.h bswapintrin.h)
add_library(JNISlingshotLoader SHARED loader.cpp loader.h)
add_executable(JNISlingshotExe utils/utils.h Tests.cpp base64/base64.cpp base64/base64.h jni_str.h stb_image_write.h)

target_link_libraries(JNISlingshot PRIVATE CURL::libcurl)
target_link_libraries(JNISlingshotExe PRIVATE CURL::libcurl)

target_link_libraries(JNISlingshot PRIVATE fmt::fmt)