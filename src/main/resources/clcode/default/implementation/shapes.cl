#include<clcode/default/headers/shapes.h>

float distToOrig(struct ray_t* ray) {
    return length(cross(ray->origin, ray->direction));
}

float3 reflectionRayDirection(float3 direction, float3 normal) {
    //http://paulbourke.net/geometry/reflected/
    return direction - 2 * normal * dot(direction, normal);
}

