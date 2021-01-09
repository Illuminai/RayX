#include<clcode/default/headers/matrixmath.h>
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
                __global struct subtractionSDF_t* dataSubtractionSDF) {
    for(int i = 0; i < numShapes; i++) {
        for(int k = 0; k < sizeof(struct shape_t) / sizeof(long); k++) {
            __global long* tmp= (__global long*)&shapes[i];
            tmp[k] = -12;
        }
        long shape = getNextLong(inputData); inputData += sizeof(long);
        long id = getNextLong(inputData); inputData += sizeof(long);
        long shouldRender = getNextLong(inputData); inputData += sizeof(long);
        double maxRad = getNextDouble(inputData); inputData += sizeof(double);
        double3 position = getNextDouble3 (inputData); inputData += sizeof(double) * 3;
        double3 rotation = getNextDouble3 (inputData); inputData += sizeof(double) * 3;
        shapes[i] = (struct shape_t){shape, id, shouldRender, maxRad, position, rotation,
                        rotationMatrix(rotation.x, rotation.y, rotation.z),
                        reverseRotationMatrix(rotation.z, rotation.y, rotation.x),
                    0};
        if(shape == SPHERE_RTC) {
            shapes[i].shape = dataSphere;
            dataSphere->radius = getNextDouble(inputData); inputData += sizeof(double);
            dataSphere++;
        } else if(shape == TORUS_SDF) {
            shapes[i].shape = dataTorus;
            dataTorus->radiusSmall = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->radiusBig = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus++;
        } else if (shape == PLANE_RTC) {
            shapes[i].shape = dataPlane;
            dataPlane->normal = getNextDouble3 (inputData); inputData += sizeof(double) * 3;
            dataPlane++;
        } else if (shape == SUBTRACTION_SDF) {
            shapes[i].shape = dataSubtractionSDF;
            dataSubtractionSDF->shape1 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataSubtractionSDF->shape2 = shapes + getNextLong(inputData); inputData += sizeof(long);
            dataSubtractionSDF++;
        } else {
            //TODO notify host of error
        }
    }
}

double3 getNextDouble3(__global char* data) {
    return (double3){
        getNextDouble(data),
        getNextDouble(data + sizeof(double)),
        getNextDouble(data + 2 * sizeof(double))
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
double getNextDouble(__global char* data) {
    double res;
    char* pointer = (char*)&res;
    for(int k = 0; k < sizeof(double); k++) {
        *pointer = data[0];
        pointer++;
        data++;
    }
    return res;
}
