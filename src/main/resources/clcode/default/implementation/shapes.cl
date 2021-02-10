#include<clcode/default/headers/shapes.h>

numf sphereSDF(numf3 point, __global struct sphere_t* sphere) {
    return length(point) - sphere->radius;
}

numf torusSDF(numf3 point, __global struct torus_t* torus) {
    numf2 q =
        (numf2){length(point.yz) - torus->radiusBig, point.x};
    return length(q) - torus->radiusSmall;
}

numf planeSDF(numf3 point, __global struct plane_t* plane) {
    return dot(point, plane->normal);
}

numf boxSDF(numf3 point, __global struct box_t* box) {
    //TODO optimize
    numf3 p = point;
    numf3 dimensions = box->dimensions;
    numf3 q = fabs((numf4){p, 0}).xyz - dimensions;
    return length(max(q,(numf3)0.0)) + min((numf)max(q.x,max(q.y,q.z)),(numf)0.0);
}

numf octahedronSDF(numf3 point, __global struct octahedron_t* octahedron) {
    numf3 p = fabs(point);
    numf m = p.x + p.y + p.z - octahedron->size;
    numf3 q;

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
    return length((numf3){q.x,q.y-octahedron->size+k,q.z-k});
}

numf distToRay(numf3 point, struct ray_t* ray) {
    //No need to divide by the length of the
    //ray->direction, as the length is always 1
    return length(cross(ray->origin - point, ray->direction));
}

numf distToOrig(struct ray_t* ray) {
    return length(cross(ray->origin, ray->direction));
}

numf3 reflectionRayDirection(numf3 direction, numf3 normal) {
    //http://paulbourke.net/geometry/reflected/
    return direction - 2 * normal * dot(direction, normal);
}

