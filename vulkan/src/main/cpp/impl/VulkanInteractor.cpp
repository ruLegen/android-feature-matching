
#include "VulkanInteractor.h"
#include "vulkan.hpp"
#include "log.h"
#include "Kompute.hpp"
#include "shaderc/shaderc.hpp"


std::vector<uint32_t> compileShader(const std::string &basicString,shaderc_shader_kind kind) {
    shaderc::Compiler compiler;
    shaderc::CompileOptions options;

    // Like -DMY_DEFINE=1
    options.AddMacroDefinition("MY_DEFINE", "1");

    auto module = compiler.CompileGlslToSpv(basicString,kind,"shader.com\0");


    if (module.GetNumErrors() > 0) {
        std::cerr << module.GetErrorMessage();
    }

    std::vector<uint32_t> result(module.cbegin(), module.cend());
    return result;
}

void kompute(const std::string& shader) {

    // 1. Create Kompute Manager with default settings (device 0, first queue and no extensions)
    kp::Manager mgr;

    // 2. Create and initialise Kompute Tensors through manager

    // Default tensor constructor simplifies creation of float values
    auto tensorInA = mgr.tensor({ 2., 2., 2. });
    auto tensorInB = mgr.tensor({ 1., 2., 3. });
    // Explicit type constructor supports uint32, int32, double, float and bool
    auto tensorOutA = mgr.tensorT<uint32_t>({ 0, 0, 0 });
    auto tensorOutB = mgr.tensorT<uint32_t>({ 0, 0, 0 });

    std::vector<std::shared_ptr<kp::Tensor>> params = {tensorInA, tensorInB, tensorOutA, tensorOutB};

    // 3. Create algorithm based on shader (supports buffers & push/spec constants)
    kp::Workgroup workgroup({3, 1, 1});
    std::vector<float> specConsts({ 2 });
    std::vector<float> pushConstsA({ 2.0 });
    std::vector<float> pushConstsB({ 3.0 });

    auto algorithm = mgr.algorithm(params,
                                   compileShader(shader,shaderc_shader_kind::shaderc_compute_shader),
                                   workgroup,
                                   specConsts,
                                   pushConstsA);

    // 4. Run operation synchronously using sequence
    mgr.sequence()
            ->record<kp::OpTensorSyncDevice>(params)
            ->record<kp::OpAlgoDispatch>(algorithm) // Binds default push consts
            ->eval() // Evaluates the two recorded operations
            ->record<kp::OpAlgoDispatch>(algorithm, pushConstsB) // Overrides push consts
            ->eval(); // Evaluates only last recorded operation

    // 5. Sync results from the GPU asynchronously
    auto sq = mgr.sequence();
    sq->evalAsync<kp::OpTensorSyncLocal>(params);

    // ... Do other work asynchronously whilst GPU finishes

    sq->evalAwait();

    // Prints the first output which is: { 4, 8, 12 }
    for (const float& elem : tensorOutA->vector()) LOGE("%f ",elem);
    // Prints the second output which is: { 10, 10, 10 }
    for (const float& elem : tensorOutB->vector()) LOGE("%f ",elem);

}


// Manages / releases all CPU and GPU memory resources


void VulkanInteractor::init() {
    try
    {
        // Define your shader as a string (using string literals for simplicity)
        // (You can also pass the raw compiled bytes, or even path to file)
        std::string shader(R"(
         #version 450

        layout (local_size_x = 1) in;

        // The input tensors bind index is relative to index in parameter passed
        layout(set = 0, binding = 0) buffer buf_in_a { float in_a[]; };
        layout(set = 0, binding = 1) buffer buf_in_b { float in_b[]; };
        layout(set = 0, binding = 2) buffer buf_out_a { uint out_a[]; };
        layout(set = 0, binding = 3) buffer buf_out_b { uint out_b[]; };

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
    )");
        kompute(shader);
    }
    catch ( ... )
    {
        exit( -1 );
    }
}





