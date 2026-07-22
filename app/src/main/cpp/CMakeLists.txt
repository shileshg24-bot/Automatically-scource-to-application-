cmake_minimum_required(VERSION 3.22.1)

project("bn_native")

add_library(
        bn_native
        SHARED
        bn_native.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        bn_native
        ${log-lib})
