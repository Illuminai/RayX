package com.rayx.opencl.exceptions;

public class CLKernelEnqueueException extends CLException {
    public CLKernelEnqueueException(int error) {
        this(null, error);
    }

    public CLKernelEnqueueException(String message, int error) {
        super(message, error);
    }
}
