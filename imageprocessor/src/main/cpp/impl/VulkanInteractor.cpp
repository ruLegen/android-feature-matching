
#include "VulkanInteractor.h"
#include <vulkan/vulkan.hpp>
#include "log.h"
#include "shaderc/shaderc.hpp"
#include "shader_utils.h"
#include <vector>

VulkanInteractor::VulkanInteractor()  :
        mgr()
{
    shImageToGrayScale = Shaders::compileShader(Shaders::ImageToGrayScaleShader,shaderc_shader_kind::shaderc_compute_shader);
     auto data = std::make_shared<std::vector<int>>(100*100);


    auto image = mgr.image2d(vk::Format::eR8G8B8A8Sint,100,100,(*data).data());

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






