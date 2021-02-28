#include <clcode/default/headers/render.h>

/**
 * Debug viewport
 */
__kernel void renderDebug(__write_only image2d_t resultImage, float width,
                          float height, float4 cameraPosition,
                          float4 cameraRotation, float cameraFOV,
                          int globalNumShapes,
                          __global struct shape_t *globalShapes,
                          int samples) {
    int2 pixCo = (int2){get_global_id(0), get_global_id(1)};

    if (pixCo.x >= width | pixCo.y >= height) {
        return;
    }

    struct ray_t ray;
    struct rayIntersection_t intersection;

    float3 sampledColor = (float3) {0.0f,0.0f,0.0f};

    for(int s = 1; s <= samples; s++) {

        float u = 2.0 * ((pixCo.x + getSampledHalton(s,2) + 0.5) / width) - 1;
        float v = 1.0 - 2.0 * ((pixCo.y + getSampledHalton(s,3) + 0.5) / height);

        float aspectRatio = height / width;
        v = v * aspectRatio;

        intersection = (struct rayIntersection_t){
            (__global struct shape_t *)0, (float3){0, 0, 0}, (float3){0, 0, 0}, 0};

        ray = getRay(u, v, cameraPosition.xyz, cameraRotation.xyz, cameraFOV);
        traceRay(&ray, globalNumShapes, globalShapes, &intersection);

        float3 color = (float3){0.0f,0.0f,0.0f};
        float factor = 0.0f;
        if (intersection.obj != 0) {
            factor = max(0.0f, dot(intersection.normal, -ray.direction) );
            color = getNormalColor(intersection);
        }

        sampledColor = (sampledColor * (s - 1) + (factor * color)) * (1.0f / (float) s);
    }

    write_imagef(resultImage, pixCo, (float4) {sampledColor, 1.0f});
    return;
}

float3 getNormalColor(struct rayIntersection_t inter) {
    return (float3){(inter.normal.x + 1.0f) * 0.5f, (inter.normal.y + 1.0f) * 0.5f,
                    (inter.normal.z + 1.0f) * 0.5f};
}

float getSampledHalton(int index, int base) {
    float f = 1.0f;
    float r = 0.0f;
    float i = index;

    while (i > 0) {
        f = f / (float)base;
        r = r + f * fmod(i , (float) base);
        i = floor(i / (float)base);
    }
    return r;
}

/** Although cameraPosition/cameraRotation are 3dim, the parameter passed is
4dim so it can be passed using clSetKernelArg4d */
__kernel void render(__write_only image2d_t resultImage, float width,
                     float height, float4 cameraPosition, float4 cameraRotation,
                     float cameraFOV, int globalNumShapes,
                     __global struct shape_t *globalShapes, int samples) {
    int2 pixCo = (int2){get_global_id(0), get_global_id(1)};

    if (pixCo.x >= width | pixCo.y >= height) {
        return;
    }

    float u = 2.0 * ((pixCo.x + 0.5) / width) - 1;
    float v = 1.0 - 2.0 * ((pixCo.y + 0.5) / height);

    float aspectRatio = height / width;
    v = v * aspectRatio;

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
        float t = 0.5f * (rays[0].direction.z + 1.0f );
        float3 color = (float3) { 1.0f,1.0f,1.0f } * (1.0f - t) + t * (float3) { 0.5f, 0.7f, 1.0f };
        write_imagef(resultImage, pixCo, (float4){color, 1});
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

    float d = 0;
    for(int i = 0; i < 100; i++) {
        float3 tmp = (ray.origin + ray.direction * d)/shape->size;
        float dist = fabs(oneStepSDF(tmp, shape)) * shape->size;
        if(dist < EPSILON_INTERSECTION) {
            inter->point = pRay->origin + pRay->direction * d;
            inter->d = d;

            //Do not multiply by shape->size, as it is already normalized
            inter->normal = sdfNormal(tmp, oneStepSDF, shape);
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
            ray.origin += EPSILON_NEW_RAY * inter->normal;
            return ray;
        }
        case MATERIAL_REFRACTION: {
            //TODO optimize
            float3 n1 = inter->normal;
            float3 r1 = oldRay->direction;
            float f = mat.properties.refraction.refractionIndex;
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
            ray.origin += EPSILON_NEW_RAY * n2;
            return ray;
        }
    }
}
