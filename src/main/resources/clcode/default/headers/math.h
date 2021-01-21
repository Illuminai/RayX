#ifndef __HEADER_MATRIXMATH_H
#define __HEADER_MATRIXMATH_H

typedef float numf;
typedef float2 numf2;
typedef float3 numf3;
typedef float4 numf4;

//r for rows:
/**
    r[0].x r[0].y r[0].y
    r[1].x r[1].y r[1].y
    r[2].x r[2].y r[2].y
*/
struct matrix3x3 {
    numf3 r[3];
};

struct matrix3x3 matrixProduct(struct matrix3x3 a, struct matrix3x3 b);

numf3 matrixTimesVector(struct matrix3x3 m, numf3 vector);

struct matrix3x3 rotationMatrix(numf alpha, numf beta, numf gamma);

struct matrix3x3 reverseRotationMatrix(numf gamma, numf beta, numf alpha);

struct matrix3x3 rotationMatrixX(numf alpha);

struct matrix3x3 rotationMatrixY(numf beta);

struct matrix3x3 rotationMatrixZ(numf gamma);

#endif