#include<clcode/default/headers/math.h>

struct matrix3x3 matrixProduct(struct matrix3x3 a, struct matrix3x3 b) {
    return (struct matrix3x3){
        {(float3){ a.r[0].x * b.r[0].x + a.r[0].y * b.r[1].x + a.r[0].z * b.r[2].x,
                    a.r[0].x * b.r[0].y + a.r[0].y * b.r[1].y + a.r[0].z * b.r[2].y,
                    a.r[0].x * b.r[0].z + a.r[0].y * b.r[1].z + a.r[0].z * b.r[2].z},
        (float3){  a.r[1].x * b.r[0].x + a.r[1].y * b.r[1].x + a.r[1].z * b.r[2].x,
                    a.r[1].x * b.r[0].y + a.r[1].y * b.r[1].y + a.r[1].z * b.r[2].y,
                    a.r[1].x * b.r[0].z + a.r[1].y * b.r[1].z + a.r[1].z * b.r[2].z},
        (float3){  a.r[2].x * b.r[0].x + a.r[2].y * b.r[1].x + a.r[2].z * b.r[2].x,
                    a.r[2].x * b.r[0].y + a.r[2].y * b.r[1].y + a.r[2].z * b.r[2].y,
                    a.r[2].x * b.r[0].z + a.r[2].y * b.r[1].z + a.r[2].z * b.r[2].z}}
    };
}

float3 matrixTimesVector(struct matrix3x3 m, float3 v) {
    return (float3){
        m.r[0].x * v.x + m.r[0].y * v.y + m.r[0].z * v.z,
        m.r[1].x * v.x + m.r[1].y * v.y + m.r[1].z * v.z,
        m.r[2].x * v.x + m.r[2].y * v.y + m.r[2].z * v.z
    };
}

struct matrix3x3 rotationMatrix(float alpha, float beta, float gamma) {
    return (struct matrix3x3){{
            (float3){cos(beta) * cos(gamma), - cos(beta) * sin(gamma), sin(beta)},
            (float3){cos(gamma) * sin(alpha) * sin(beta) + cos(alpha) * sin(gamma),
                      cos(alpha) * cos(gamma) - sin(alpha) * sin(beta) * sin(gamma),
                      -cos(beta) * sin(alpha)},
            (float3){-cos(alpha) * cos(gamma) * sin(beta) + sin(alpha) * sin(gamma),
                      cos(gamma) * sin(alpha) + cos(alpha) * sin(beta) * sin(gamma),
                      cos(alpha) * cos(beta)}
        }};
}

struct matrix3x3 reverseRotationMatrix(float gamma, float beta, float alpha) {
    return (struct matrix3x3){{
                (float3){cos(beta) * cos(gamma), cos(gamma) * sin(alpha) * sin(beta) + cos(alpha) * sin(gamma), -cos(alpha) * cos(gamma) * sin(beta) + sin(alpha) * sin(gamma)},
                (float3){-cos(beta) * sin(gamma), cos(alpha) * cos(gamma) - sin(alpha) * sin(beta) * sin(gamma), cos(gamma) * sin(alpha) + cos(alpha) * sin(beta) * sin(gamma)},
                (float3){sin(beta), -cos(beta) * sin(alpha), cos(alpha) * cos(beta)}
            }};
}

struct matrix3x3 rotationMatrixX(float alpha) {
    return (struct matrix3x3){{
                (float3){1,0,0},
                (float3){0, cos(alpha), -sin(alpha)},
                (float3){0, sin(alpha), cos(alpha)}
            }};
}

struct matrix3x3 rotationMatrixY(float beta) {
    return (struct matrix3x3){{
                (float3){cos(beta), 0, sin(beta)},
                (float3){0,1,0},
                (float3){-sin(beta), 0, cos(beta)}
            }};
}

struct matrix3x3 rotationMatrixZ(float gamma) {
    return (struct matrix3x3){{
                    (float3){cos(gamma), -sin(gamma), 0},
                    (float3){sin(gamma), cos(gamma), 0},
                    (float3){0,0,1}
                }};
}