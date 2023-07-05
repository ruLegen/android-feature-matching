#include "ImageProcessor.h"
#include "Utils.h"
ImageProcessor::ImageProcessor():
    f9()
{

}

int ImageProcessor::detectCorners(uint8_t *input, uint8_t *output, int width, int height, int bytesPerPixel, uint8_t threshold) {
    std::unique_ptr<uint8_t[]> data = std::unique_ptr<uint8_t[]>(new uint8_t[width*height]);
    auto rawPtr = data.get();
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            int outActual1DIndex = ((i * width)+j);
            int input1DIndex = outActual1DIndex*bytesPerPixel;

            auto r = input[input1DIndex+0];
            rawPtr[outActual1DIndex] =   r;
        }
    }
    auto corners = f9.detectCorners(data.get(),width,height,width,threshold, false);
    for(auto location : corners){
        int actual1DIndex = ((location.y * width)+location.x)*bytesPerPixel;
        output[actual1DIndex +0] = 255;     //r
        output[actual1DIndex +3] = 255;     //a
    }
    return  corners.size();
}

