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
                __global struct torusSDF_t* dataTorus,
                __global struct planeRTC_t* dataPlane,
                __global struct subtractionSDF_t* dataSubtractionSDF,
                __global struct boxSDF_t* dataBoxSDF,
                __global struct unionSDF_t* dataUnionSDF,
                __global struct intersectionSDF_t* dataIntersectionSDF);

float getNextDouble(__global char* data);

float3 getNextDouble3(__global char* data);

long getNextLong(__global char* data);
#endif