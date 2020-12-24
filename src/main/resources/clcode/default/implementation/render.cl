#include<clcode/default/headers/render.h>

/** Although cameraPosition/cameraRotation are 3dim, the parameter passed is 4dim so it can
be passed using clSetKernelArg4d */
__kernel void render(  __write_only image2d_t resultImage,
                            double4 cameraPosition,
                            double4 cameraRotation,
                            double cameraFOV,
                            int numShapes,
                            __global struct shape_t * shapes,
                            __global struct sphere_t * sphereData,
                            __global struct torus_t * torusData) {
    int2 pixCo = (int2){get_global_id(0), get_global_id(1)};
    int w = get_image_width(resultImage);
    int h = get_image_height(resultImage);
    if(pixCo.x >= w | pixCo.y >= h) {
        return;
    }
    //TODO implement cameraFOV

    double u = 2.0 * (((pixCo.x + .5) / w) - .5);
    //sign is because image is saved with origin in the
        //upper left corner instead of lower left
    double v = -2.0 * (((pixCo.y + .5) / h) - .5);

    struct matrix3x3 rotMat = rotationMatrix(cameraRotation.x,
                                            cameraRotation.y, cameraRotation.z);
    double3 viewDirection = matrixTimesVector(rotMat, (double3){cameraFOV,0,0});
    double3 cameraRight = matrixTimesVector(rotMat, (double3){0,1,0});
    double3 cameraUp = matrixTimesVector(rotMat, (double3){0,0,1});

    struct ray_t rayToCheck = (struct ray_t) {cameraPosition.xyz,
        normalize(viewDirection * cameraFOV +
        cameraRight * u + cameraUp * v)};
    float4 color;
//    if(pixCo.x == 0 && pixCo.y == 0) {
//        sphereData[0].position = (double3){u, v, 0};
//    }

    struct intersection_t inter = (struct intersection_t){
        (struct ray_t*)0,
        (double3){0,0,0},(double3){0,0,0}, 0}, tmp;
    for(int i = 0; i < numShapes; i++) {
        if(firstIntersectionWithShape(&rayToCheck, &shapes[i], &tmp)) {
            if(inter.ray != 0) {
                if(tmp.d < inter.d) {
                    inter = tmp;
                }
            } else {
                inter = tmp;
            }
        }
    }
    if(inter.ray == 0) {
        color = (float4){0, 0, 0 ,1};
    } else {
        color = (float4){1, 1, 1, 1};
    }
    write_imagef(resultImage, pixCo, color);
}

bool firstIntersectionWithShape(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t* inter) {
    if(shape->type == SPHERE) {
        return firstIntersectionWithSphere(ray, shape->shape, inter);
    } else if(shape->type == TORUS) {
        return false;
    } else {
        return false;
    }
}

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct sphere_t* sphere, struct intersection_t* inter) {
    double3 omc = ray->origin - sphere->position;
    double delta = pow(dot(ray->direction,omc),2) -
                   (pow(length(omc),2) - sphere->radius * sphere->radius);

    if(delta < 0) {
        return false;
    }
    delta = sqrt(delta);

    double d1 = -dot(ray->direction, omc);
    double d2 = d1;

    d1 += delta;
    d2 -= delta;

    if(d2 > 0) {
        inter->ray = ray;
        inter->point = ray->origin + d2 * ray->direction;
        inter->normal = normalize(inter->point - sphere->position);
        inter->d = d2;
        return true;
    } else if (d1 > 0) {
        inter->ray = ray;
        inter->point = ray->origin + d1 * ray->direction;
        inter->normal = normalize(inter->point - sphere->position);
        inter->d = d1;
        return true;
    } else {
        return false;
    }

}

bool firstIntersectionWithTorus(struct ray_t* ray, __global struct torus_t* torus, struct intersection_t* inter) {
    //TODO
    return false;
}

struct matrix3x3 matrixProduct(struct matrix3x3 a, struct matrix3x3 b) {
    return (struct matrix3x3){
        {(double3){ a.r[0].x * b.r[0].x + a.r[0].y * b.r[1].x + a.r[0].z * b.r[2].x,
                    a.r[0].x * b.r[0].y + a.r[0].y * b.r[1].y + a.r[0].z * b.r[2].y,
                    a.r[0].x * b.r[0].z + a.r[0].y * b.r[1].z + a.r[0].z * b.r[2].z},
        (double3){  a.r[1].x * b.r[0].x + a.r[1].y * b.r[1].x + a.r[1].z * b.r[2].x,
                    a.r[1].x * b.r[0].y + a.r[1].y * b.r[1].y + a.r[1].z * b.r[2].y,
                    a.r[1].x * b.r[0].z + a.r[1].y * b.r[1].z + a.r[1].z * b.r[2].z},
        (double3){  a.r[2].x * b.r[0].x + a.r[2].y * b.r[1].x + a.r[2].z * b.r[2].x,
                    a.r[2].x * b.r[0].y + a.r[2].y * b.r[1].y + a.r[2].z * b.r[2].y,
                    a.r[2].x * b.r[0].z + a.r[2].y * b.r[1].z + a.r[2].z * b.r[2].z}}
    };
}

double3 matrixTimesVector(struct matrix3x3 m, double3 v) {
    return (double3){
        m.r[0].x * v.x + m.r[0].y * v.y + m.r[0].z * v.z,
        m.r[1].x * v.x + m.r[1].y * v.y + m.r[1].z * v.z,
        m.r[2].x * v.x + m.r[2].y * v.y + m.r[2].z * v.z
    };
}

struct matrix3x3 rotationMatrix(double alpha, double beta, double gamma) {
    return (struct matrix3x3){{
            (double3){cos(beta) * cos(gamma), - cos(beta) * sin(gamma), sin(beta)},
            (double3){cos(gamma) * sin(alpha) * sin(beta) + cos(alpha) * sin(gamma),
                      cos(alpha) * cos(gamma) - sin(alpha) * sin(beta) * sin(gamma),
                      -cos(beta) * sin(alpha)},
            (double3){-cos(alpha) * cos(gamma) * sin(beta) + sin(alpha) * sin(gamma),
                      cos(gamma) * sin(alpha) + cos(alpha) * sin(beta) * sin(gamma),
                      cos(alpha) * cos(beta)}
        }};
}

struct matrix3x3 rotationMatrixX(double alpha) {
    return (struct matrix3x3){{
                (double3){1,0,0},
                (double3){0, cos(alpha), -sin(alpha)},
                (double3){0, sin(alpha), cos(alpha)}
            }};
}

struct matrix3x3 rotationMatrixY(double beta) {
    return (struct matrix3x3){{
                (double3){cos(beta), 0, sin(beta)},
                (double3){0,1,0},
                (double3){-sin(beta), 0, cos(beta)}
            }};
}

struct matrix3x3 rotationMatrixZ(double gamma) {
    return (struct matrix3x3){{
                    (double3){cos(gamma), -sin(gamma), 0},
                    (double3){sin(gamma), cos(gamma), 0},
                    (double3){0,0,1}
                }};
}