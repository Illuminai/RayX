package com.rayx.opencl;

public class CLDevice {

    private final long device;
    private long context, commandQueue;

    public CLDevice(long device) {
        this.device = device;
    }

    public long getDevice() {
        return device;
    }

    public long getContext() {
        return context;
    }

    public void setContext(long context) {
        this.context = context;
    }

    public long getCommandQueue() {
        return commandQueue;
    }

    public void setCommandQueue(long commandQueue) {
        this.commandQueue = commandQueue;
    }

}
