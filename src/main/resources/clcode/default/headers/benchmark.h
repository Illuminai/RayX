#ifndef __HEADER_BENCHMARK_H
#define __HEADER_BENCHMARK_H

#define BENCHMARK_MAX 1000

__kernel void testPerformanceDouble(__global double4* input, __global double* result, int size);

__kernel void testPerformanceFloat(__global float4* input, __global float* result, int size);

#endif