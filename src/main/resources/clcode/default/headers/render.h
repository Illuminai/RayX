#ifndef __HEADER_RENDER_H
#define __HEADER_RENDER_H
#include<clcode/default/headers/shapes.h>
#include<clcode/default/headers/matrixmath.h>

/** Make sure that direction is always normalized!*/
struct ray_t {
    double3 origin;
    double3 direction;
};


/** Make sure that normal is always normalized*/
struct intersection_t {
    __global struct shape_t* obj;
    struct ray_t* ray;
    double3 point;
    double3 normal;
    double d;
};

__kernel void render(  __write_only image2d_t resultImage,
                       double4 cameraPosition,
                       double4 cameraRotation,
                       double cameraFOV,
                       int globalNumShapes,
                       __global struct shape_t * globalShapes,
                       __global struct sphereRTC_t * sphereData,
                       __global struct torusSDF_t * torusData,
                       __global struct planeRTC_t * planeData);

struct ray_t getRay(double u, double v, double3 camPos, double3 camRot, double camFOV);

void traceRay(struct ray_t* ray, int numShapes,
            __global struct shape_t* allShapes, struct intersection_t* inter);

// Returns true if there is an intersection
bool firstIntersectionWithShape(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t* inter);

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct shape_t* sphere, struct intersection_t* inter);

bool firstIntersectionWithPlane(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * intersection);

bool firstIntersectionWithSDF(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * intersection);

double torusSDF(double3 point, __global struct torusSDF_t* torus);

double subtractionSDF(double3 point, __global struct subtractionSDF_t* subtraction);

double distToRay(double3 point, struct ray_t* ray);

double distToOrig(struct ray_t* ray);

double oneStepSDF(double3 point, __global struct shape_t* shape);

#define EPSILON 0.0001


#define sdfNormal(POINT,SDFFUN,OBJ)\
    (normalize((double3){\
            SDFFUN(POINT + (double3){EPSILON,0,0}, OBJ) - SDFFUN(POINT - (double3){EPSILON,0,0}, OBJ),\
            SDFFUN(POINT + (double3){0,EPSILON,0}, OBJ) - SDFFUN(POINT - (double3){0,EPSILON,0}, OBJ),\
            SDFFUN(POINT + (double3){0,0,EPSILON}, OBJ) - SDFFUN(POINT - (double3){0,0,EPSILON}, OBJ),\
        }))

#endif