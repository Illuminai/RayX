package com.rayx.opencl.exceptions;

public class CLCommandQueueCreationException extends CLException {
    public CLCommandQueueCreationException(int error) {
        this(null, error);
    }

    public CLCommandQueueCreationException(String message, int error) {
        super(message, error);
    }
}
