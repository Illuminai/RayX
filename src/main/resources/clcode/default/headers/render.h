#ifndef __HEADER_RENDER_H
#define __HEADER_RENDER_H
#include<clcode/default/headers/shapes.h>

//r for rows:
/**
    r[0].x r[0].y r[0].y
    r[1].x r[1].y r[1].y
    r[2].x r[2].y r[2].y
*/
struct matrix3x3 {
    double3 r[3];
};

/** Make sure that direction is always normalized!*/
struct ray_t {
    double3 origin;
    double3 direction;
};


/** Make sure that normal is always normalized*/
struct intersection_t {
    struct ray_t* ray;
    double3 point;
    double3 normal;
    double d;
};

__kernel void render(  __write_only image2d_t resultImage,
                            double4 cameraPosition,
                            double4 cameraRotation,
                            double cameraFOV,
                            int numShapes,
                            __global struct shape_t * shapes,
                            __global struct sphere_t * sphereData,
                            __global struct torus_t * torusData);

struct matrix3x3 matrixProduct(struct matrix3x3 a, struct matrix3x3 b);

double3 matrixTimesVector(struct matrix3x3 m, double3 vector);

struct matrix3x3 rotationMatrix(double alpha, double beta, double gamma);

struct matrix3x3 rotationMatrixX(double alpha);

struct matrix3x3 rotationMatrixY(double beta);

struct matrix3x3 rotationMatrixZ(double gamma);

/** Returns true if there is an intersection*/
bool firstIntersectionWithShape(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t* inter);

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct sphere_t* sphere, struct intersection_t* inter);

bool firstIntersectionWithTorus(struct ray_t* ray, __global struct torus_t* torus, struct intersection_t* inter);

#endif