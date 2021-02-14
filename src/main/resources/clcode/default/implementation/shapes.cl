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

float octahedronSDF(float3 point, __global struct octahedron_t* octahedron) {
    float3 p = fabs(point);
    float m = p.x + p.y + p.z - octahedron->size;
    float3 q;

    if ( 3.0*p.x < m ) {
        q = p.xyz;
    } else if ( 3.0*p.y < m ) {
        q = p.yzx;
    } else if ( 3.0*p.z < m ) {
        q = p.zxy;
    } else {
        return m * 0.57735027;
    }

    float k = clamp(0.5f*(q.z-q.y+octahedron->size),0.0f,octahedron->size);
    return length((float3){q.x,q.y-octahedron->size+k,q.z-k});
}


float distToOrig(struct ray_t* ray) {
    return length(cross(ray->origin, ray->direction));
}

float3 reflectionRayDirection(float3 direction, float3 normal) {
    //http://paulbourke.net/geometry/reflected/
    return direction - 2 * normal * dot(direction, normal);
}

