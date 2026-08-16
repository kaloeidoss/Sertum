cmake_minimum_required(VERSION 3.22.1)
project(sertumspike CXX)

add_library(sertumspike SHARED spike1_aaudio.cpp)

find_library(aaudio-lib aaudio)
find_library(android-lib android)
find_library(log-lib log)

target_link_libraries(sertumspike ${aaudio-lib} ${android-lib} ${log-lib})
