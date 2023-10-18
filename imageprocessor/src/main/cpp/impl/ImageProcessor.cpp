#include "ImageProcessor.h"
#include "log.h"
ImageProcessor::ImageProcessor():
    f9(),
    vulkanInteractor()
{
}

int ImageProcessor::detectCorners(uint8_t *input, uint8_t *output, int width, int height, int bytesPerPixel, uint8_t threshold) {
    auto inputImage = std::shared_ptr<std::vector<uint8_t>> (new std::vector(&input[0],&input[width*height*bytesPerPixel]));

    auto data = vulkanInteractor.imageToGray(inputImage,width,height,bytesPerPixel);
    auto corners = f9.detectCorners(data.data(),width,height,width,threshold, false);

    for(auto location : corners){
        int actual1DIndex = ((location.y * width)+location.x)*bytesPerPixel;
        output[actual1DIndex +0] = 255;     //r
        output[actual1DIndex +3] = 255;     //a
    }
    return  corners.size();
}


