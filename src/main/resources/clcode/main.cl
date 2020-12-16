#include<clcode/headers/utility/mandelbrot.h>
/*
float4 color(int, int);
int calc(double2, int);
*/
__kernel void testKernel(__write_only image2d_t image, __global double* center) {
    int2 pixCo = (int2){get_global_id(0),get_global_id(1)};
    int w = get_image_width(image);
    int h = get_image_height(image);
    if(pixCo.x >= w | pixCo.y >= h) {
        return;
    }
    double2 absCo = (double2)
        {center[2] * ((1.0 * pixCo.x / w) - .5) + center[0],
         center[2] * ((1.0 * (h - pixCo.y - 1) / h) - .5) + center[1]};

    write_imagef(image, pixCo, mandelbrotColor(mandelbrotCalc(absCo, 100), 100));
}