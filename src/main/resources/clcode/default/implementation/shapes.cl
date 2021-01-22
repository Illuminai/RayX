#include<clcode/default/headers/shapes.h>

bool firstIntersectionWithSphere(struct ray_t* ray, __global struct shape_t* shape, struct intersection_t* inter) {
    __global struct sphereRTC_t* sphere = (shape->shape);
    numf3 omc = ray->origin - shape->position;
    numf tmp = dot(ray->direction,omc);
    numf delta = tmp * tmp -
                   (dot(omc, omc) - sphere->radius * sphere->radius);

    if(delta < 0) {
        return false;
    }
    delta = sqrt(delta);

    numf d1 = -dot(ray->direction, omc);
    numf d2 = d1;

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
    numf tmp = dot(ray->direction, plane->normal);
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

numf torusSDF(numf3 point, __global struct torusSDF_t* torus) {
    numf2 q =
        (numf2){length(point.yz) - torus->radiusBig, point.x};
    return length(q) - torus->radiusSmall;
}

numf boxSDF(numf3 point, __global struct boxSDF_t* box) {
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