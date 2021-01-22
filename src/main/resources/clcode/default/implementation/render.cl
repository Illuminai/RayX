#include<clcode/default/headers/render.h>

/** Although cameraPosition/cameraRotation are 3dim, the parameter passed is 4dim so it can
be passed using clSetKernelArg4d */
__kernel void render(  __write_only image2d_t resultImage,
                            float4 cameraPosition,
                            float4 cameraRotation,
                            float cameraFOV,
                            int globalNumShapes,
                            __global struct shape_t * globalShapes) {
    int2 pixCo = (int2){get_global_id(0), get_global_id(1)};
    int w = get_image_width(resultImage);
    int h = get_image_height(resultImage);

    numf u = 2.0 * (((pixCo.x + .5) / w) - .5);
    //sign is because image is saved with origin in the
    //upper left corner instead of lower left
    numf v = -2.0 * (((pixCo.y + .5) / h) - .5);

    if(pixCo.x >= w | pixCo.y >= h) {
        return;
    }

    struct ray_t rays[MAX_RAY_BOUNCES];
    struct intersection_t inters[MAX_RAY_BOUNCES];

    rays[0] = getRay(u, v,
        (numf3){cameraPosition.x,cameraPosition.y,cameraPosition.z},
        (numf3){cameraRotation.x,cameraRotation.y,cameraRotation.z}, cameraFOV);
    for(int i = 0; i < MAX_RAY_BOUNCES; i++) {
        inters[i] = (struct intersection_t) {
                (__global struct shape_t*)0,
                (numf3){0,0,0},
                (numf3){0,0,0},
                0 };
    }
    int i;
    numf lumen;
    for(i = 0; i < MAX_RAY_BOUNCES; i++) {
        traceRay(&rays[i], globalNumShapes, globalShapes, &inters[i]);
        if(inters[i].obj == 0) {
            goto finished;
        }

        if(i != MAX_RAY_BOUNCES - 1) {
            rays[i + 1] = (struct ray_t) {inters[i].point, reflectionRayDirection(rays[i].direction, inters[i].normal)};
            rays[i + 1].origin += 2 * EPSILON * rays[i+1].direction;
        }
    }
    finished:
    i--;

    if(i == -1) {
        write_imagef(resultImage, pixCo, (float4){1,0,1,1});
        return;
    }
    lumen = 0;

    float4 color = getTypeColor(inters[i].obj->type);

    for(int j = i; j >= 0; j--) {
        color *= (float).2;
        color += (float).8 * getTypeColor(inters[j].obj->type);
        lumen += inters[j].obj->lumen;
        lumen *= 1 / (1 + inters[j].d);
    }

    float factor = atanpi(lumen) * 2;
    write_imagef(resultImage, pixCo, factor * color);

}

float4 getTypeColor(int type) {
    float3 tc;
    switch(type) {
        case TORUS_SDF:         tc = (float3){1,0,0}; break;
        case SPHERE_RTC:        tc = (float3){0,1,0}; break;
        case PLANE_RTC:         tc = (float3){1,1,1}; break;
        case SUBTRACTION_SDF:   tc = (float3){0,1,1}; break;
        case BOX_SDF:           tc = (float3){1,0,1}; break;
        case UNION_SDF:         tc = (float3){0,.5,.5}; break;
        case INTERSECTION_SDF:  tc = (float3){1,1,1}; break;
        default:                tc = (float3){1,0,1}; break;
    }
    return (float4){tc, 0};
}

struct ray_t getRay(numf u, numf v, numf3 camPos, numf3 camRot, numf camFOV) {
    struct matrix3x3 rotMat = rotationMatrix(camRot);
        numf3 viewDirection = matrixTimesVector(rotMat, (numf3){camFOV,0,0});
        numf3 cameraRight = matrixTimesVector(rotMat, (numf3){0,1,0});
        numf3 cameraUp = matrixTimesVector(rotMat, (numf3){0,0,1});

        return (struct ray_t) {camPos,
            normalize(viewDirection +
            cameraRight * u + cameraUp * v)};
}

void traceRay(  struct ray_t* ray,
                int numShapes,
                __global struct shape_t* allShapes,
                struct intersection_t* inter) {
    struct intersection_t tmp;
    numf maxD = 10;
    for(int i = 0; i < numShapes; i++) {
        if((allShapes[i].flags & FLAG_SHOULD_RENDER) &&
            firstIntersectionWithShape(ray, &allShapes[i], &tmp, maxD)) {
            if(inter->obj != 0) {
                if(dot(tmp.normal, ray->direction) < 0 && tmp.d < inter->d) {
                    *inter = tmp;
                    maxD = tmp.d;
                }
            } else {
                *inter = tmp;
            }
        }
    }
}

bool firstIntersectionWithShape(struct ray_t* ray,
    __global struct shape_t* shape, struct intersection_t* inter,
    numf maxD) {
    inter->obj = shape;
    switch(shape->type) {
        case SPHERE_RTC:
            return firstIntersectionWithSphere(ray, shape, inter);
        case PLANE_RTC:
            return firstIntersectionWithPlane(ray, shape, inter);
        case TORUS_SDF:
        case SUBTRACTION_SDF:
        case BOX_SDF:
        case UNION_SDF:
        case INTERSECTION_SDF:
            return firstIntersectionWithSDF(ray, shape, inter, maxD);
        default:
            inter->obj = 0;
            return false;
    }
}

bool firstIntersectionWithSDF(struct ray_t* pRay, __global struct shape_t* shape, struct intersection_t * inter, numf maxD) {
    struct ray_t ray = (struct ray_t){pRay->origin - shape->position, pRay->direction};

    ray.origin = matrixTimesVector(shape->rotationMatrix, ray.origin);
    ray.direction = matrixTimesVector(shape->rotationMatrix, ray.direction);

    if(distToOrig(&ray) > shape->maxRadius) {
        return false;
    }

    numf d = 0;
    for(int i = 0; i < 1000; i++) {
        numf dist = oneStepSDF(ray.origin + ray.direction * d, shape);
        if(dist < EPSILON) {
            inter->point = pRay->origin + pRay->direction * d;
            inter->d = d;

            inter->normal = sdfNormal(ray.origin + ray.direction * d, oneStepSDF, shape);
            inter->normal = matrixTimesVector(shape->inverseRotationMatrix, inter->normal);
            return true;
        } else if (dist > maxD) {//TODO max distance
            return false;
        }
        d += dist;
    }

    return false;
}

numf oneStepSDF(numf3 point, __global struct shape_t* shape) {
    int index = 0;
    //TODO define max stack size
    struct oneStepSDFArgs_t stack[10];
    stack[0] = (struct oneStepSDFArgs_t) {
       point,
       shape,
       0, 0, 0
    };
    do {
        numf3 point = stack[index].point;
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
                case UNION_SDF: {
                    __global struct shape_t* shape1 =
                        ((__global struct unionSDF_t*)shape->shape)->shape1;
                    __global struct shape_t* shape2 =
                        ((__global struct unionSDF_t*)shape->shape)->shape2;

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
                            stack[index].d1 = min(stack[index].d1,
                                stack[index].d2);
                            index--;
                            continue;
                    }
                }
                case INTERSECTION_SDF: {
                    __global struct shape_t* shape1 =
                        ((__global struct intersectionSDF_t*)shape->shape)->shape1;
                    __global struct shape_t* shape2 =
                        ((__global struct intersectionSDF_t*)shape->shape)->shape2;

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
                            stack[index].d1 = max(stack[index].d1,
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

