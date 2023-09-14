#ifndef FEATUREMATCHING_VULKANINTERACTOR_H
#define FEATUREMATCHING_VULKANINTERACTOR_H
#include "vulkan.hpp"

class VulkanInteractor {
private:
    vk::Instance vkInstance;

public:
    void init();
};
#endif //FEATUREMATCHING_VULKANINTERACTOR_H
