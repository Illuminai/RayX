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
        } else {
            result[i] = -1;
        }
    }
}

__kernel void putShapesInMemory(int numShapes,
                __global char* inputData,
                __global struct shape_t* shapes,
                __global char* shapesData) {
    for(int i = 0; i < numShapes; i++) {
        int shape = *((__global int*)inputData);
        inputData += sizeof(int);
        shapes[i] = (struct shape_t){shape, shapesData};
        if(shape == SHAPE) {
            //TODO notify host of error
            //Shape is abstract class, cannot be rendered
            shapes[i] = (struct shape_t){-1L, (__global void*)i};
        } else if(shape == SPHERE) {
            __global double* data = (__global double*)inputData;
            __global struct sphere_t* sphereData =
                (__global struct sphere_t*)(shapesData);
            sphereData->value.x = data[0];
            sphereData->value.y = data[1];
            sphereData->value.z = data[2];
            sphereData->value.w = data[3];

            inputData += 4 * sizeof(double);
            shapesData += sizeof(struct sphere_t);
        } else if(shape == TORUS) {
              __global double* data = (__global double*)inputData;
              __global struct torus_t* torusData =
                  (__global struct sphere_t*)(shapesData);
              torusData->position.x = data[0];
              torusData->position.y = data[1];
              torusData->position.z = data[2];
              torusData->rotation.x = data[3];
              torusData->rotation.y = data[4];
              torusData->rotation.z = data[5];
              torusData->radiusSmall = data[6];
              torusData->radiusBig = data[7];

              inputData += 8 * sizeof(double);
              shapesData += sizeof(struct torus_t);
          } else {
            //TODO notify host of error
            //Invalid shapes are ignored
        }
    }
}
