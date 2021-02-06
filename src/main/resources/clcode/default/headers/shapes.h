#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

#include<clcode/default/headers/math.h>

struct material_t {
    long type;
    float3 color;
    float lumen;
    float refractionIndex;
};

struct shape_t {
    long type;
    long id;
    long flags;
    struct material_t material;
    float maxRadius;
    float3 position;
    float3 rotation;
    struct matrix3x3 rotationMatrix;
    struct matrix3x3 inverseRotationMatrix;
    __global void* shape;
};

struct sphere_t {
    float radius;
};

struct torus_t {
    float radiusSmall;
    float radiusBig;
};

struct plane_t {
    float3 normal;
};

struct subtraction_t {
    __global struct shape_t* shape1;
    __global struct shape_t* shape2;
};

struct box_t {
    float3 dimensions;
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
    float3 origin;
    float3 direction;
};

//The parameter for the oneStepSDF function
struct oneStepSDFArgs_t {
    float3 point;
    __global struct shape_t* shape;
    float d1;
    float d2;
    int status;
};

/** Make sure that normal is always normalized*/
struct rayIntersection_t {
    __global struct shape_t* obj;
    float3 point;
    float3 normal;
    float d;
};

float sphereSDF(float3 point, __global struct sphere_t* sphere);

float torusSDF(float3 point, __global struct torus_t* torus);

float planeSDF(float3 point, __global struct plane_t* plane);

float boxSDF(float3 point, __global struct box_t* box);

float distToRay(float3 point, struct ray_t* ray);

float distToOrig(struct ray_t* ray);

#define sdfNormal(POINT,SDFFUN,OBJ)\
    (normalize((float3){\
            SDFFUN(POINT + (float3){EPSILON,0,0}, OBJ) - SDFFUN(POINT - (float3){EPSILON,0,0}, OBJ),\
            SDFFUN(POINT + (float3){0,EPSILON,0}, OBJ) - SDFFUN(POINT - (float3){0,EPSILON,0}, OBJ),\
            SDFFUN(POINT + (float3){0,0,EPSILON}, OBJ) - SDFFUN(POINT - (float3){0,0,EPSILON}, OBJ),\
        }))

#endif