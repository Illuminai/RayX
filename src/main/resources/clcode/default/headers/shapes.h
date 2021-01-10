#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

#include<clcode/default/headers/matrixmath.h>

struct shape_t {
    long type;
    long id;
    long shouldRender;
    double maxRadius;
    double3 position;
    double3 rotation;
    struct matrix3x3 rotationMatrix;
    struct matrix3x3 inverseRotationMatrix;
    __global void* shape;
};

struct sphereRTC_t {
    double radius;
};

struct torusSDF_t {
    double radiusSmall;
    double radiusBig;
};

struct planeRTC_t {
    double3 normal;
};

struct subtractionSDF_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

struct boxSDF_t {
    double3 dimensions;
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