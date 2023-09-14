
#include "VulkanInteractor.h"
#include "vulkan.hpp"
#include "log.h"

static std::string AppName    = "01_InitInstance";
static std::string EngineName = "Vulkan.hpp";

void VulkanInteractor::init() {
    try
    {
        // initialize the vk::ApplicationInfo structure
        vk::ApplicationInfo applicationInfo( AppName.c_str(), 1, EngineName.c_str(), 1, VK_API_VERSION_1_1 );

        // initialize the vk::InstanceCreateInfo
        vk::InstanceCreateInfo instanceCreateInfo( {}, &applicationInfo );

        // create an Instance
        vkInstance = vk::createInstance( instanceCreateInfo );

        auto deviceIt = instance.enumeratePhysicalDevices();
        for(auto&& dev:deviceIt){
            auto props = dev.getProperties();
            LOGD("%s",props.deviceName);
        }


        // destroy it again
//        instance.destroy();
    }
    catch ( vk::SystemError & err )
    {
        exit( -1 );
    }
    catch ( std::exception & err )
    {
        exit( -1 );
    }
    catch ( ... )
    {
        exit( -1 );
    }
}
