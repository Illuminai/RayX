#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

struct shape_t {
    long type;
    long id;
    long shouldRender;
    double maxRadius;
    double3 position;
    __global void* shape;
};

struct sphereRTC_t {
    double radius;
};

struct torusSDF_t {
    double3 rotation;
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

#endif