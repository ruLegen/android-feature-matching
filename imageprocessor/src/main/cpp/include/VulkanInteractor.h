#ifndef FEATUREMATCHING_VULKANINTERACTOR_H
#define FEATUREMATCHING_VULKANINTERACTOR_H
#include "vulkan.hpp"
#include "kompute/Kompute.hpp"

class VulkanInteractor {
private:
    kp::Manager mgr;

    std::vector<uint32_t> shImageToGrayScale;
public:
    VulkanInteractor();
    std::vector<uint8_t> imageToGray(std::shared_ptr<std::vector<uint8_t>> image, int width, int height, int bytesPerPixel);


};
#endif //FEATUREMATCHING_VULKANINTERACTOR_H
