#ifndef __HEADER_RENDER_H
#define __HEADER_RENDER_H
#include<clcode/default/headers/shapes.h>
#include<clcode/default/headers/math.h>

#define MAX_RAY_BOUNCES 5

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

bool firstIntersectionWithSDF(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * intersection, numf maxD);

numf oneStepSDF(numf3 point, __global struct shape_t* shape);

#endif