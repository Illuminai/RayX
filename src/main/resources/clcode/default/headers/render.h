#ifndef __HEADER_RENDER_H
#define __HEADER_RENDER_H
#include<clcode/default/headers/shapes.h>
#include<clcode/default/headers/matrixmath.h>

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
struct intersection_t {
    __global struct shape_t* obj;
    float3 point;
    float3 normal;
    float d;
};

__kernel void render(  __write_only image2d_t resultImage,
                       float4 cameraPosition,
                       float4 cameraRotation,
                       float cameraFOV,
                       int globalNumShapes,
                       __global struct shape_t * globalShapes);

float4 getTypeColor(int type);

struct ray_t getRay(float u, float v, float3 camPos, float3 camRot, float camFOV);

void traceRay(struct ray_t* ray, int numShapes,
            __global struct shape_t* allShapes, struct intersection_t* inter);

// Returns true if there is an intersection
bool firstIntersectionWithShape(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t* inter);

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct shape_t* sphere, struct intersection_t* inter);

bool firstIntersectionWithPlane(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * intersection);

bool firstIntersectionWithSDF(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * intersection);

float torusSDF(float3 point, __global struct torusSDF_t* torus);

float boxSDF(float3 point, __global struct boxSDF_t* box);

float subtractionSDF(float3 point, __global struct subtractionSDF_t* subtraction);

float distToRay(float3 point, struct ray_t* ray);

float distToOrig(struct ray_t* ray);

float oneStepSDF(float3 point, __global struct shape_t* shape);

float3 reflectionRayDirection(float3 direction, float3 normal);

#define sdfNormal(POINT,SDFFUN,OBJ)\
    (normalize((float3){\
            SDFFUN(POINT + (float3){EPSILON,0,0}, OBJ) - SDFFUN(POINT - (float3){EPSILON,0,0}, OBJ),\
            SDFFUN(POINT + (float3){0,EPSILON,0}, OBJ) - SDFFUN(POINT - (float3){0,EPSILON,0}, OBJ),\
            SDFFUN(POINT + (float3){0,0,EPSILON}, OBJ) - SDFFUN(POINT - (float3){0,0,EPSILON}, OBJ),\
        }))

#endif