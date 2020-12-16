package com.rayx.opencl;

import org.lwjgl.opencl.CL22;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class CLContext {
    private final long device;
    private long context;
    private long commandQueue;
    private HashMap<String, CLKernel> kernels;
    private HashMap<String, CLProgram> programs;
    private HashMap<String, CLMemoryObject> memoryObjects;

    /** Only {@link CLManager} should create instances */
    CLContext(long device, long context, long commandQueue) {
        this.context = context;
        this.device = device;
        this.commandQueue = commandQueue;
        kernels = new HashMap<>();
        programs = new HashMap<>();
        memoryObjects = new HashMap<>();
    }

    public void freeMemoryObject(String id) {
        long p = getMemoryObject(id).getPointer();
        CLManager.freeMemoryInternal(p);
        memoryObjects.remove(id);
    }

    public void destroy() {
        kernels.values().forEach(CLKernel::destroy);
        kernels.clear();
        kernels = null;

        programs.values().forEach(CLProgram::destroy);
        programs.clear();
        programs = null;

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

    public CLProgram getProgramObject(String id) {
        assert programs.containsKey(id);
        return programs.get(id);
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

    public void destroyProgram(String name) {
        assert kernels.values().stream().
                noneMatch(k -> name.equals(k.getProgramId())) : "Kernels depend on program \"" + name + "\"";
        assert kernels.containsKey(name);
        kernels.remove(name).destroy();
    }

    public void addKernelObject(CLKernel kernel) {
        assert !kernels.containsKey(kernel.kernelId);
        kernels.put(kernel.kernelId, kernel);
    }

    public void addProgramObject(CLProgram program) {
        assert !programs.containsKey(program.programId);
        programs.put(program.programId, program);
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
        private final long size;

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

    public class CLProgram {
        private final String programId;
        private long program;
        /** A kernel can only be created from a program if <code>linked</code> is set to <code>true</code>*/
        private final boolean linked;

        /** @param programId Name of the program.
         *                    Should be name of the file it was generated from,
         *                    so that it may used for include directives in OpenCL C*/
        public CLProgram(String programId, long program, boolean linked) {
            this.programId = programId;
            this.program = program;
            this.linked = linked;
        }

        public String getProgramId() {
            return programId;
        }

        public long getProgram() {
            return program;
        }

        private void destroy() {
            CLManager.destroyCLProgramInternal(program);
            program = 0;
        }

        public boolean isLinked() {
            return linked;
        }
    }

    public class CLKernel {
        private final String kernelId, programId;
        private long kernel;

        public CLKernel(String kernelId, String programId, long kernel) {
            this.kernelId = kernelId;
            this.programId = programId;
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
            CLManager.destroyCLKernelInternal(kernel);
            this.kernel = 0;
        }

        public long getKernel() {
            return kernel;
        }

        public String getProgramId() {
            return programId;
        }

        public String getKernelId() {
            return kernelId;
        }
    }
}
