#include<clcode/default/headers/render.h>

/** Although cameraPosition/cameraRotation are 3dim, the parameter passed is 4dim so it can
be passed using clSetKernelArg4d */
__kernel void render(  __write_only image2d_t resultImage,
                            double4 cameraPosition,
                            double4 cameraRotation,
                            double cameraFOV,
                            int globalNumShapes,
                            __global struct shape_t * globalShapes) {
    int2 pixCo = (int2){get_global_id(0), get_global_id(1)};
    int w = get_image_width(resultImage);
    int h = get_image_height(resultImage);

    double u = 2.0 * (((pixCo.x + .5) / w) - .5);
    //sign is because image is saved with origin in the
    //upper left corner instead of lower left
    double v = -2.0 * (((pixCo.y + .5) / h) - .5);
    struct ray_t rayToCheck =
        getRay(u, v, cameraPosition.xyz, cameraRotation.xyz, cameraFOV);

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
        write_imagef(resultImage, pixCo, (float4){
            (pixCo.x / (pixCo.x % 4 + 4) + pixCo.y / (pixCo.y % 4 + 4)) % 2, 0,
            (pixCo.x / (pixCo.x % 4 + 4) + pixCo.y / (pixCo.y % 4 + 4)) % 2, 1});
        return;
    }

    {
        double3 lightSource = (double3){-2, 0, 0};
        double angle = -dot(normalize(inter.point - lightSource), inter.normal);
        if(angle < 0) {
            angle = 0;
        } else {
            //angle = 1;
        }
        float3 asdf = (float3){0,0,0};
        switch(inter.obj->type) {
            case TORUS_SDF: asdf = (float3){1,0,0}; break;
            case SPHERE_RTC: asdf = (float3){0,1,0}; break;
            case PLANE_RTC: asdf = (float3){0,0,1}; break;
            case SUBTRACTION_SDF: asdf = (float3){0,1,1}; break;
            case BOX_SDF: asdf = (float3){1,0,1}; break;
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
        if(allShapes[i].shouldRender && firstIntersectionWithShape(ray, &allShapes[i], &tmp)) {
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
    int multiplier = 7;
    inter->obj = shape;
    inter->ray = ray;
    switch(shape->type) {
        case SPHERE_RTC:
            return firstIntersectionWithSphere(ray, shape, inter);
        case PLANE_RTC:
            return firstIntersectionWithPlane(ray, shape, inter);
        case TORUS_SDF:
        case SUBTRACTION_SDF:
        case BOX_SDF:
            return firstIntersectionWithSDF(ray, shape, inter);
        default:
            return false;
    }
}

bool firstIntersectionWithSDF(struct ray_t* pRay, __global struct shape_t* shape, struct intersection_t * inter) {
    struct ray_t ray = (struct ray_t){pRay->origin - shape->position, pRay->direction};

    ray.origin = matrixTimesVector(shape->rotationMatrix, ray.origin);
    ray.direction = matrixTimesVector(shape->rotationMatrix, ray.direction);

    if(distToOrig(&ray) > shape->maxRadius) {
        return false;
    }

    double d = 0;
    for(int i = 0; i < 100; i++) {
        double dist = oneStepSDF(ray.origin + ray.direction * d, shape);
        if(dist < 0.0001) {
            inter->point = pRay->origin + pRay->direction * d;
            inter->d = d;

            inter->normal = sdfNormal(ray.origin + ray.direction * d, oneStepSDF, shape);
            inter->normal = matrixTimesVector(shape->inverseRotationMatrix, inter->normal);
            return true;
        } else if (dist > 10) {//TODO max distance
            return false;
        }
        d += dist;
    }

    return false;
}
double oneStepSDF(double3 point, __global struct shape_t* shape) {
    int index = 0;
    //TODO define max stack size
    struct oneStepSDFArgs_t stack[10];
    stack[0] = (struct oneStepSDFArgs_t) {
       point,
       shape,
       0, 0, 0
    };
    do {
        double3 point = stack[index].point;
        __global struct shape_t* shape = stack[index].shape;
        switch(shape->type) {
                case SUBTRACTION_SDF: {
                    __global struct shape_t* shape1 =
                        ((__global struct subtractionSDF_t*)shape->shape)->shape1;
                    __global struct shape_t* shape2 =
                        ((__global struct subtractionSDF_t*)shape->shape)->shape2;

                    switch(stack[index].status) {
                        case 0:
                            stack[index].status = 1;
                            index++;
                            stack[index] = (struct oneStepSDFArgs_t) {
                                matrixTimesVector(shape1->rotationMatrix,
                                    point - shape1->position),
                                shape1,
                                0, 0, 0
                            };
                            continue;
                        case 1:
                            stack[index].status = 2;
                            stack[index].d1 = stack[index + 1].d1;
                            index++;
                            stack[index] = (struct oneStepSDFArgs_t) {
                                matrixTimesVector(shape2->rotationMatrix,
                                    point - shape2->position),
                                shape2,
                                0, 0, 0
                            };
                            continue;
                        case 2:
                            stack[index].d2 = stack[index + 1].d1;
                            stack[index].d1 = max(-stack[index].d1,
                                stack[index].d2);
                            index--;
                            continue;
                    }
                }
                case TORUS_SDF:
                    stack[index].d1 = torusSDF(point, shape->shape);
                    index--;
                    continue;
                case BOX_SDF:
                    stack[index].d1 = boxSDF(point, shape->shape);
                    index--;
                    continue;
                default:
                    return 0;
            }
    } while(index >= 0);
    return stack[0].d1;
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

double boxSDF(double3 point, __global struct boxSDF_t* box) {
    //TODO optimize
    double3 p = point;
    double3 dimensions = box->dimensions;
    double3 q = fabs((double4){p, 0}).xyz - dimensions;
    return length(max(q,0.0)) + min(max(q.x,max(q.y,q.z)),0.0);
}

double distToRay(double3 point, struct ray_t* ray) {
    //No need to divide by the length of the
    //ray->direction, as the length is always 1
    return length(cross(ray->origin - point, ray->direction));
}

double distToOrig(struct ray_t* ray) {
    return length(cross(ray->origin, ray->direction));
}
