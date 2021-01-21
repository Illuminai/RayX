#include<clcode/default/headers/math.h>
#include<clcode/default/headers/java_to_cl.h>

__kernel void getShapeSizes(int numShapes,
                __global int* shapesInQuestion,
                __global int* result) {
    for(int i = 0; i < numShapes; i++) {
        int shape = shapesInQuestion[i];
        if(shape == SHAPE) {
            result[i] = sizeof(struct shape_t);
        } else if(shape == SPHERE_RTC) {
            result[i] = sizeof(struct sphereRTC_t);
        } else if(shape == TORUS_SDF) {
            result[i] = sizeof(struct torusSDF_t);
        } else if(shape == PLANE_RTC) {
            result[i] = sizeof(struct planeRTC_t);
        } else if(shape == SUBTRACTION_SDF) {
            result[i] = sizeof(struct subtractionSDF_t);
        } else if(shape == BOX_SDF) {
            result[i] = sizeof(struct boxSDF_t);
        } else if(shape == UNION_SDF) {
            result[i] = sizeof(struct unionSDF_t);
        } else if(shape == INTERSECTION_SDF) {
            result[i] = sizeof(struct intersectionSDF_t);
        } else {
            result[i] = -1;
        }
    }
}

__kernel void putShapesInMemory(int numShapes,
                __global char* inputData,
                __global struct shape_t* shapes,
                __global struct sphereRTC_t* dataSphere,
                __global struct torusSDF_t* dataTorus,
                __global struct planeRTC_t* dataPlane,
                __global struct subtractionSDF_t* dataSubtractionSDF,
                __global struct boxSDF_t* dataBoxSDF,
                __global struct unionSDF_t* dataUnionSDF,
                __global struct intersectionSDF_t* dataIntersectionSDF) {
    for(int i = 0; i < numShapes; i++) {
        for(int k = 0; k < sizeof(struct shape_t) / sizeof(long); k++) {
            __global long* tmp= (__global long*)&shapes[i];
            tmp[k] = -12;
        }
        long shape = getNextLong(inputData); inputData += sizeof(long);
        long id = getNextLong(inputData); inputData += sizeof(long);
        long shouldRender = getNextLong(inputData); inputData += sizeof(long);
        numf maxRad = getNextFloat(inputData); inputData += sizeof(float);
        numf lumen = getNextFloat(inputData); inputData += sizeof(float);
        numf3 position = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
        numf3 rotation = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
        shapes[i] = (struct shape_t){shape, id, shouldRender, maxRad, lumen, position, rotation,
                        rotationMatrix(rotation.x, rotation.y, rotation.z),
                        reverseRotationMatrix(rotation.z, rotation.y, rotation.x),
                    0};
        if(shape == SPHERE_RTC) {
            shapes[i].shape = dataSphere;
            dataSphere->radius = getNextFloat(inputData); inputData += sizeof(float);
            dataSphere++;
        } else if(shape == TORUS_SDF) {
            shapes[i].shape = dataTorus;
            dataTorus->radiusSmall = getNextFloat(inputData); inputData += sizeof(float);
            dataTorus->radiusBig = getNextFloat(inputData); inputData += sizeof(float);
            dataTorus++;
        } else if (shape == PLANE_RTC) {
            shapes[i].shape = dataPlane;
            dataPlane->normal = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
            dataPlane++;
        } else if (shape == SUBTRACTION_SDF) {
            shapes[i].shape = dataSubtractionSDF;
            dataSubtractionSDF->shape1 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataSubtractionSDF->shape2 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataSubtractionSDF++;
        } else if (shape == BOX_SDF) {
            shapes[i].shape = dataBoxSDF;
            dataBoxSDF->dimensions = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
            dataBoxSDF++;
        } else if (shape == UNION_SDF) {
            shapes[i].shape = dataUnionSDF;
            dataUnionSDF->shape1 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataUnionSDF->shape2 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataUnionSDF++;
        } else if (shape == INTERSECTION_SDF) {
            shapes[i].shape = dataIntersectionSDF;
            dataIntersectionSDF->shape1 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataIntersectionSDF->shape2 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataIntersectionSDF++;
        }else {
            //TODO notify host of error
        }
    }
}

numf3 getNextFloat3(__global char* data) {
    return (numf3){
        getNextFloat(data),
        getNextFloat(data + sizeof(float)),
        getNextFloat(data + 2 * sizeof(float))
        };
}

long getNextLong(__global char* data) {
    long res;
    char* pointer = (char*)&res;
    for(int k = 0; k < sizeof(long); k++) {
        *pointer = data[0];
        pointer++;
        data++;
    }
    return res;
}
numf getNextFloat(__global char* data) {
    float res;
    char* pointer = (char*)&res;
    for(int k = 0; k < sizeof(float); k++) {
        *pointer = data[0];
        pointer++;
        data++;
    }
    return (numf)res;
}
