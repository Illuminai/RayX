#ifndef __HEADER_JAVA_TO_CL_H
#define __HEADER_JAVA_TO_CL_H

#include<clcode/default/headers/shapes.h>

__kernel void getShapeSizes(int numShapes,
                __global int* shapesInQuestion,
                __global int* result);

__kernel void putShapesInMemory(int numShapes,
                __global char* inputData,
                __global struct shape_t* rawShapes,
                __global struct sphere_t* dataSphere,
                __global struct torus_t* dataTorus,
                __global struct plane_t* dataPlane,
                __global struct subtraction_t* dataSubtractionSDF,
                __global struct box_t* dataBoxSDF,
                __global struct union_t* dataUnionSDF,
                __global struct intersection_t* dataIntersectionSDF,
                __global struct octahedron_t* dataOctahedronSDF);

numf getNextFloat(__global char* data);

numf3 getNextFloat3(__global char* data);

long getNextLong(__global char* data);

#endif