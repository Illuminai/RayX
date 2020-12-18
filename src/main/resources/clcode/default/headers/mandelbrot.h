#ifndef __HEADER_MANDELBROT_H
#define __HEADER_MANDELBROT_H

int mandelbrotCalc(double2 coo, int maxIter);
float4 mandelbrotColor(int value, int maxIterations);

#endif