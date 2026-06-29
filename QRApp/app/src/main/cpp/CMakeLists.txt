cmake_minimum_required(VERSION 3.22.1)
project("qr_native")

add_library(
    native-lib
    SHARED
    native-lib.cpp
    image_processor.cpp
    watermark.cpp
    qrcodegen.cpp
)

find_library(jnigraphics-lib jnigraphics)
find_library(log-lib log)

target_link_libraries(
    native-lib
    ${jnigraphics-lib}
    ${log-lib}
)
