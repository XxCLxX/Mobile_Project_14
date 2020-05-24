cmake_minimum_required(VERSION 3.7.0)

add_library(photoEditor SHARED photoEditor.cpp)

target_link_libraries(photoEditor android)