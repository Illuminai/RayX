#include<clcode/default/headers/benchmark.h>

__kernel void testPerformanceDouble(__global double4* input, __global double* result, int amount) {
    int glob = get_global_id(0);
    if(glob >= amount) {
        return;
    }

    double4 d = input[glob];

    for(int i = 0; i < BENCHMARK_MAX; i++) {
        d = sin(d) + cos(d);
    }

    result[glob] = d.x + d.y + d.z + d.w;
}

__kernel void testPerformanceFloat(__global float4* input, __global float* result, int amount) {
    int glob = get_global_id(0);
    if(glob >= amount) {
        return;
    }

    float4 d = input[glob];

    for(int i = 0; i < BENCHMARK_MAX; i++) {
        d = sin(d) + cos(d);
    }


    result[glob] = d.x + d.y + d.z + d.w;
}
