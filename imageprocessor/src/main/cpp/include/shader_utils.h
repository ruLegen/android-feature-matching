#ifndef FEATUREMATCHING_SHADER_UTILS_H
#define FEATUREMATCHING_SHADER_UTILS_H

#include "vector"
#include "string"
#include "log.h"
namespace Shaders{

std::vector<uint32_t> compileShader(const std::string &basicString,shaderc_shader_kind kind) {
    shaderc::Compiler compiler;
    shaderc::CompileOptions options;

    // Like -DMY_DEFINE=1
    //  options.AddMacroDefinition("MY_DEFINE", "1");

    auto module = compiler.CompileGlslToSpv(basicString,kind,"shader.com\0");


    if (module.GetNumErrors() > 0) {
        LOGE("%s",module.GetErrorMessage().c_str());
        throw std::runtime_error(module.GetErrorMessage().c_str());
    }

    std::vector<uint32_t> result(module.cbegin(), module.cend());
    return result;
}

const std::string ImageToGrayScaleShader =R"(
        #version 450
        layout (local_size_x = 1,local_size_y = 1) in;

        layout(set = 0, binding = 0,rgba8ui) uniform uimage2D img_input;
        layout(set = 0, binding = 1,rgba8ui) uniform uimage2D img_out;

        void main() {
            //vec3 pixel = imageLoad(img_input, ivec2(gl_GlobalInvocationID.xy)).rgb;
            //ivec4 pixel = ivec4(gl_GlobalInvocationID.x,gl_GlobalInvocationID.y,64,0);
//            imageStore(img_input, ivec2(gl_GlobalInvocationID.x,0),ivec4(255,0,0,0));
            uvec4 pixel = imageLoad(img_input, ivec2(gl_GlobalInvocationID.xy));
            imageStore(img_out, ivec2(gl_GlobalInvocationID.xy), pixel);
        }
    )";

}

#endif //FEATUREMATCHING_SHADER_UTILS_H
