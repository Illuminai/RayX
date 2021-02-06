#include <clcode/default/headers/render.h>

/**
 * Debug viewport
 */
__kernel void renderDebug(__write_only image2d_t resultImage, float height,
                          float width, float4 cameraPosition,
                          float4 cameraRotation, float cameraFOV,
                          int globalNumShapes,
                          __global struct shape_t *globalShapes) {
    int2 pixCo = (int2){get_global_id(0), get_global_id(1)};
    int w = get_image_width(resultImage);
    int h = get_image_height(resultImage);

    // float u = 2.0 * (((pixCo.x + .5) / width) - .5);
    // float v = -2.0 * (((pixCo.y + .5) / height) - .5);

    float u = 2.0 * ((pixCo.x + 0.5) / w) - 1;
    float v = 1 - 2.0 * ((pixCo.y + 0.5) / h);

    float aspectRatio = width / height;
    v = ((v - 0.5) * aspectRatio) + 0.5;

    if (pixCo.x >= w | pixCo.y >= h) {
        return;
    }

    struct ray_t ray;
    struct rayIntersection_t intersection;

    intersection = (struct rayIntersection_t){
        (__global struct shape_t *)0, (float3){0, 0, 0}, (float3){0, 0, 0}, 0};

    ray = getRay(u, v, cameraPosition.xyz, cameraRotation.xyz, cameraFOV);
    traceRay(&ray, globalNumShapes, globalShapes, &intersection);

    if (intersection.obj == 0) {
        write_imagef(resultImage, pixCo, (float4){0, 0, 1, 1});
        return;
    }

    float4 color = getDebugColor(intersection);
    write_imagef(resultImage, pixCo, color);
    return;
}

float4 getDebugColor(struct rayIntersection_t inter) {
    return (float4){(inter.normal.x + 1.0) * 0.5, (inter.normal.y + 1.0) * 0.5,
                    (inter.normal.z + 1.0) * 0.5, 1.0};
}

/** Although cameraPosition/cameraRotation are 3dim, the parameter passed is
4dim so it can be passed using clSetKernelArg4d */
__kernel void render(__write_only image2d_t resultImage, float height,
                     float width, float4 cameraPosition, float4 cameraRotation,
                     float cameraFOV, int globalNumShapes,
                     __global struct shape_t *globalShapes) {
    int2 pixCo = (int2){get_global_id(0), get_global_id(1)};
    int w = get_image_width(resultImage);
    int h = get_image_height(resultImage);

    // float u = 2.0 * (((pixCo.x + .5) / width) - .5);
    // float v = -2.0 * (((pixCo.y + .5) / height) - .5);

    float u = 2.0 * ((pixCo.x + 0.5) / w) - 1;
    float v = 1 - 2.0 * ((pixCo.y + 0.5) / h);

    float aspectRatio = width / height;
    v = ((v - 0.5) * aspectRatio) + 0.5;

    if (pixCo.x >= w | pixCo.y >= h) {
        return;
    }

    struct ray_t rays[MAX_RAY_BOUNCES];
    struct rayIntersection_t inters[MAX_RAY_BOUNCES];

    rays[0] = getRay(u, v,
        (float3){cameraPosition.x,cameraPosition.y,cameraPosition.z},
        (float3){cameraRotation.x,cameraRotation.y,cameraRotation.z}, cameraFOV);
    for(int i = 0; i < MAX_RAY_BOUNCES; i++) {
        inters[i] = (struct rayIntersection_t) {
                (__global struct shape_t*)0,
                (float3){0,0,0},
                (float3){0,0,0},
                0 };
    }
    int i;
    float lumen;
    for(i = 0; i < MAX_RAY_BOUNCES; i++) {
        traceRay(&rays[i], globalNumShapes, globalShapes, &inters[i]);
        if (inters[i].obj == 0) {
            goto finished;
        }

        if(i != MAX_RAY_BOUNCES - 1) {
            rays[i + 1] = nextRayOnIntersection(&rays[i], &inters[i]);
        }
    }
    finished:
    i--;

    if (i == -1) {
        write_imagef(resultImage, pixCo, (float4){1, 0, 1, 1});
        return;
    }
    lumen = 0;

    float3 color = inters[i].obj->material.color;

    for(int j = i; j >= 0; j--) {
        color *= (float).2;
        color += (float).8 * inters[j].obj->material.color;
        lumen += inters[j].obj->material.lumen;
        lumen *= 1 / (1 + inters[j].d);
    }

    float factor = atanpi(lumen) * 2;
    write_imagef(resultImage, pixCo, (float4){factor * color,1});

}

float3 perfectReflectionRayDirection(float3 direction, float3 normal) {
    //http://paulbourke.net/geometry/reflected/
    return direction - 2 * normal * dot(direction, normal);
}

