#ifndef __HEADER_RENDER_H
#define __HEADER_RENDER_H
#include<clcode/default/headers/shapes.h>
#include<clcode/default/headers/math.h>

#define MAX_RAY_BOUNCES 5

__kernel void renderDebug(__write_only image2d_t resultImage, float width,
                          float height, float4 cameraPosition,
                          float4 cameraRotation, float cameraFOV,
                          int globalNumShapes,
                          __global struct shape_t* globalShapes,
                          int samples);

struct ray_t getRay(float u, float v, float3 camPos, float3 camRot, float camFOV);

float3 getNormalColor(struct rayIntersection_t inter);

float getSampledHalton(int index, int base);

__kernel void render(__write_only image2d_t resultImage, float width,
                     float height, float4 cameraPosition, float4 cameraRotation,
                     float cameraFOV, int globalNumShapes,
                     __global struct shape_t* globalShapes,
                     int samples);

float4 getTypeColor(int type);

float3 perfectReflectionRayDirection(float3 direction, float3 normal);

struct ray_t getRay(float u, float v, float3 camPos, float3 camRot, float camFOV);

void traceRay(struct ray_t* ray, int numShapes,
            __global struct shape_t* allShapes, struct rayIntersection_t* inter);

// Returns true if there is an intersection
bool firstIntersectionWithShape(struct ray_t* ray, __global struct shape_t* shape, struct rayIntersection_t* inter, float maxD);

bool firstIntersectionWithSDF(struct ray_t* ray, __global struct shape_t* shape, struct rayIntersection_t * intersection, float maxD);

struct ray_t nextRayOnIntersection(struct ray_t* ray, struct rayIntersection_t* inter);

float oneStepSDF(float3 point, __global struct shape_t* shape);

#endif