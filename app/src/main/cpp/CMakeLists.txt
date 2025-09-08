cmake_minimum_required(VERSION 3.22.1)
project(whisperjni)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add source files
add_library(whisperjni SHARED 
    whisperjni.cpp
    whisper_context.cpp
)

# Find required libraries
find_library(log-lib log)

# Link libraries
target_link_libraries(whisperjni ${log-lib})

# Include directories
target_include_directories(whisperjni PRIVATE .)

