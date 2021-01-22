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
struct intersection_t {
    __global struct shape_t* obj;
    numf3 point;
    numf3 normal;
    numf d;
};

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct shape_t* sphere, struct intersection_t* inter);

bool firstIntersectionWithPlane(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * intersection);

numf torusSDF(numf3 point, __global struct torusSDF_t* torus);

numf boxSDF(numf3 point, __global struct boxSDF_t* box);

numf distToRay(numf3 point, struct ray_t* ray);

numf distToOrig(struct ray_t* ray);

numf3 reflectionRayDirection(numf3 direction, numf3 normal);

#define sdfNormal(POINT,SDFFUN,OBJ)\
    (normalize((numf3){\
            SDFFUN(POINT + (numf3){EPSILON,0,0}, OBJ) - SDFFUN(POINT - (numf3){EPSILON,0,0}, OBJ),\
            SDFFUN(POINT + (numf3){0,EPSILON,0}, OBJ) - SDFFUN(POINT - (numf3){0,EPSILON,0}, OBJ),\
            SDFFUN(POINT + (numf3){0,0,EPSILON}, OBJ) - SDFFUN(POINT - (numf3){0,0,EPSILON}, OBJ),\
        }))

#endif