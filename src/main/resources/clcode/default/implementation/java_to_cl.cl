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
        } else {
            result[i] = -1;
        }
    }
}

__kernel void putShapesInMemory(int numShapes,
                __global char* inputData,
                __global struct shape_t* shapes,
                __global struct sphereRTC_t* dataSphere,
                __global struct torusSDF_t* dataTorus) {
    for(int i = 0; i < numShapes; i++) {
        int shape = *((__global int*)inputData);
        inputData += sizeof(int);
        if(shape == SPHERE_RTC) {
            shapes[i] = (struct shape_t){shape, dataSphere};

            dataSphere->position.x = getNextDouble(inputData); inputData += sizeof(double);
            dataSphere->position.y = getNextDouble(inputData); inputData += sizeof(double);
            dataSphere->position.z = getNextDouble(inputData); inputData += sizeof(double);
            dataSphere->radius = getNextDouble(inputData); inputData += sizeof(double);

            dataSphere++;
        } else if(shape == TORUS_SDF) {
            shapes[i] = (struct shape_t){shape, dataTorus};

            dataTorus->position.x = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->position.y = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->position.z = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->rotation.x = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->rotation.y = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->rotation.z = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->radiusSmall = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->radiusBig = getNextDouble(inputData); inputData += sizeof(double);

            dataTorus++;
        } else {
            //TODO notify host of error
            shapes[i] = (struct shape_t){shape, (__global void*)(long)i};
        }
    }
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
