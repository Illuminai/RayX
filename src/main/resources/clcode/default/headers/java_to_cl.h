#ifndef __HEADER_JAVA_TO_CL_H
#define __HEADER_JAVA_TO_CL_H

#include<clcode/default/headers/shapes.h>

__kernel void getShapeSizes(int numShapes,
                __global int* shapesInQuestion,
                __global int* result);

float getNextFloat(__global char* data);

float3 getNextFloat3(__global char* data);

long getNextLong(__global char* data);

//#endif append when creating file!