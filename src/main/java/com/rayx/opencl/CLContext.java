package com.rayx.opencl;

import com.rayx.scene.shape.Shape;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;

public class CLContext {
    public static final String KERNEL_GET_STRUCT_SIZE = "defaultKernelGetStructSize",
            KERNEL_FILL_BUFFER_DATA = "defaultKernelFillDataBuffer",
            KERNEL_RENDER = "defaultKernelRender",
            KERNEL_RENDER_DEBUG = "defaultKernelRenderDebug",
            KERNEL_BENCHMARK_DOUBLE = "defaultKernelBenchmarkDouble",
            KERNEL_BENCHMARK_FLOAT = "defaultKernelBenchmarkFloat";

    public static final String COMPILE_OPTIONS =
            " -cl-fast-relaxed-math " +
                    " -cl-kernel-arg-info " +
                    " -Werror " +
                    " -D EPSILON=((float)0.00001) " +
                    " -D FLAG_SHOULD_RENDER=" + Shape.FLAG_SHOULD_RENDER +
                    " -D SHAPE=" + Shape.SHAPE +
                    " -D SPHERE=" + Shape.SPHERE +
                    " -D TORUS=" + Shape.TORUS +
                    " -D PLANE=" + Shape.PLANE +
                    " -D SUBTRACTION=" + Shape.SUBTRACTION +
                    " -D BOX=" + Shape.BOX +
                    " -D UNION=" + Shape.UNION +
                    " -D INTERSECTION=" + Shape.INTERSECTION +
                    " -D OCTAHEDRON=" + Shape.OCTAHEDRON +
                    " -D MATERIAL_REFLECTION=" + Material.MATERIAL_REFLECTION +
                    " -D MATERIAL_REFRACTION=" + Material.MATERIAL_REFRACTION;

    private final long device;
    private long context;
    private long commandQueue;
    private HashMap<String, CLKernel> kernels;
    private HashMap<String, CLProgram> programs;
    private HashMap<String, CLMemoryObject> memoryObjects;
    private HashMap<Integer, Integer> structSizes;

    /**
     * Only {@link CLManager} should create instances
     */
    CLContext(long device, long context, long commandQueue) {
        this.context = context;
        this.device = device;
        this.commandQueue = commandQueue;
        kernels = new HashMap<>();
        programs = new HashMap<>();
        memoryObjects = new HashMap<>();
        structSizes = new HashMap<>();
    }

    public void freeMemoryObject(String id) {
        CLMemoryObject p = getMemoryObject(id);
        p.delete();
        memoryObjects.remove(id);
    }

    public void freeAllMemoryObjects() {
        memoryObjects.values().forEach(CLMemoryObject::delete);
        memoryObjects.clear();
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
        assert programs.containsKey(id) : "No program object: " + id;
        return programs.get(id);
    }

    public CLMemoryObject getMemoryObject(String id) {
        assert memoryObjects.containsKey(id) : "No memory object: " + id;
        return memoryObjects.get(id);
    }

    public void addMemoryObject(String id, CLMemoryObject memoryObject) {
        assert !memoryObjects.containsKey(id) : "Memory object already exists: " + id;
        memoryObjects.put(id, memoryObject);
    }

