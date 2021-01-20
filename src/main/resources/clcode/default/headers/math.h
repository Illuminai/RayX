#ifndef __HEADER_MATRIXMATH_H
#define __HEADER_MATRIXMATH_H

//r for rows:
/**
    r[0].x r[0].y r[0].y
    r[1].x r[1].y r[1].y
    r[2].x r[2].y r[2].y
*/
struct matrix3x3 {
    float3 r[3];
};

struct matrix3x3 matrixProduct(struct matrix3x3 a, struct matrix3x3 b);

float3 matrixTimesVector(struct matrix3x3 m, float3 vector);

struct matrix3x3 rotationMatrix(float alpha, float beta, float gamma);

struct matrix3x3 reverseRotationMatrix(float gamma, float beta, float alpha);

struct matrix3x3 rotationMatrixX(float alpha);

struct matrix3x3 rotationMatrixY(float beta);

struct matrix3x3 rotationMatrixZ(float gamma);

#endif