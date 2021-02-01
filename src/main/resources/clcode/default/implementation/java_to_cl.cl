#include<clcode/default/headers/math.h>
#include<clcode/default/headers/java_to_cl.h>

__kernel void getShapeSizes(int numShapes,
                __global int* shapesInQuestion,
                __global int* result) {
    for(int i = 0; i < numShapes; i++) {
        int shape = shapesInQuestion[i];
        if(shape == SHAPE) {
            result[i] = sizeof(struct shape_t);
        } else if(shape == SPHERE) {
            result[i] = sizeof(struct sphere_t);
        } else if(shape == TORUS) {
            result[i] = sizeof(struct torus_t);
        } else if(shape == PLANE) {
            result[i] = sizeof(struct plane_t);
        } else if(shape == SUBTRACTION) {
            result[i] = sizeof(struct subtraction_t);
        } else if(shape == BOX) {
            result[i] = sizeof(struct box_t);
        } else if(shape == UNION) {
            result[i] = sizeof(struct union_t);
        } else if(shape == INTERSECTION) {
            result[i] = sizeof(struct intersection_t);
        } else {
            result[i] = -1;
        }
    }
}

__kernel void putShapesInMemory(int numShapes,
                __global char* inputData,
                __global struct shape_t* shapes,
                __global struct sphere_t* dataSphere,
                __global struct torus_t* dataTorus,
                __global struct plane_t* dataPlane,
                __global struct subtraction_t* dataSubtractionSDF,
                __global struct box_t* dataBoxSDF,
                __global struct union_t* dataUnionSDF,
                __global struct intersection_t* dataIntersectionSDF) {
    for(int i = 0; i < numShapes; i++) {
        for(int k = 0; k < sizeof(struct shape_t) / sizeof(float); k++) {
            __global float* tmp= (__global float*)&shapes[i];
            tmp[k] = -101.101;
        }
        long shape = getNextLong(inputData); inputData += sizeof(long);
        long id = getNextLong(inputData); inputData += sizeof(long);
        long flags = getNextLong(inputData); inputData += sizeof(long);
        numf maxRad = getNextFloat(inputData); inputData += sizeof(float);
        numf3 position = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
        numf3 rotation = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
        //Get Material
        long materialType = getNextLong(inputData); inputData += sizeof(long);
        numf3 materialColor = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
        numf materialLumen = getNextFloat(inputData); inputData += sizeof(float);
        struct matrix3x3 rotMatrix = rotationMatrix(rotation);
        shapes[i] = (struct shape_t){shape, id, flags,
                        (struct material_t) {
                            materialType,
                            materialColor,
                            materialLumen
                        },
                        maxRad,
                        position, rotation,
                        rotMatrix,
                        inverse(rotMatrix),
                    0};
        if(shape == SPHERE) {
            shapes[i].shape = dataSphere;
            dataSphere->radius = getNextFloat(inputData); inputData += sizeof(float);
            dataSphere++;
        } else if(shape == TORUS) {
            shapes[i].shape = dataTorus;
            dataTorus->radiusSmall = getNextFloat(inputData); inputData += sizeof(float);
            dataTorus->radiusBig = getNextFloat(inputData); inputData += sizeof(float);
            dataTorus++;
        } else if (shape == PLANE) {
            shapes[i].shape = dataPlane;
            dataPlane->normal = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
            dataPlane++;
        } else if (shape == SUBTRACTION) {
            shapes[i].shape = dataSubtractionSDF;
            dataSubtractionSDF->shape1 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataSubtractionSDF->shape2 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataSubtractionSDF++;
        } else if (shape == BOX) {
            shapes[i].shape = dataBoxSDF;
            dataBoxSDF->dimensions = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
            dataBoxSDF++;
        } else if (shape == UNION) {
            shapes[i].shape = dataUnionSDF;
            dataUnionSDF->shape1 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataUnionSDF->shape2 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataUnionSDF++;
        } else if (shape == INTERSECTION) {
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
