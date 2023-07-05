#ifndef FEATUREMATCHING_IMAGEPROCESSOR_H
#define FEATUREMATCHING_IMAGEPROCESSOR_H


#include <cstdint>
#include "f9.h"

class ImageProcessor {
private:
    F9 f9;
public:
    ImageProcessor();
    int detectCorners(uint8_t *input, uint8_t *output, int width, int height, int bytesPerPixel, uint8_t  threshold);
};


#endif //FEATUREMATCHING_IMAGEPROCESSOR_H
