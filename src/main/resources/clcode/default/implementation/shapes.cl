#include<clcode/default/headers/shapes.h>

float sphereSDF(float3 point, __global struct sphere_t* sphere) {
    return length(point) - sphere->radius;
}

float torusSDF(float3 point, __global struct torus_t* torus) {
    float2 q =
        (float2){length(point.yz) - torus->radiusBig, point.x};
    return length(q) - torus->radiusSmall;
}

float planeSDF(float3 point, __global struct plane_t* plane) {
    return dot(point, plane->normal);
}

float boxSDF(float3 point, __global struct box_t* box) {
    //TODO optimize
    float3 p = point;
    float3 dimensions = box->dimensions;
    float3 q = fabs((float4){p, 0}).xyz - dimensions;
    return length(max(q,(float3)0.0)) + min((float)max(q.x,max(q.y,q.z)),(float)0.0);
}

float distToRay(float3 point, struct ray_t* ray) {
    //No need to divide by the length of the
    //ray->direction, as the length is always 1
    return length(cross(ray->origin - point, ray->direction));
}

float distToOrig(struct ray_t* ray) {
    return length(cross(ray->origin, ray->direction));
}