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

struct matrix3x3 rotationMatrix(float3 angles) {
    float2 x = (float2){cos(angles.x), sin(angles.x)};
    float2 y = (float2){cos(angles.y), sin(angles.y)};
    float2 z = (float2){cos(angles.z), sin(angles.z)};

    return (struct matrix3x3){{
            (float3){y.x * z.x, - y.x * z.y, y.y},
            (float3){z.x * x.y * y.y + x.x * z.y,
                      x.x * z.x - x.y * y.y * z.y,
                      -y.x * x.y},
            (float3){-x.x * z.x * y.y + x.y * z.y,
                      z.x * x.y + x.x * y.y * z.y,
                      x.x * y.x}
        }};
}

struct matrix3x3 inverse(struct matrix3x3 m) {
    float invDet = 1 / determinant(m);
    return (struct matrix3x3) {
        {
            invDet * (float3){
                -m.r[1].z * m.r[2].y + m.r[1].y * m.r[2].z,
                m.r[0].z * m.r[2].y - m.r[0].y * m.r[2].z,
                -m.r[0].z * m.r[1].y + m.r[0].y * m.r[1].z},
            invDet * (float3) {
                m.r[1].z * m.r[2].x - m.r[1].x * m.r[2].z,
                -m.r[0].z * m.r[2].x + m.r[0].x * m.r[2].z,
                m.r[0].z * m.r[1].x - m.r[0].x * m.r[1].z},
            invDet * (float3) {
                -m.r[1].y * m.r[2].x + m.r[1].x * m.r[2].y,
                m.r[0].y * m.r[2].x - m.r[0].x * m.r[2].y,
                -m.r[0].y * m.r[1].x + m.r[0].x * m.r[1].y}
        }
    };
}

float determinant(struct matrix3x3 m) {
    return m.r[0].x * m.r[1].y * m.r[2].z +
           m.r[0].y * m.r[1].z * m.r[2].x +
           m.r[0].z * m.r[1].x * m.r[2].y -
           (
           m.r[0].z * m.r[1].y * m.r[2].x +
           m.r[0].x * m.r[1].z * m.r[2].y +
           m.r[0].y * m.r[1].x * m.r[2].z
           );
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