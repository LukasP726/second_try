cmake_minimum_required(VERSION 3.29)
project(server2)

set(CMAKE_CXX_STANDARD 20)

add_executable(server2 main.cpp)
target_link_libraries(server2 PRIVATE ws2_32)
