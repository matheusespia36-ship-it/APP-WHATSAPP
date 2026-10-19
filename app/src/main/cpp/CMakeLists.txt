cmake_minimum_required(VERSION 3.18.1)
project("native-lib")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Inclui os cabeçalhos do Node.js (node.h) baixados no build
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include/node)

# Importa a biblioteca pré-compilada libnode.so
add_library(node SHARED IMPORTED)
set_target_properties(node PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libnode.so)

# Compila o nosso código C++ nativo
add_library(native-lib SHARED native-lib.cpp)

# Liga a nossa biblioteca às dependências (libnode.so e biblioteca de logs do Android)
target_link_libraries(
    native-lib
    node
    log
)
