#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

struct shape_t {
    int type;
    __global void* shape;
};

// value.w is the radius
struct sphere_t {
    double3 position;
    double radius;
};

struct torus_t {
    double3 position;
    double3 rotation;
    double radiusSmall;
    double radiusBig;
};

#endif