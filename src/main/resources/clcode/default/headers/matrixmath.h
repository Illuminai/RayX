#ifndef __HEADER_MATRIXMATH_H
#define __HEADER_MATRIXMATH_H

//r for rows:
/**
    r[0].x r[0].y r[0].y
    r[1].x r[1].y r[1].y
    r[2].x r[2].y r[2].y
*/
struct matrix3x3 {
    double3 r[3];
};

struct matrix3x3 matrixProduct(struct matrix3x3 a, struct matrix3x3 b);

double3 matrixTimesVector(struct matrix3x3 m, double3 vector);

struct matrix3x3 rotationMatrix(double alpha, double beta, double gamma);

struct matrix3x3 reverseRotationMatrix(double gamma, double beta, double alpha);

struct matrix3x3 rotationMatrixX(double alpha);

struct matrix3x3 rotationMatrixY(double beta);

struct matrix3x3 rotationMatrixZ(double gamma);

#endif