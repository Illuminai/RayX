#ifndef __HEADER_RENDER_H
#define __HEADER_RENDER_H
#include<clcode/default/headers/shapes.h>
#include<clcode/default/headers/math.h>

#define MAX_RAY_BOUNCES 5

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

__kernel void render(  __write_only image2d_t resultImage,
                       float4 cameraPosition,
                       float4 cameraRotation,
                       float cameraFOV,
                       int globalNumShapes,
                       __global struct shape_t * globalShapes);

float4 getTypeColor(int type);

struct ray_t getRay(numf u, numf v, numf3 camPos, numf3 camRot, numf camFOV);

void traceRay(struct ray_t* ray, int numShapes,
            __global struct shape_t* allShapes, struct intersection_t* inter);

// Returns true if there is an intersection
bool firstIntersectionWithShape(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t* inter, numf maxD);

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct shape_t* sphere, struct intersection_t* inter);

bool firstIntersectionWithPlane(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * intersection);

bool firstIntersectionWithSDF(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * intersection, numf maxD);

numf torusSDF(numf3 point, __global struct torusSDF_t* torus);

numf boxSDF(numf3 point, __global struct boxSDF_t* box);

numf subtractionSDF(numf3 point, __global struct subtractionSDF_t* subtraction);

numf distToRay(numf3 point, struct ray_t* ray);

numf distToOrig(struct ray_t* ray);

numf oneStepSDF(numf3 point, __global struct shape_t* shape);

numf3 reflectionRayDirection(numf3 direction, numf3 normal);

#define sdfNormal(POINT,SDFFUN,OBJ)\
    (normalize((numf3){\
            SDFFUN(POINT + (numf3){EPSILON,0,0}, OBJ) - SDFFUN(POINT - (numf3){EPSILON,0,0}, OBJ),\
            SDFFUN(POINT + (numf3){0,EPSILON,0}, OBJ) - SDFFUN(POINT - (numf3){0,EPSILON,0}, OBJ),\
            SDFFUN(POINT + (numf3){0,0,EPSILON}, OBJ) - SDFFUN(POINT - (numf3){0,0,EPSILON}, OBJ),\
        }))

#endif