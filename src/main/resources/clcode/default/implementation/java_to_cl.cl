#include<clcode/default/headers/math.h>
#include<clcode/default/headers/java_to_cl.h>

float3 getNextFloat3(__global char* data) {
    return (float3){
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
float getNextFloat(__global char* data) {
    float res;
    char* pointer = (char*)&res;
    for(int k = 0; k < sizeof(float); k++) {
        *pointer = data[0];
        pointer++;
        data++;
    }
    return (float)res;
}
