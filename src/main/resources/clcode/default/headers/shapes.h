#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

#include<clcode/default/headers/math.h>

struct shape_t {
    long type;
    long id;
    long flags;
    numf maxRadius;
    numf lumen;
    numf3 position;
    numf3 rotation;
    struct matrix3x3 rotationMatrix;
    struct matrix3x3 inverseRotationMatrix;
    __global void* shape;
};

struct sphereRTC_t {
    numf radius;
};

struct torusSDF_t {
    numf radiusSmall;
    numf radiusBig;
};

struct planeRTC_t {
    numf3 normal;
};

struct subtractionSDF_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

struct boxSDF_t {
    numf3 dimensions;
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