struct ray_t getRay(float u, float v, float3 camPos, float3 camRot, float camFOV) {
    struct matrix3x3 rotMat = rotationMatrix(camRot);
        float3 viewDirection = matrixTimesVector(rotMat, (float3){camFOV,0,0});
        float3 cameraRight = matrixTimesVector(rotMat, (float3){0,1,0});
        float3 cameraUp = matrixTimesVector(rotMat, (float3){0,0,1});

    return (struct ray_t){
        camPos, normalize(viewDirection + cameraRight * u + cameraUp * v)};
}

void traceRay(  struct ray_t* ray,
                int numShapes,
                __global struct shape_t* allShapes,
                struct rayIntersection_t* inter) {
    struct rayIntersection_t tmp;
    float maxD = 10;
    for(int i = 0; i < numShapes; i++) {
        if((allShapes[i].flags & FLAG_SHOULD_RENDER) &&
            firstIntersectionWithShape(ray, &allShapes[i], &tmp, maxD)) {
            if(inter->obj != 0) {
                if(tmp.d < inter->d) {
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
    __global struct shape_t* shape, struct rayIntersection_t* inter,
    float maxD) {
    inter->obj = shape;
    return firstIntersectionWithSDF(ray, shape, inter, maxD);
}

bool firstIntersectionWithSDF(struct ray_t* pRay, __global struct shape_t* shape, struct rayIntersection_t * inter, float maxD) {
    struct ray_t ray = (struct ray_t){pRay->origin - shape->position, pRay->direction};

    ray.origin = matrixTimesVector(shape->rotationMatrix, ray.origin);
    ray.direction = matrixTimesVector(shape->rotationMatrix, ray.direction);

    if(distToOrig(&ray) > shape->maxRadius && shape->maxRadius != -1) {
        return false;
    }

    float d = 0;
    for(int i = 0; i < 100; i++) {
        float dist = fabs(oneStepSDF(ray.origin + ray.direction * d, shape));
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

struct ray_t nextRayOnIntersection(struct ray_t* oldRay, struct rayIntersection_t* inter) {
    struct material_t mat = inter->obj->material;
    switch(mat.type) {
        default:
        case MATERIAL_REFLECTION: {
            struct ray_t ray = (struct ray_t) {inter->point, perfectReflectionRayDirection(oldRay->direction, inter->normal)};
            ray.origin += 1.1f * EPSILON * inter->normal;
            return ray;
        }
        case MATERIAL_REFRACTION: {
            //TODO optimize
            float3 n1 = inter->normal;
            float3 r1 = oldRay->direction;
            float f = mat.refractionIndex;
            if(dot(n1, -r1) < 0) {
                n1 = -n1;
                f = 1 / f;
            }
            float alpha = acos(dot(-r1, n1));
            float3 k1 = n1 * cos(alpha) + r1;
            float delta = sin(alpha) / f;
            float3 n2, k2;
            if(delta <= 1) {
                float beta = asin(delta);
                n2 = -n1 * cos(beta);
                k2 = normalize(k1) * sin(beta);
            } else {
                n2 = n1 * cos(alpha);
                k2 = normalize(k1) * sin(alpha);
            }
            //r2 is already normalized
            float3 r2 = n2 + k2;

            struct ray_t ray = (struct ray_t) {inter->point, r2};
            ray.origin += 10.0f * EPSILON * n2;
            return ray;
        }
    }
}

float oneStepSDF(float3 point, __global struct shape_t* shape) {
    int index = 0;
    // TODO define max stack size
    struct oneStepSDFArgs_t stack[10];
    stack[0] = (struct oneStepSDFArgs_t){point, shape, 0, 0, 0};
    do {
        float3 point = stack[index].point;
        __global struct shape_t* shape = stack[index].shape;
        switch(shape->type) {
                case SUBTRACTION: {
                    __global struct shape_t* shape1 =
                        ((__global struct subtraction_t*)shape->shape)->shape1;
                    __global struct shape_t* shape2 =
                        ((__global struct subtraction_t*)shape->shape)->shape2;

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
                case UNION: {
                    __global struct shape_t* shape1 =
                        ((__global struct union_t*)shape->shape)->shape1;
                    __global struct shape_t* shape2 =
                        ((__global struct union_t*)shape->shape)->shape2;

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
                case INTERSECTION: {
                    __global struct shape_t* shape1 =
                        ((__global struct intersection_t*)shape->shape)->shape1;
                    __global struct shape_t* shape2 =
                        ((__global struct intersection_t*)shape->shape)->shape2;

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
                case SPHERE:
                    stack[index].d1 = sphereSDF(point, shape->shape);
                    index--;
                    continue;

                case TORUS:
                    stack[index].d1 = torusSDF(point, shape->shape);
                    index--;
                    continue;
                case PLANE:
                    stack[index].d1 = planeSDF(point, shape->shape);
                    index--;
                    continue;
                case BOX:
                    stack[index].d1 = boxSDF(point, shape->shape);
                    index--;
                    continue;
                default:
                    return 0;
            }
    } while(index >= 0);
    return stack[0].d1;
}

