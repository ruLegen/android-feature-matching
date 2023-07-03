#include "ImageProcessor.h"
#include "Fast.h"
#include "Utils.h"
int ImageProcessor::getSome() {
    return this->some;
}

void ImageProcessor::adjustBrightness(uint8_t *input, uint8_t *output, int width, int height,int bytesPerPixel, float scale) {
    std::vector<fast_xy> corners;

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
    fast_corner_detect_10(data.get(),width,height,width,1,corners);
//    ALOGI("%d",corners.size());
    for(auto location : corners){
        int actual1DIndex = ((location.y * width)+location.x)*bytesPerPixel;
        output[actual1DIndex +0] = 255;     //r
        output[actual1DIndex +3] = 255;     //a
    }
    /*
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            int actual1DIndex = ((i * width)+j)*stride;
            auto r = (uint8_t)(input[actual1DIndex+0]-50);
            auto g = (uint8_t)(input[actual1DIndex+1]-40);
            auto b = (uint8_t)(input[actual1DIndex+2]-54);
           // auto a = (uint8_t)(input[actual1DIndex+3]*scale);
            output[actual1DIndex + 0] = r;
            output[actual1DIndex + 1] = g;
            output[actual1DIndex + 2] = b;
            output[actual1DIndex + 3] = 255;        // a
        }
    }*/
}
