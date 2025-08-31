cmake_minimum_required(VERSION 3.22.1)
project(whisperjni)

add_library(whisperjni SHARED whisperjni.cpp)

find_library(log-lib log)

target_link_libraries(whisperjni ${log-lib})

