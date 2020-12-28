#include<clcode/default/headers/render.h>

/** Although cameraPosition/cameraRotation are 3dim, the parameter passed is 4dim so it can
be passed using clSetKernelArg4d */
__kernel void render(  __write_only image2d_t resultImage,
                            double4 cameraPosition,
                            double4 cameraRotation,
                            double cameraFOV,
                            int numShapes,
                            __global struct shape_t * shapes,
                            __global struct sphereRTC_t * sphereData,
                            __global struct torusSDF_t * torusData,
                            __global struct planeRTC_t * planeData) {
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

    struct intersection_t inter = (struct intersection_t){
        (__global struct shape_t*)0,
        (struct ray_t*)0,
        (double3){0,0,0},
        (double3){0,0,0},
        0}, tmp;

    traceRay(&rayToCheck, numShapes, shapes, &inter);

    if(inter.ray == 0) {
        write_imagef(resultImage, pixCo, (float4){0, 0, 0 ,1});
        return;
    }

    struct ray_t newRay = (struct ray_t){
        inter.point,
        normalize(rayToCheck.direction + 2 * inter.normal)
    };

    {
        double3 lightSource = (double3){-1, 0, 0};
        double angle = -dot(normalize(inter.point - lightSource), inter.normal);
        if(angle < 0) {
            angle = 0;
        }
        float3 asdf = (float3){0,0,0};
        if(inter.obj->type == TORUS_SDF) {
            asdf = (float3){1,0,0};
        } else if(inter.obj->type == SPHERE_RTC) {
           asdf = (float3){0,1,0};
       } else if(inter.obj->type == PLANE_RTC) {
          asdf = (float3){0,0,1};
      }
        color = (float4){(float)angle * asdf, 1};
    }
    write_imagef(resultImage, pixCo, color);
}

void traceRay(  struct ray_t* ray,
                int numShapes,
                __global struct shape_t* allShapes,
                struct intersection_t* inter) {
    struct intersection_t tmp;
    for(int i = 0; i < numShapes; i++) {
            if(firstIntersectionWithShape(ray, &allShapes[i], &tmp)) {
                if(inter->ray != 0) {
                    if(tmp.d < inter->d) {
                        *inter = tmp;
                    }
                } else {
                    *inter = tmp;
                }
            }
        }
}

bool firstIntersectionWithShape(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t* inter) {
    inter->obj = shape;
    inter->ray = ray;
    if(shape->type == SPHERE_RTC) {
        return firstIntersectionWithSphere(ray, shape->shape, inter);
    } else if(shape->type == TORUS_SDF) {
        return firstIntersectionWithTorus(ray, shape->shape, inter);
    } else if(shape->type == PLANE_RTC) {
        return firstIntersectionWithPlane(ray, shape->shape, inter);
    } else {
        return false;
    }
}

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct sphereRTC_t* sphere, struct intersection_t* inter) {
    double3 omc = ray->origin - sphere->position;
    double tmp = dot(ray->direction,omc);
    double delta = tmp * tmp -
                   (dot(omc, omc) - sphere->radius * sphere->radius);

    if(delta < 0) {
        return false;
    }
    delta = sqrt(delta);

    double d1 = -dot(ray->direction, omc);
    double d2 = d1;

    d1 += delta;
    d2 -= delta;

    if(d2 > 0) {
        inter->point = ray->origin + d2 * ray->direction;
        inter->normal = normalize(inter->point - sphere->position);
        inter->d = d2;
        return true;
    } else if (d1 > 0) {
        inter->point = ray->origin + d1 * ray->direction;
        inter->normal = normalize(inter->point - sphere->position);
        inter->d = d1;
        return true;
    } else {
        return false;
    }

}

bool firstIntersectionWithTorus(struct ray_t* ray,
                                __global struct torusSDF_t* torus,
                                struct intersection_t * inter) {
    inter->d = 0;
    for(int i = 0; i < 100; i++) {
        double dist = torusSDF(ray->origin + ray->direction * inter->d, torus);
        if(dist < 0.0001) {
            inter->point = ray->origin + ray->direction * inter->d;
            inter->normal = sdfNormal(inter->point, torusSDF, torus);
            return true;
        } else if (dist > 10) {
            return false;
        }
        inter->d += dist;
    }

    return false;
}

bool firstIntersectionWithPlane(struct ray_t* ray, __global struct planeRTC_t* plane, struct intersection_t * inter) {
    double tmp = dot(ray->direction, plane->normal);
    if(tmp == 0) {
        return false;
    }

    inter->d = (dot(plane->position - ray->origin, plane->normal))/(tmp);
    if(inter->d < 0) {
        return false;
    }
    inter->point = ray->origin + inter->d * ray->direction;
    inter->normal = plane->normal;
    return true;
}

double torusSDF(double3 point, __global struct torusSDF_t* torus) {
    double2 q =
        (double2){length(point.yz) - torus->radiusBig, point.x};
    return length(q) - torus->radiusSmall;
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