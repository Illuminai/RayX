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