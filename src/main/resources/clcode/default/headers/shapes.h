#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

struct shape_t {
    int type;
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

#endif