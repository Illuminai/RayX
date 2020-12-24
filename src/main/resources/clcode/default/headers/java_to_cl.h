#ifndef __HEADER_JAVA_TO_CL_H
#define __HEADER_JAVA_TO_CL_H
#include<clcode/default/headers/shapes.h>

__kernel void getShapeSizes(int numShapes,
                __global int* shapesInQuestion,
                __global int* result);

__kernel void putShapesInMemory(int numShapes,
                __global char* inputData,
                __global struct shape_t* rawShapes,
                __global struct sphereRTC_t* dataSphere,
                __global struct torusSDF_t* dataTorus);

double getNextDouble(__global char* data);

#endif