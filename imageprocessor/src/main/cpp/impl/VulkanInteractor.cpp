
#include "VulkanInteractor.h"
#include <vulkan/vulkan.hpp>
#include "log.h"
#include "shaderc/shaderc.hpp"
#include "shader_utils.h"
#include <numeric>
#include <vector>

VulkanInteractor::VulkanInteractor()  :
        mgr()
{
    shImageToGrayScale = Shaders::compileShader(Shaders::ImageToGrayScaleShader,shaderc_shader_kind::shaderc_compute_shader);

   // RunCmd();

}
void VulkanInteractor::RunCmd() {
    int w = 10;
    int h = 10;
    auto data = std::make_shared<std::vector<int>>(w*h);
    std::iota(data->begin(),data->end(), 1);

    auto elementSize = sizeof((*data)[0]);
    auto image = mgr.image2d(vk::Format::eR8G8B8A8Uint,w,h,(*data).data(),data->size() * elementSize);
    auto outImage = mgr.image2d(vk::Format::eR8G8B8A8Uint, w, h, nullptr, data->size() * elementSize);

    //auto res = outImage->vector<unsigned int>();

    std::vector<std::shared_ptr<kp::KomputeResource>> params = {image,outImage};
    std::vector<std::shared_ptr<kp::Image2D>> images = {image,outImage};
    std::vector<std::shared_ptr<kp::Image2D>> syncImages = {outImage};
    //std::vector<std::shared_ptr<kp::Tensor>> tensors = {t};
    // 3. Create algorithm based on shader (supports buffers & push/spec constants)
    kp::Workgroup workgroup({static_cast<unsigned int>(w),
                             static_cast<unsigned int>(h),
                             1});

    auto algorithm = mgr.algorithm(params,shImageToGrayScale,workgroup);
    mgr.sequence()
            ->record(std::make_shared<kp::OpImageSyncStageBuffers>(images))
            ->record<kp::OpAlgoDispatch>(algorithm)
            ->eval();

    auto sq = mgr.sequence();
    sq->evalAsync(std::make_shared<kp::OpImageSyncDeviceData>(images));
    sq->evalAwait();

    auto res = outImage->vector<unsigned int >();

    for (auto&& color :res){
        auto red = (color & 0xFF000000) >> 24;
        auto green = (color & 0x00FF0000) >> 16;
        auto blue = (color & 0x0000FF00) >> 8;
        auto alpha = (color & 0x000000FF);
//        std::string f = Formatter() << "R " << red <<"|G "<<green<<"|B "<<blue<<"|A "<<alpha;
        std::string f = Formatter() <<color<<" ";

        LOGE(f.c_str());
    }
    // ... Do other work asynchronously whilst GPU finishes

    auto stop = 0;



}



/*
void kompute(const std::string& shader) {

    // 1. Create Kompute Manager with default settings (device 0, first queue and no extensions)

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

*/

std::vector<uint8_t>
VulkanInteractor::imageToGray(std::shared_ptr<std::vector<uint8_t>> image, int width, int height, int bytesPerPixel) {
    auto resultVector = std::vector<uint8_t>(width * height);

    //auto inputTensor = mgr.tensorT<uint8_t>(*image);
   // auto outputTensor = mgr.tensorT<uint8_t>(resultVector);
   // std::vector<std::shared_ptr<kp::Tensor>> params = {inputTensor,outputTensor};

    /*
      // Convert 3d image to 1D
    auto rawPtr = data.get();
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            int outActual1DIndex = ((i * width)+j);
            int input1DIndex = outActual1DIndex*bytesPerPixel;

            auto r = input[input1DIndex+0];
            rawPtr[outActual1DIndex] =   r;
        }
    }
     */
    return std::move(resultVector);
}

void VulkanInteractor::referenceTest() {
    std::string shader = (R"(
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
    // 2. Create and initialise Kompute Tensors through manager

    // Default tensor constructor simplifies creation of float values
    auto tensorInA = mgr.tensor({ 2., 2., 2. });
    auto tensorInB = mgr.tensor({ 1., 2., 3. });
    // Explicit type constructor supports uint32, int32, double, float and bool
    auto tensorOutA = mgr.tensorT<uint32_t>({ 0, 0, 0 });
    auto tensorOutB = mgr.tensorT<uint32_t>({ 0, 0, 0 });

    std::vector<std::shared_ptr<kp::KomputeResource>> algoParams = {tensorInA, tensorInB, tensorOutA, tensorOutB};
    std::vector<std::shared_ptr<kp::Tensor>> params = {tensorInA, tensorInB, tensorOutA, tensorOutB};

    // 3. Create algorithm based on shader (supports buffers & push/spec constants)
    kp::Workgroup workgroup({3, 1, 1});
    std::vector<float> specConsts({ 2 });
    std::vector<float> pushConstsA({ 2.0 });
    std::vector<float> pushConstsB({ 3.0 });

    auto algorithm = mgr.algorithm(algoParams,
            // See documentation shader section for compileSource
                                   Shaders::compileShader(shader,shaderc_shader_kind::shaderc_compute_shader),
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
    for (const float& elem : tensorOutA->vector()) LOGE("%f",elem);
    // Prints the second output which is: { 10, 10, 10 }
    for (const float& elem : tensorOutB->vector()) LOGE("%f",elem);

}







