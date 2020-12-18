#include<clcode/default/headers/mandelbrot.h>

int mandelbrotCalc(double2 coo, int maxIter) {
    double a = 0, b = 0, ta, tb;
    int i = 0;
    for(;(i < maxIter) & !(a * a + b * b > 4); i++) {
        ta = a*a - b*b + coo.x;
        tb = 2 * a * b + coo.y;
        a = ta;
        b = tb;
    }
    return i == maxIter ? -1 : i;
}

float4 mandelbrotColor(int value, int maxIterations) {
    int color;
    if(value == -1) {
        color = 0x0;
    } else if(value == 0) {
        color = 0x0;
    } else {
        color = 0xffffff * (1.0*log((double)value)/log((double)maxIterations));
    }

    return (float4){
        1.0 * ((color >> 0) & 0xFF) / 0xFF,
        1.0 * ((color >> 8) & 0xFF) / 0xFF,
        1.0 * ((color >> 16) & 0xFF) / 0xFF,
        1
    };
}