    public Map<String, CLMemoryObject> getAllMemoryObjects() {
        return memoryObjects;
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
        assert !kernels.containsKey(kernel.kernelId) : kernel.getKernelId();
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

    public int getStructSize(int name) {
        assert structSizes.containsKey(name) : "No struct: " + name;
        return structSizes.get(name);
    }

    public void initialize() {
        createBenchmarks();

        //From least dependent to most dependent
        //----------- H E A D E R S -----------
        //math.h
        CLManager.putProgramFromFile(this, null,
                "clcode/default/headers/math.h",
                COMPILE_OPTIONS);
        //shapes.h
        CLManager.putProgramFromFile(this,
                new String[]{
                        "clcode/default/headers/math.h"
                },
                "clcode/default/headers/shapes.h",
                COMPILE_OPTIONS);
        //java_to_cl.h
        CLManager.putProgramFromFile(this,
                new String[]{"clcode/default/headers/shapes.h",
                        "clcode/default/headers/math.h"},
                "clcode/default/headers/java_to_cl.h",
                COMPILE_OPTIONS);
        //render.h
        CLManager.putProgramFromFile(this,
                new String[]{"clcode/default/headers/shapes.h",
                        "clcode/default/headers/math.h"},
                "clcode/default/headers/render.h",
                COMPILE_OPTIONS);

        //----------- C O D E -----------
        //math.cl
        CLManager.putProgramFromFile(this, new String[]{
                        "clcode/default/headers/math.h"
                },
                "clcode/default/implementation/math.cl",
                COMPILE_OPTIONS);
        //shapes.cl
        CLManager.putProgramFromFile(this,
                new String[]{"clcode/default/headers/shapes.h",
                        "clcode/default/headers/math.h"},
                "clcode/default/implementation/shapes.cl",
                COMPILE_OPTIONS);

        //java_to_cl.cl
        CLManager.putProgramFromFile(this,
                new String[]{
                        "clcode/default/headers/math.h",
                        "clcode/default/headers/java_to_cl.h",
                        "clcode/default/headers/shapes.h"},
                "clcode/default/implementation/java_to_cl.cl",
                COMPILE_OPTIONS);
        //java_to_cl.cl
        CLManager.putProgramFromFile(this,
                new String[]{"clcode/default/headers/render.h",
                        "clcode/default/headers/shapes.h",
                        "clcode/default/headers/math.h"},
                "clcode/default/implementation/render.cl",
                COMPILE_OPTIONS);

        //----------- E X E C U T A B L E - P R O G R A M S -----------
        CLManager.putExecutableProgram(this,
                new String[]{
                        "clcode/default/implementation/shapes.cl",
                        "clcode/default/implementation/java_to_cl.cl",
                        "clcode/default/implementation/math.cl"
                },
                "javaToCLProgram");
        CLManager.putExecutableProgram(this,
                new String[]{
                        "clcode/default/implementation/shapes.cl",
                        "clcode/default/implementation/math.cl",
                        "clcode/default/implementation/render.cl"
                },
                "renderProgram");
        //----------- K E R N E L S -----------
        //Query struct sizes in opencl
        CLManager.putKernel(this, "getShapeSizes",
                KERNEL_GET_STRUCT_SIZE, "javaToCLProgram");

        //Transfer data from RAM to VRAM
        CLManager.putKernel(this, "putShapesInMemory",
                KERNEL_FILL_BUFFER_DATA, "javaToCLProgram");
        //Run renderer
        CLManager.putKernel(this, "render",
                KERNEL_RENDER, "renderProgram");

        // Debug preview
        CLManager.putKernel(this, "renderDebug",
                KERNEL_RENDER_DEBUG, "renderProgram");
        //----------- I N I T I A L I Z E -----------
        getStructSizes();
    }

    public void createBenchmarks() {
        CLManager.putProgramFromFile(this, null,
                "clcode/default/headers/benchmark.h",
                COMPILE_OPTIONS);
        CLManager.putProgramFromFile(this,
                new String[]{"clcode/default/headers/benchmark.h"},
                "clcode/default/implementation/benchmark.cl",
                COMPILE_OPTIONS);

        CLManager.putExecutableProgram(this,
                new String[]{
                        "clcode/default/implementation/benchmark.cl"
                },
                "benchmarkProgram");

        CLManager.putKernel(this, "testPerformanceDouble",
                KERNEL_BENCHMARK_DOUBLE, "benchmarkProgram");
        CLManager.putKernel(this, "testPerformanceFloat",
                KERNEL_BENCHMARK_FLOAT, "benchmarkProgram");
    }

    public void runBenchmarks() {
        System.out.println("Running benchmarks...");
        final int amount = 20000;

        double[] vals = new Random(1).doubles(4 * amount).toArray();

        float[] f = new float[vals.length];
        for (int i = 0; i < f.length; i++) {
            f[i] = (float) vals[i];
        }

        CLManager.allocateMemory(this, CL_MEM_READ_ONLY, vals, "benchmarkDoubleIn");
        CLManager.allocateMemory(this, CL_MEM_READ_ONLY, f, "benchmarkFloatIn");
        CLManager.allocateMemory(this, CL_MEM_WRITE_ONLY,
                amount * Double.BYTES, "benchmarkDoubleOut");
        CLManager.allocateMemory(this, CL_MEM_WRITE_ONLY,
                amount * Float.BYTES, "benchmarkFloatOut");

        CLKernel dKern = getKernelObject(KERNEL_BENCHMARK_DOUBLE);
        dKern.setParameterPointer(0, "benchmarkDoubleIn");
        dKern.setParameterPointer(1, "benchmarkDoubleOut");
        dKern.setParameter1i(2, amount);

        CLKernel fKern = getKernelObject(KERNEL_BENCHMARK_FLOAT);
        fKern.setParameterPointer(0, "benchmarkFloatIn");
        fKern.setParameterPointer(1, "benchmarkFloatOut");
        fKern.setParameter1i(2, amount);

        dKern.run(new long[]{1}, null);
        fKern.run(new long[]{1}, null);

        long t = System.currentTimeMillis();
        dKern.run(new long[]{amount}, null);
        System.out.println("Double: " + (System.currentTimeMillis() - t));
        /*{
            ByteBuffer dbuffer =
                    BufferUtils.createByteBuffer(amount * Double.BYTES);
            this.getMemoryObject("benchmarkDoubleOut").getValue(dbuffer);
            double[] dres = new double[amount];
            System.out.println("Double input : " + Arrays.toString(f));
            dbuffer.asDoubleBuffer().get(dres);
            System.out.println("Double result: " + Arrays.toString(dres));
        }*/

        t = System.currentTimeMillis();
        fKern.run(new long[]{amount}, null);
        System.out.println("Float : " + (System.currentTimeMillis() - t));
        /*{
            ByteBuffer fbuffer =
                    BufferUtils.createByteBuffer(amount * Float.BYTES);
            this.getMemoryObject("benchmarkFloatOut").getValue(fbuffer);
            float[] fres = new float[amount];
            System.out.println("Float input : " + Arrays.toString(f));
            fbuffer.asFloatBuffer().get(fres);
            System.out.println("Float result: " + Arrays.toString(fres));
        }*/

        freeMemoryObject("benchmarkDoubleIn");
        freeMemoryObject("benchmarkDoubleOut");
        freeMemoryObject("benchmarkFloatIn");
        freeMemoryObject("benchmarkFloatOut");
    }

    private void getStructSizes() {
        int[] structs = {
                Shape.SHAPE,
                Shape.SPHERE,
                Shape.TORUS,
                Shape.PLANE,
                Shape.SUBTRACTION,
                Shape.BOX,
                Shape.UNION,
                Shape.INTERSECTION,
                Shape.OCTAHEDRON
        };
        CLContext.CLKernel kernelStruct =
                getKernelObject(CLContext.KERNEL_GET_STRUCT_SIZE);
        CLManager.allocateMemory(this, CL_MEM_READ_ONLY,
                structs,
                "shapesInQuestion");
        CLManager.allocateMemory(this, CL_MEM_WRITE_ONLY,
                Integer.BYTES * structs.length,
                "result");
        kernelStruct.setParameter1i(0, structs.length);
        kernelStruct.setParameterPointer(
                1, "shapesInQuestion");
        kernelStruct.setParameterPointer(
                2, "result");

        kernelStruct.run(new long[]{1}, null);

        //TODO use MemoryStack instead of BufferUtils
        ByteBuffer buffer = BufferUtils.createByteBuffer(Integer.BYTES * structs.length);
        getMemoryObject("result").getValue(buffer);

        for (int struct : structs) {
            this.structSizes.put(struct, buffer.getInt());
        }

        assert !this.structSizes.containsValue(-1) :
                "Struct with size -1. The kernel does not recognize a struct";

        freeMemoryObject("result");
        freeMemoryObject("shapesInQuestion");
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
            assert this.size == destination.remaining() : size +
                    " " + destination.remaining();
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

        public void releaseFromGL(PointerBuffer eventWaitList, PointerBuffer event) {
            assert size == -1;
            CLManager.releaseFromGLInternal(commandQueue, pointer, eventWaitList, event);
        }

        private void delete() {
            CLManager.freeMemoryInternal(pointer);
            pointer = 0;
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
        /**
         * A kernel can only be created from a program if <code>linked</code> is set to <code>true</code>
         */
        private final boolean linked;

        /**
         * @param programId Name of the program.
         *                  Should be name of the file it was generated from,
         *                  so that it may used for include directives in OpenCL C
         */
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
            CLManager.setParameterPointerInternal(CLContext.this, kernel, index, memoryObject);
        }

        public void setParameter1i(int index, int value) {
            CLManager.setParameter1iInternal(kernel, index, value);
        }

        public void setParameter4f(int index, float d0, float d1, float d2, float d3) {
            CLManager.setParameter4fInternal(kernel, index, d0, d1, d2, d3);
        }

        public void setParameter1f(int index, float d0) {
            CLManager.setParameter1fInternal(kernel, index, d0);
        }

        public void run(long[] globalWorkSize, long[] localWorkSize) {
            CLManager.runKernelInternal(
                    kernel, commandQueue, globalWorkSize, localWorkSize
            );
        }

        public long enqueue(long[] globalWorkSize, long[] localWorkSize, PointerBuffer eventWaitList) {
            return CLManager.enqueueKernelInternal(
                    kernel, commandQueue, globalWorkSize, localWorkSize, eventWaitList
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
