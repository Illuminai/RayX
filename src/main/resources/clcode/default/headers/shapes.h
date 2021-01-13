#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

#include<clcode/default/headers/matrixmath.h>

struct shape_t {
    long type;
    long id;
    long flags;
    float maxRadius;
    float lumen;
    float3 position;
    float3 rotation;
    struct matrix3x3 rotationMatrix;
    struct matrix3x3 inverseRotationMatrix;
    __global void* shape;
};

struct sphereRTC_t {
    float radius;
};

struct torusSDF_t {
    float radiusSmall;
    float radiusBig;
};

struct planeRTC_t {
    float3 normal;
};

struct subtractionSDF_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

struct boxSDF_t {
    float3 dimensions;
};

struct unionSDF_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

struct intersectionSDF_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

#endif