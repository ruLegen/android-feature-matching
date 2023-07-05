#include "ImageProcessor.h"
#include "Utils.h"

const int DIFF_ARR_SIZE = 16 + 8;
struct CircleIndeces
{
    F9_CORNER locations[16];
};

struct ConsecutiveCount{
    int negative;
    int positive;
};

static inline ConsecutiveCount CountConsecutivePositiveAndNegatives(int upper[DIFF_ARR_SIZE],int lower[DIFF_ARR_SIZE]);

int upperIntensityDiffArray[DIFF_ARR_SIZE];
int lowerIntensityDiffArray[DIFF_ARR_SIZE];

static inline bool
fastCheckIsCorner(uint8_t *data, CircleIndeces &indeces, uint8_t currentIntensity,
                  int upperIntensityThreshold, int lowerIntensityThreshold);

static inline void getCircleIndeces(int cx, int cy, CircleIndeces &indeces)
{
    auto &locations = indeces.locations;

    locations[0] = F9_CORNER{.x=cx + 0, .y =cy - 3};
    locations[1] = F9_CORNER{.x=cx + 1, .y =cy - 3};
    locations[2] = F9_CORNER{.x=cx + 2, .y =cy - 2};
    locations[3] = F9_CORNER{.x=cx + 3, .y =cy - 1};
    locations[4] = F9_CORNER{.x=cx + 3, .y =cy + 0};

    locations[5] = F9_CORNER{.x=cx + 3, .y =cy + 1};
    locations[6] = F9_CORNER{.x=cx + 2, .y =cy + 2};
    locations[7] = F9_CORNER{.x=cx + 1, .y =cy + 3};
    locations[8] = F9_CORNER{.x=cx + 0, .y =cy + 3};

    locations[9] = F9_CORNER{.x=cx - 1, .y =cy + 3};
    locations[10] = F9_CORNER{.x=cx - 2, .y =cy + 2};
    locations[11] = F9_CORNER{.x=cx - 3, .y =cy + 1};
    locations[12] = F9_CORNER{.x=cx - 3, .y =cy + 0};

    locations[13] = F9_CORNER{.x=cx - 3, .y =cy - 1};
    locations[14] = F9_CORNER{.x=cx - 2, .y =cy - 2};
    locations[15] = F9_CORNER{.x=cx + -1, .y =cy - 3};
}

CircleIndeces circleIndeces;


ImageProcessor::ImageProcessor() : f9()
{
}

int ImageProcessor::detectCorners(uint8_t *input, uint8_t *output, int width, int height,
                                  int bytesPerPixel, uint8_t threshold)
{
    std::unique_ptr<uint8_t[]> data = std::unique_ptr<uint8_t[]>(new uint8_t[width * height]);
    auto rawPtr = data.get();
    // Convert 4D grayscale image to 1D grayscale
    for (int i = 0; i < height; ++i)
    {
        for (int j = 0; j < width; ++j)
        {
            int outActual1DIndex = ((i * width) + j);
            int input1DIndex = outActual1DIndex * bytesPerPixel;

            auto r = input[input1DIndex + 0];
            rawPtr[outActual1DIndex] = r;
        }
    }
    auto corners = detectCornersOwnImplenentation(data.get(), width, height, width, threshold, false);
    //f9.detectCorners(data.get(), width, height, width, threshold, false);
    for (auto location: corners)
    {
        int actual1DIndex = ((location.y * width) + location.x) * bytesPerPixel;
        output[actual1DIndex + 0] = 255;     //r
        output[actual1DIndex + 3] = 255;     //a
    }

    return corners.size();
}

const std::vector<F9_CORNER>& ImageProcessor::detectCornersOwnImplenentation(uint8_t *image_data, int width, int height,
                                                    int bytes_per_row, uint8_t threshold,
                                                    bool suppress_non_max)
{
    cornerPoints.clear();

    for (int y = 3; y < height - 3; ++y)
    {
        auto row = &image_data[y * bytes_per_row];
        for (int x = 3; x < width - 3; ++x)
        {
            uint8_t currentIntensity = *(row + x);
            int upperIntensityThreshold = currentIntensity + threshold;
            int lowerIntensityThreshold = currentIntensity - threshold;

            getCircleIndeces(x, y, circleIndeces);

            //   if(!fastCheckIsCorner(image_data, circleIndeces, currentIntensity, upperIntensityThreshold, lowerIntensityThreshold)){
            //       continue;
            //    }
            for (int i = 0; i < DIFF_ARR_SIZE; ++i)
            {
                auto &location = circleIndeces.locations[i%16];
                int oneDIndex = location.y * bytes_per_row + location.x;
                uint8_t circlePixelIntesity = image_data[oneDIndex];
                upperIntensityDiffArray[i] = circlePixelIntesity - upperIntensityThreshold;     // if < 0 darker; otherwise lighter;
                lowerIntensityDiffArray[i] = circlePixelIntesity - lowerIntensityThreshold;
            }
            auto res = CountConsecutivePositiveAndNegatives(upperIntensityDiffArray,lowerIntensityDiffArray);
            auto isCorner = res.negative > 12 || res.positive > 12;
            if(isCorner){
                cornerPoints.push_back(F9_CORNER{
                    .x  = x,
                    .y = y
                });
            }
        }
        return cornerPoints;
    }
}

static inline bool
fastCheckIsCorner(uint8_t *data, CircleIndeces &indeces, uint8_t currentIntensity,
                  int upperIntensityThreshold, int lowerIntensityThreshold)
{
    auto &location_1 = indeces.locations[0];

}
static inline ConsecutiveCount CountConsecutivePositiveAndNegatives(int upper[DIFF_ARR_SIZE],int lower[DIFF_ARR_SIZE])
{
    int maxConsecutivePositive = 0;
    int maxConsecutiveNegative = 0;
    int currentConsecutivePositive = 0;
    int currentConsecutiveNegative = 0;

    for (int i = 0; i < DIFF_ARR_SIZE; i++) {
        int isPositive = (upper[i] >= 0);
        int isNegative = (lower[i] < 0);


        currentConsecutivePositive = (currentConsecutivePositive + 1) * isPositive;
        currentConsecutiveNegative = (currentConsecutiveNegative + 1) * isNegative;

        maxConsecutivePositive = (maxConsecutivePositive < currentConsecutivePositive) * currentConsecutivePositive
                                 + (maxConsecutivePositive >= currentConsecutivePositive) * maxConsecutivePositive;
        maxConsecutiveNegative = (maxConsecutiveNegative < currentConsecutiveNegative) * currentConsecutiveNegative
                                 + (maxConsecutiveNegative >= currentConsecutiveNegative) * maxConsecutiveNegative;
    }
    return ConsecutiveCount{
        .positive = maxConsecutivePositive,
        .negative = maxConsecutiveNegative
    };
}



