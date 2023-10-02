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
    }

    std::vector<uint32_t> result(module.cbegin(), module.cend());
    return result;
}

const std::string ImageToGrayScaleShader =R"(
         #version 450

        layout (local_size_x = 1) in;

        // The input tensors bind index is relative to index in parameter passed
        layout(set = 0, binding = 0) buffer buf_in_a { int in_a[]; };
        layout(set = 0, binding = 1) buffer buf_out_a { int out_a[]; };

        // Kompute supports push constants updated on dispatch
        layout(push_constant) uniform PushConstants {
            float val;
        } push_const;

        // Kompute also supports spec constants on initalization
        layout(constant_id = 0) const float const_one = 0;

        void main() {
            uint index = gl_GlobalInvocationID.x;
            out_a[index] += uint( in_a[index] * in_b[index] );
            out_b[index] += uint( const_one * push_const.val );
        }
    )";

}

#endif //FEATUREMATCHING_SHADER_UTILS_H
