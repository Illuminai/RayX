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
                __global struct planeRTC_t* dataPlane) {
    for(int i = 0; i < numShapes; i++) {
        int shape = getNextInt(inputData); inputData += sizeof(int);
        double maxRad = getNextDouble(inputData); inputData += sizeof(double);
        double3 position;
        position.x = getNextDouble(inputData); inputData += sizeof(double);
        position.y = getNextDouble(inputData); inputData += sizeof(double);
        position.z = getNextDouble(inputData); inputData += sizeof(double);

        if(shape == SPHERE_RTC) {
            shapes[i] = (struct shape_t){shape, maxRad, position, dataSphere};

            dataSphere->radius = getNextDouble(inputData); inputData += sizeof(double);

            dataSphere++;
        } else if(shape == TORUS_SDF) {
            shapes[i] = (struct shape_t){shape, maxRad, position, dataTorus};

            dataTorus->rotation.x = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->rotation.y = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->rotation.z = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->radiusSmall = getNextDouble(inputData); inputData += sizeof(double);
            dataTorus->radiusBig = getNextDouble(inputData); inputData += sizeof(double);

            dataTorus++;
        } else if (shape == PLANE_RTC) {
            shapes[i] = (struct shape_t){shape, maxRad, position, dataPlane};

            dataPlane->normal.x = getNextDouble(inputData); inputData += sizeof(double);
            dataPlane->normal.y = getNextDouble(inputData); inputData += sizeof(double);
            dataPlane->normal.z = getNextDouble(inputData); inputData += sizeof(double);
            dataPlane++;
        } else if (shape == SUBTRACTION_SDF) {
            //TODO
            shapes[i] = (struct shape_t){shape, maxRad, position, dataPlane};

            inputData += sizeof(struct subtractionSDF_t);
        } else {
            //TODO notify host of error
            shapes[i] = (struct shape_t){shape, 0, (double3){0,0,0}, (__global void*)(long)i};
        }
    }
}

int getNextInt(__global char* data) {
    int res;
    char* pointer = (char*)&res;
    for(int k = 0; k < sizeof(int); k++) {
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
