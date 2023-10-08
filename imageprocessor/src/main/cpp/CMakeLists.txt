cmake_minimum_required(VERSION 3.18.1)
SET(PROJECT "imageprocessor")

project("${PROJECT}")
set(KOMPUTE_OPT_ANDROID_BUILD ON)
set(KOMPUTE_OPT_DISABLE_VK_DEBUG_LAYERS ON)
set(KOMPUTE_OPT_USE_BUILT_IN_VULKAN_HEADER OFF)


set(SHADERC_BASE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/shaderc)
add_subdirectory("../../../../kompute" ${CMAKE_CURRENT_BINARY_DIR}/kompute_build)


file(GLOB SOURCES impl/*.cpp)

add_library(imageprocessor SHARED ${SOURCES})

target_include_directories(${PROJECT} PUBLIC include ${SHADERC_BASE_DIR}/include)

find_library(log-lib
            vulkan
            kompute
            log)
add_library(shaderc_lib SHARED IMPORTED)
set_target_properties(shaderc_lib PROPERTIES IMPORTED_LOCATION
        ${SHADERC_BASE_DIR}/libs/c++_shared/${ANDROID_ABI}/libshaderc.a)
		
		


set(VULKAN_INCLUDE_DIR ${ANDROID_NDK}/sources/third_party/vulkan/src/include)
target_sources(vulkanAndroid INTERFACE ${VULKAN_INCLUDE_DIR}/vulkan/vulkan.hpp)
target_include_directories(vulkanAndroid INTERFACE ${VULKAN_INCLUDE_DIR})

target_compile_definitions(vulkanAndroid INTERFACE VK_NO_PROTOTYPES=1)
target_compile_definitions(vulkanAndroid INTERFACE VULKAN_HPP_DISPATCH_LOADER_DYNAMIC=1)

target_link_libraries( # Specifies the target library.
		${PROJECT}
		#vulkanAndroid
		kompute::kompute
		# cpufeatures
		log
		shaderc_lib
		jnigraphics
		${log-lib})
