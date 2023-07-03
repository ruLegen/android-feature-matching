#ifndef FEATUREMATCHING_IMAGEPROCESSOR_H
#define FEATUREMATCHING_IMAGEPROCESSOR_H


#include <cstdint>

class ImageProcessor {

private:
    int some;

public:
    int getSome();
    void adjustBrightness(uint8_t *input, uint8_t *output, int width, int height, int bytesPerPixel,float scale);
};


#endif //FEATUREMATCHING_IMAGEPROCESSOR_H
