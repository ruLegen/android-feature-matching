#ifndef FEATUREMATCHING_IMAGEPROCESSOR_H
#define FEATUREMATCHING_IMAGEPROCESSOR_H


#include <cstdint>

class ImageProcessor {
public:
    int detectCorners(uint8_t *input, uint8_t *output, int width, int height, int bytesPerPixel, uint8_t  threshold);
};


#endif //FEATUREMATCHING_IMAGEPROCESSOR_H
