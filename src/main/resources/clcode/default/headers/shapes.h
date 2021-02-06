#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

#include<clcode/default/headers/math.h>

struct material_t {
    long type;
    float3 color;
    numf lumen;
    numf refractionIndex;
};

struct shape_t {
    long type;
    long id;
    long flags;
    struct material_t material;
    numf maxRadius;
    numf3 position;
    numf3 rotation;
    struct matrix3x3 rotationMatrix;
    struct matrix3x3 inverseRotationMatrix;
    __global void* shape;
};

struct sphere_t {
    numf radius;
};

struct torus_t {
    numf radiusSmall;
    numf radiusBig;
};

struct plane_t {
    numf3 normal;
};

struct subtraction_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

struct box_t {
    numf3 dimensions;
};

struct union_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

struct intersection_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

/** Make sure that direction is always normalized!*/
struct ray_t {
    numf3 origin;
    numf3 direction;
};

//The parameter for the oneStepSDF function
struct oneStepSDFArgs_t {
    numf3 point;
    __global struct shape_t* shape;
    numf d1;
    numf d2;
    int status;
};

/** Make sure that normal is always normalized*/
struct rayIntersection_t {
    __global struct shape_t* obj;
    numf3 point;
    numf3 normal;
    numf d;
};

numf sphereSDF(numf3 point, __global struct sphere_t* sphere);

numf torusSDF(numf3 point, __global struct torus_t* torus);

numf planeSDF(numf3 point, __global struct plane_t* plane);

numf boxSDF(numf3 point, __global struct box_t* box);

numf distToRay(numf3 point, struct ray_t* ray);

numf distToOrig(struct ray_t* ray);

#define sdfNormal(POINT,SDFFUN,OBJ)\
    (normalize((numf3){\
            SDFFUN(POINT + (numf3){EPSILON,0,0}, OBJ) - SDFFUN(POINT - (numf3){EPSILON,0,0}, OBJ),\
            SDFFUN(POINT + (numf3){0,EPSILON,0}, OBJ) - SDFFUN(POINT - (numf3){0,EPSILON,0}, OBJ),\
            SDFFUN(POINT + (numf3){0,0,EPSILON}, OBJ) - SDFFUN(POINT - (numf3){0,0,EPSILON}, OBJ),\
        }))

#endif