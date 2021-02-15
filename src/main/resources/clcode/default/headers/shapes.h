#ifndef __HEADER_SHAPES_H
#define __HEADER_SHAPES_H

#include<clcode/default/headers/math.h>

struct material_t {
    long type;
    float3 color;
    float lumen;
    union {
        struct {
        } reflection;
        struct {
            float refractionIndex;
        } refraction;
    } properties;
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

float distToOrig(struct ray_t* ray);

#define sdfNormal(POINT,SDFFUN,OBJ)\
    (normalize((float3){\
            SDFFUN(POINT + (float3){EPSILON,0,0}, OBJ) - SDFFUN(POINT - (float3){EPSILON,0,0}, OBJ),\
            SDFFUN(POINT + (float3){0,EPSILON,0}, OBJ) - SDFFUN(POINT - (float3){0,EPSILON,0}, OBJ),\
            SDFFUN(POINT + (float3){0,0,EPSILON}, OBJ) - SDFFUN(POINT - (float3){0,0,EPSILON}, OBJ),\
        }))

//#endif append when creating file!