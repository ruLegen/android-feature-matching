#ifndef FEATUREMATCHING_FAST_H
#define FEATUREMATCHING_FAST_H

#include <vector>


struct fast_xy {
    short x, y;

    fast_xy(short x_, short y_) : x(x_), y(y_) {}
};

typedef uint8_t fast_byte;

void fast_corner_detect_10(const fast_byte *img, int imgWidth, int imgHeight, int widthStep,
                           short barrier, std::vector<fast_xy> &corners);

#endif //FEATUREMATCHING_FAST_H
