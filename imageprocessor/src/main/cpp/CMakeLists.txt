cmake_minimum_required(VERSION 3.18.1)

project("imageprocessor")

file(GLOB SOURCES impl/*.cpp)

add_library(imageprocessor SHARED ${SOURCES})
target_include_directories(imageprocessor PUBLIC include)

find_library(log-lib
            log)

target_link_libraries( # Specifies the target library.
        imageprocessor
       # cpufeatures
        jnigraphics
        ${log-lib})