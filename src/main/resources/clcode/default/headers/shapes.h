#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

struct shape_t {
    int type;
    __global void* shape;
};

// value.z is the radius
struct sphere_t {
    double4 value;
};

struct torus_t {
    double3 position;
    double3 rotation;
    double radiusSmall;
    double radiusBig;
};

#endif