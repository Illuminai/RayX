package com.rayx.opencl;

import org.lwjgl.opencl.CL22;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class CLContext {
    private long context, device, commandQueue;
    private HashMap<String, CLKernel> kernels;
    private HashMap<String, CLMemoryObject> memoryObjects;

    /** Only {@link CLManager} should create instances */
    CLContext(long device, long context, long commandQueue) {
        this.context = context;
        this.device = device;
        this.commandQueue = commandQueue;
        kernels = new HashMap<>();
        memoryObjects = new HashMap<>();
    }

    public void freeMemoryObject(String id) {
        long p = getMemoryObject(id).getPointer();
        CLManager.freeMemoryInternal(p);
        memoryObjects.remove(id);
    }

    public void destroy() {
        for(CLKernel k: kernels.values()) {
            k.destroy();
        }
        kernels.clear();
        kernels = null;

        memoryObjects.values().forEach(CLMemoryObject::delete);

        memoryObjects.clear();
        memoryObjects = null;

        CLManager.freeCommandQueueInternal(commandQueue);
        commandQueue = 0;

        CLManager.destroyContextInternal(this);
        this.context = 0;
    }

    public void getMemoryObjectValue(String id, ByteBuffer destination) {
        getMemoryObject(id).getValue(destination);
    }

    public CLMemoryObject getMemoryObject(String id) {
        assert memoryObjects.containsKey(id);
        return memoryObjects.get(id);
    }

    public void addMemoryObject(String id, CLMemoryObject memoryObject) {
        assert !memoryObjects.containsKey(id);
        memoryObjects.put(id, memoryObject);
    }

    public long getContext() {
        return context;
    }

    public long getDevice() {
        return device;
    }

    public long getCommandQueue() {
        return commandQueue;
    }

    public void addKernelObject(String id, CLKernel kernel) {
        assert !kernels.containsKey(id);
        kernels.put(id, kernel);
    }

    public CLKernel getKernelObject(String id) {
        assert kernels.containsKey(id);
        return kernels.get(id);
    }

    public void destroyKernel(String name) {
        assert kernels.containsKey(name);
        CLKernel k = kernels.remove(name);
        k.destroy();
    }

    public class CLMemoryObject {
        private long pointer;
        //In bytes
        //If size is -1, it indicates that the memory object should not be read to the main memory
        //This is the case when
        private long size;

        public CLMemoryObject(long pointer, long size) {
            this.pointer = pointer;
            this.size = size;
        }

        public void getValue(ByteBuffer destination) {
            assert this.size == destination.remaining();
            CLManager.readMemoryInternal(commandQueue, this.pointer, destination);
        }

        public void acquireFromGL() {
            assert size == -1;
            CLManager.acquireFromGLInternal(commandQueue, pointer);
        }

        public void releaseFromGL() {
            assert size == -1;
            CLManager.releaseFromGLInternal(commandQueue, pointer);
        }

        public void delete() {
            assert false: "TODO";
        }

        public long getPointer() {
            return pointer;
        }

        public long getSize() {
            return size;
        }
    }

    public class CLKernel {
        private String name;
        private long program;
        private long kernel;

        public CLKernel(String name, long program, long kernel) {
            this.name = name;
            this.program = program;
            this.kernel = kernel;
        }

        public void setParameterPointer(int index, String memoryObject) {
            long pointer = CLContext.this.getMemoryObject(memoryObject).getPointer();
            int error = CL22.clSetKernelArg1p(kernel, index, pointer);
            CLManager.checkForError(error);
        }

        public void setParameterI(int index, int value) {
            int error = CL22.clSetKernelArg1i(kernel, index, value);
            CLManager.checkForError(error);
        }

        public void run(long[] globalWorkSize, long[] localWorkSize) {
            CLManager.runKernelInternal(
                    kernel, commandQueue, globalWorkSize, localWorkSize
            );
        }

        private void destroy() {
            CLManager.destroyCLKernelInternal(program, kernel);
            this.kernel = 0;
            this.program = 0;
            name = null;
        }

        public long getKernel() {
            return kernel;
        }

        public long getProgram() {
            return program;
        }

        public String getName() {
            return name;
        }
    }
}
