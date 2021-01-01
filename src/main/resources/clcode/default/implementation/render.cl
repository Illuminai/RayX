#include<clcode/default/headers/render.h>

#define MAX_RENDER_DIST 10
#define MAX_LOCAL_RENDER_SHAPES 50
#define MAX_LOCAL_SPHERE_RTC 50
#define MAX_LOCAL_TORUS_SDF 50
#define MAX_LOCAL_PLANE_RTC 50

/** Although cameraPosition/cameraRotation are 3dim, the parameter passed is 4dim so it can
be passed using clSetKernelArg4d */
__kernel void render(  __write_only image2d_t resultImage,
                            double4 cameraPosition,
                            double4 cameraRotation,
                            double cameraFOV,
                            int globalNumShapes,
                            __global struct shape_t * globalShapes,
                            __global struct sphereRTC_t * sphereData,
                            __global struct torusSDF_t * torusData,
                            __global struct planeRTC_t * planeData) {
    int2 pixCo = (int2){get_global_id(0), get_global_id(1)};
    int w = get_image_width(resultImage);
    int h = get_image_height(resultImage);

    double u = 2.0 * (((pixCo.x + .5) / w) - .5);
    //sign is because image is saved with origin in the
    //upper left corner instead of lower left
    double v = -2.0 * (((pixCo.y + .5) / h) - .5);
    struct ray_t rayToCheck = getRay(u, v, cameraPosition.xyz, cameraRotation.xyz, cameraFOV);

    if(pixCo.x >= w | pixCo.y >= h) {
        return;
    }

    float4 color;

    struct intersection_t inter = (struct intersection_t){
        (__global struct shape_t*)0,
        (struct ray_t*)0,
        (double3){0,0,0},
        (double3){0,0,0},
        0}, tmp;

    traceRay(&rayToCheck, globalNumShapes, globalShapes, &inter);

    if(inter.ray == 0) {
        write_imagef(resultImage, pixCo, (float4){0, 0, 0 ,1});
        return;
    }

    struct ray_t newRay = (struct ray_t){
        inter.point,
        //TODO calculate direction of new ray using normal
        normalize(rayToCheck.direction + 2 * inter.normal)
    };

    {
        double3 lightSource = (double3){-2, 0, 0};
        double angle = -dot(normalize(inter.point - lightSource), inter.normal);
        if(angle < 0) {
            angle = 0;
        } else {
            //angle = 1;
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

struct ray_t getRay(double u, double v, double3 camPos, double3 camRot, double camFOV) {
    struct matrix3x3 rotMat = rotationMatrix(camRot.x,
                                                camRot.y, camRot.z);
        double3 viewDirection = matrixTimesVector(rotMat, (double3){camFOV,0,0});
        double3 cameraRight = matrixTimesVector(rotMat, (double3){0,1,0});
        double3 cameraUp = matrixTimesVector(rotMat, (double3){0,0,1});

        return (struct ray_t) {camPos,
            normalize(viewDirection +
            cameraRight * u + cameraUp * v)};
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
        return firstIntersectionWithSphere(ray, shape, inter);
    } else if(shape->type == TORUS_SDF) {
        return firstIntersectionWithTorus(ray, shape, inter);
    } else if(shape->type == PLANE_RTC) {
        return firstIntersectionWithPlane(ray, shape, inter);
    } else {
        return false;
    }
}

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t* inter) {
    __global struct sphereRTC_t* sphere = (shape->shape);
    double3 omc = ray->origin - shape->position;
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
        inter->normal = normalize(inter->point - shape->position);
        inter->d = d2;
        return true;
    } else if (d1 > 0) {
        inter->point = ray->origin + d1 * ray->direction;
        inter->normal = normalize(inter->point - shape->position);
        inter->d = d1;
        return true;
    } else {
        return false;
    }

}

bool firstIntersectionWithTorus(struct ray_t* pRay,
                                __global struct shape_t* shape,
                                struct intersection_t * inter) {
    __global struct torusSDF_t* torus = shape->shape;
    struct ray_t ray = (struct ray_t){pRay->origin - shape->position, pRay->direction};
    struct matrix3x3 m = rotationMatrix(torus->rotation.x, torus->rotation.y, torus->rotation.z);
    ray.origin = matrixTimesVector(m, ray.origin);
    ray.direction = matrixTimesVector(m, ray.direction);

    if(distToOrig(&ray) > shape->maxRadius) {
        return false;
    }

    double d = 0;
    for(int i = 0; i < 100; i++) {
        double dist = torusSDF(ray.origin + ray.direction * d, torus);
        if(dist < 0.0001) {
            inter->point = pRay->origin + pRay->direction * d;
            inter->d = d;
            m = reverseRotationMatrix( torus->rotation.z,
                                torus->rotation.y,
                                torus->rotation.x);
            inter->normal = sdfNormal(ray.origin + ray.direction * d, torusSDF, torus);
            inter->normal = matrixTimesVector(m, inter->normal);
            return true;
        } else if (dist > 10) {//TODO max distance
            return false;
        }
        d += dist;
    }

    return false;
}

bool firstIntersectionWithPlane(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t * inter) {
    __global struct planeRTC_t* plane = (shape->shape);
    double tmp = dot(ray->direction, plane->normal);
    if(tmp == 0) {
        return false;
    }

    inter->d = (dot(shape->position - ray->origin, plane->normal))/(tmp);
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

struct matrix3x3 reverseRotationMatrix(double gamma, double beta, double alpha) {
    return (struct matrix3x3){{
                (double3){cos(beta) * cos(gamma), cos(gamma) * sin(alpha) * sin(beta) + cos(alpha) * sin(gamma), -cos(alpha) * cos(gamma) * sin(beta) + sin(alpha) * sin(gamma)},
                (double3){-cos(beta) * sin(gamma), cos(alpha) * cos(gamma) - sin(alpha) * sin(beta) * sin(gamma), cos(gamma) * sin(alpha) + cos(alpha) * sin(beta) * sin(gamma)},
                (double3){sin(beta), -cos(beta) * sin(alpha), cos(alpha) * cos(beta)}
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

double distToRay(double3 point, struct ray_t* ray) {
    //No need to divide by the length of the
    //ray->direction, as the length is always 1
    return length(cross(ray->origin - point, ray->direction));
}

double distToOrig(struct ray_t* ray) {
    return length(cross(ray->origin, ray->direction));
}
