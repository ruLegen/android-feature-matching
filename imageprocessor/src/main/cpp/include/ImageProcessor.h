#ifndef FEATUREMATCHING_IMAGEPROCESSOR_H
#define FEATUREMATCHING_IMAGEPROCESSOR_H


#include <cstdint>
#include "f9.h"

class ImageProcessor {
private:
    F9 f9;
    std::vector<F9_CORNER> cornerPoints;
public:
    ImageProcessor();
    int detectCorners(uint8_t *input, uint8_t *output, int width, int height, int bytesPerPixel, uint8_t  threshold);
    const std::vector<F9_CORNER>& detectCornersOwnImplenentation(uint8_t *image_data, int width, int height,int bytes_per_row, uint8_t threshold,bool suppress_non_max);

};


#endif //FEATUREMATCHING_IMAGEPROCESSOR_H
