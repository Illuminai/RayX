package com.rayx.opencl;

import com.rayx.RayX;
import com.rayx.opencl.exceptions.*;
import com.rayx.shape.Shape;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL12GL;
import org.lwjgl.opencl.CL22;
import org.lwjgl.opencl.KHRGLSharing;
import org.lwjgl.opengl.CGL;
import org.lwjgl.opengl.GLX14;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext;
import static org.lwjgl.opencl.CL22.*;
import static org.lwjgl.opencl.KHRGLSharing.*;
import static org.lwjgl.opencl.KHRGLSharing.CL_GLX_DISPLAY_KHR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.WGL.wglGetCurrentContext;
import static org.lwjgl.opengl.WGL.wglGetCurrentDC;

public class CLManager {
    private CLManager() {}

    /** Should only be accessed by {@link #nextStackFrame()}*/
    private static final MemoryStack internalMemoryStack;

    static {
        //TODO Better MemoryStack?
        internalMemoryStack = MemoryStack.create(1024 * 1024 * 1024);
    }

    public static String readFromFile(String file) {
        InputStream inputStream = CLManager.class.getResourceAsStream(file);
        assert inputStream != null: "Resource " + file + " not available";
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder b = new StringBuilder();
        String line;
        try {
            while((line = r.readLine()) != null) {
                b.append(line);
                b.append('\n');
            }
            r.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return b.toString();
    }

    public static MemoryStack nextStackFrame() {
        return internalMemoryStack.push();
    }

    public static CLContext createContext(long device, long glfwWindow) {
        try(MemoryStack stack = nextStackFrame()) {
            long[] propertiesPara = CLManager.createPropertiesForContext((Long)
                    CLManager.queryDeviceInfo(device, CL_DEVICE_PLATFORM), glfwWindow);
            ByteBuffer properties = stack.malloc(propertiesPara.length * Long.BYTES);
            properties.asLongBuffer().put(propertiesPara);

            IntBuffer error = stack.mallocInt(1);
            long context = CL22.clCreateContext(PointerBuffer.create(properties), device, null, 0, error);
            checkForError(ErrorType.CONTEXT_CREATION, error);

            long commandQueue = CL22.clCreateCommandQueue(context, device, 0, error);
            checkForError(ErrorType.COMMAND_QUEUE_CREATION, error);

            return new CLContext(device, context, commandQueue);
        }
    }

    /***/
    public static void putProgramFromFile(CLContext context, String[] dependenciesId, String sourceFile, String compileOptions) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            IntBuffer errorBuffer = stack.mallocInt(1);
            //Files start without slash because the source file is the id of the program
            //Because the id is also used for header includes, it cannot start with a slash
            //The slash is attached internally
            assert !sourceFile.startsWith("/") : "Files have to start without slash";
            String programSourceCode = readFromFile("/" + sourceFile);
            long programPointer = CL22.clCreateProgramWithSource(context.getContext(), programSourceCode, errorBuffer);
            checkForError(ErrorType.PROGRAM, errorBuffer);
            if(dependenciesId == null) {
                dependenciesId = new String[0];
            }
            CLContext.CLProgram[] dependencies = new CLContext.CLProgram[dependenciesId.length];
            for(int i = 0; i < dependencies.length; i++) {
                dependencies[i] = context.getProgramObject(dependenciesId[i]);
            }
            PointerBuffer inputHeaderNames, inputHeaderPrograms;
            if(dependencies.length != 0) {
                inputHeaderNames = stack.pointers(Arrays.stream(dependencies).
                        map((CLContext.CLProgram proObj) -> stack.ASCII(proObj.getProgramId())).
                        toArray(ByteBuffer[]::new));
                inputHeaderPrograms = stack.pointers(Arrays.stream(dependencies).
                        map(CLContext.CLProgram::getProgram).mapToLong(Long::longValue).toArray());
            } else {
                inputHeaderNames = null;
                inputHeaderPrograms = null;
            }
            int error= CL22.clCompileProgram(
                    programPointer,
                    stack.pointers(context.getDevice()),
                    compileOptions,
                    inputHeaderPrograms,
                    inputHeaderNames,
                    null,
                    0
            );
            String buildInfo = createBuildInfo(context.getDevice(), programPointer);
            if(buildInfo.equals("")) {
                System.out.println("Compiled " + sourceFile);
            } else {
                System.out.println("Compiling " + sourceFile);
                System.out.println(buildInfo);
            }
            checkForError(ErrorType.COMPILE, error);
            context.addProgramObject(context.new CLProgram(sourceFile, programPointer, false));
        }
    }

    public static void putExecutableProgram(CLContext context, String[] programIds, String programName) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            IntBuffer error = stack.mallocInt(1);
            long program = CL22.clLinkProgram(context.getContext(),
                    stack.pointers(context.getDevice()),
                    "",
                    stack.pointers(Arrays.stream(programIds).mapToLong(u -> context.getProgramObject(u).getProgram()).toArray()),
                    null,
                    0,
                    error);
            checkForError(ErrorType.LINK, error);

            assert program != 0;

            context.addProgramObject(context.new CLProgram(programName, program, true));
        }
    }

    public static void putKernel(CLContext context, String kernelFunction, String kernelId, String programId) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            IntBuffer error = stack.mallocInt(1);
            CLContext.CLProgram program = context.getProgramObject(programId);
            if(!program.isLinked()) {
                throw new RuntimeException("Program is not linked");
            }
            long kernel = CL22.clCreateKernel(program.getProgram(), kernelFunction, error);
            checkForError(ErrorType.KERNEL_CREATION, error);

            CLContext.CLKernel kernelObj = context.new CLKernel(kernelId, programId, kernel);
            context.addKernelObject(kernelObj);
        }
    }

    /*
    public static void putKernelAndProgramFromSource(CLContext context, String programSourceCode,
                                                     String kernelFunction, String kernelId, String programId) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            IntBuffer error = stack.mallocInt(1);
            long programPointer = CL22.clCreateProgramWithSource(context.getContext(), programSourceCode, error);
            checkForError(error);

            int programStatus = CL22.clBuildProgram(programPointer, context.getDevice(), "", null, 0);
            String buildInfo = createBuildInfo(context.getDevice(), programPointer);
            System.out.println(buildInfo);
            checkForError(programStatus);

            long kernel = CL22.clCreateKernel(programPointer, kernelFunction, error);
            checkForError(error);

            CLContext.CLProgram program = context.new CLProgram(programId, programPointer, true);
            context.addProgramObject(program);

            CLContext.CLKernel kernelObj = context.new CLKernel(kernelId, programId, kernel);
            context.addKernelObject(kernelObj);
        }
    }
    */

    static void runKernelInternal(long kernel, long commandQueue, long[] globalSize, long[] localSize) {
        assert globalSize.length > 0;
        assert localSize == null || localSize.length == localSize.length;
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            ByteBuffer globalWorkSize = stack.malloc(globalSize.length * Long.BYTES),
                    localWorkSize = stack.malloc(globalSize.length * Long.BYTES);

            globalWorkSize.asLongBuffer().put(globalSize);

            if(localSize != null) {
                localWorkSize.asLongBuffer().put(localSize);
            }

            PointerBuffer event = stack.mallocPointer(1);
            CLManager.checkForError(ErrorType.KERNEL_ENQUEUE,
                    CL22.clEnqueueNDRangeKernel(commandQueue, kernel,
                    globalSize.length, null,
                    PointerBuffer.create(globalWorkSize),
                    localSize == null ? null: PointerBuffer.create(localWorkSize),
                    null, event));
            int executionStatus = CL22.clWaitForEvents(event.get(0));
            checkForError(ErrorType.KERNEL_EXECUTION, executionStatus);
        }
    }

    private static String createBuildInfo(long device, long program) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            ByteBuffer b = stack.malloc(Long.BYTES);
            int error;
            error = CL22.clGetProgramBuildInfo(program, device, CL22.CL_PROGRAM_BUILD_LOG, (IntBuffer) null,
                    PointerBuffer.create(b));
            checkForError(ErrorType.PROGRAM, error);

            long length = b.asLongBuffer().get();

            ByteBuffer message = stack.malloc((int) length);
            error = CL22.clGetProgramBuildInfo(program, device, CL22.CL_PROGRAM_BUILD_LOG, message, null);
            checkForError(ErrorType.PROGRAM, error);

            StringBuilder builder = new StringBuilder(message.limit() - 1);
            char c;
            while ((c = (char) message.get()) != 0) {
                builder.append(c);
            }
            return builder.toString();
        }
    }

    private enum ErrorType {
        BUFFER_CREATION, COMMAND_QUEUE_CREATION, COMPILE, CONTEXT_CREATION, LINK, PROGRAM, KERNEL_CREATION, KERNEL_EXECUTION, KERNEL_ENQUEUE, QUERY_INFO, FREE, BUFFER_READ, GL_INTEROP, KERNEL_PARAMETER
    }

    public static void checkForError(ErrorType type, int error) {
        if (error != CL22.CL_SUCCESS) {
            switch (type) {
                case BUFFER_CREATION -> throw new CLBufferCreationException(error);
                case COMMAND_QUEUE_CREATION -> throw new CLCommandQueueCreationException(error);
                case COMPILE -> throw new CLCompileException(error);
                case CONTEXT_CREATION -> throw new CLContextCreationException(error);
                case LINK -> throw new CLLinkException(error);
                case PROGRAM -> throw new CLProgramException(error);
                case KERNEL_CREATION -> throw new CLKernelCreationException(error);
                case KERNEL_EXECUTION -> throw new CLKernelExecutionException(error);
                case KERNEL_ENQUEUE -> throw new CLKernelEnqueueException(error);
                case QUERY_INFO -> throw new CLQueryInfoException(error);
                case FREE -> throw new CLFreeException(error);
                case BUFFER_READ -> throw new CLBufferReadException(error);
                case GL_INTEROP -> throw new CLGLInteropException(error);
                case KERNEL_PARAMETER -> throw new CLKernelParameterException(error);
                default -> throw new RuntimeException("Unknown error type: " + type + " Error: " + error);
            }
        }
    }

    public static void checkForError(ErrorType type, int[] error) {
        checkForError(type, error[0]);
    }

    public static void checkForError(ErrorType type, IntBuffer error) {
        checkForError(type, error.get(0));
    }

    public static Object queryPlatformInfo(long platform, int info) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            int error;
            PointerBuffer length = stack.mallocPointer(1);
            error = CL22.clGetPlatformInfo(platform, info, (long[]) null, length);
            checkForError(ErrorType.QUERY_INFO, error);

            ByteBuffer rawInfo = stack.malloc((int)length.get(0));
            error = CL22.clGetPlatformInfo(platform, info, rawInfo, null);
            checkForError(ErrorType.QUERY_INFO, error);

            return switch (info) {
                case CL22.CL_PLATFORM_NAME, CL22.CL_PLATFORM_VERSION, CL22.CL_PLATFORM_VENDOR,
                        CL22.CL_PLATFORM_PROFILE, CL22.CL_PLATFORM_EXTENSIONS -> byteBufferToString(rawInfo);
                case CL22.CL_PLATFORM_HOST_TIMER_RESOLUTION -> byteBufferToULong(rawInfo);
                default -> throw new IllegalArgumentException("Unsupported platform info: "+ info);
            };
        }
    }

    public static Object queryDeviceInfo(long device, int info) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            int error;
            PointerBuffer length = stack.mallocPointer(1);
            error = CL22.clGetDeviceInfo(device, info, (long[]) null, length);
            checkForError(ErrorType.QUERY_INFO, error);

            ByteBuffer rawInfo = stack.malloc((int)length.get(0));
            error = CL22.clGetDeviceInfo(device, info, rawInfo, null);
            checkForError(ErrorType.QUERY_INFO, error);

            return switch (info) {
                case CL22.CL_DEVICE_BUILT_IN_KERNELS, CL22.CL_DEVICE_VERSION,
                        CL22.CL_DEVICE_NAME, CL22.CL_DEVICE_EXTENSIONS -> byteBufferToString(rawInfo);
                case CL22.CL_DEVICE_GLOBAL_MEM_CACHE_SIZE, CL22.CL_DEVICE_MAX_WORK_GROUP_SIZE -> byteBufferToULong(rawInfo);
                case CL22.CL_DEVICE_MAX_COMPUTE_UNITS, CL22.CL_DEVICE_MAX_CLOCK_FREQUENCY -> byteBufferToUInt(rawInfo);
                case CL22.CL_DEVICE_TYPE -> byteBufferToDeviceType(rawInfo);
                case CL22.CL_DEVICE_PLATFORM -> byteBufferToLong(rawInfo);
                default -> throw new IllegalArgumentException("Unsupported device info: "+ info);
            };
        }
    }

    private static String byteBufferToString(ByteBuffer value) {
        StringBuilder builder = new StringBuilder(value.capacity());
        for (int i = 0; i < builder.capacity(); i++) {
            builder.append((char) value.get());
        }

        return builder.toString();
    }

    private static String byteBufferToDeviceType(ByteBuffer buffer) {
        ArrayList<String> t = new ArrayList<>();
        long i = buffer.get(0);
        if ((i & CL22.CL_DEVICE_TYPE_CPU) != 0) {
            t.add("CPU");
        }
        if ((i & CL22.CL_DEVICE_TYPE_ACCELERATOR) != 0) {
            t.add("ACCELERATOR");
        }
        if ((i & CL22.CL_DEVICE_TYPE_CUSTOM) != 0) {
            t.add("CUSTOM");
        }
        if ((i & CL22.CL_DEVICE_TYPE_GPU) != 0) {
            t.add("GPU");
        }
        if ((i & CL22.CL_DEVICE_TYPE_DEFAULT) != 0) {
            t.add("DEFAULT");
        }

        return t.toString();
    }

    private static String byteBufferToUInt(ByteBuffer buffer) {
        return Integer.toUnsignedString(getReversedBuffer(buffer).asIntBuffer().get());
    }

    private static String byteBufferToULong(ByteBuffer buffer) {
        return Long.toUnsignedString(getReversedBuffer(buffer).asLongBuffer().get());
    }

    private static ByteBuffer getReversedBuffer(ByteBuffer o) {
        ByteBuffer n = ByteBuffer.allocate(o.limit());
        for (int i = 0; i < o.limit(); i++) {
            n.put(o.get(o.limit() - i - 1));
        }
        n.rewind();
        return n;
    }

    private static long byteBufferToLong(ByteBuffer buffer) {
        return buffer.asLongBuffer().get();
    }

    public static List<Long> getDevices() {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            List<Long> platforms = queryPlatforms();
            List<Long> devices = new ArrayList<>();
            platforms.forEach((p) -> {
                int error;
                int[] amount = new int[]{-1};
                error = CL22.clGetDeviceIDs(p, CL22.CL_DEVICE_TYPE_ALL, null, amount);
                checkForError(ErrorType.QUERY_INFO, error);

                PointerBuffer bufferDevices = stack.mallocPointer(amount[0]);
                error = CL22.clGetDeviceIDs(p, CL22.CL_DEVICE_TYPE_ALL, bufferDevices, (int[]) null);
                checkForError(ErrorType.QUERY_INFO, error);

                while (bufferDevices.hasRemaining()) {
                    devices.add(bufferDevices.get());
                }
            });
            return devices;
        }
    }

    public static List<Long> queryPlatforms() {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            IntBuffer amount = stack.mallocInt(1);
            int error = CL22.clGetPlatformIDs(null, amount);
            checkForError(ErrorType.QUERY_INFO, error);

            PointerBuffer bufferPlatforms = stack.mallocPointer(amount.get(0));
            error = CL22.clGetPlatformIDs(bufferPlatforms, (IntBuffer) null);
            checkForError(ErrorType.QUERY_INFO, error);
            ArrayList<Long> platforms = new ArrayList<>(bufferPlatforms.remaining());
            while(bufferPlatforms.hasRemaining()) {
                platforms.add(bufferPlatforms.get());
            }
            return platforms;
        }
    }

    public static long[] createPropertiesForContext(long platform, long glfwWindow) {
        if(glfwWindow == 0) {
            return new long[]{
                    CL_CONTEXT_PLATFORM, platform,
                    0
            };
        }
        return switch (Platform.get()) {
            case LINUX -> new long[]{
                    CL_CONTEXT_PLATFORM, platform,
                    CL_GL_CONTEXT_KHR, glfwGetGLXContext(glfwWindow),
                    CL_GLX_DISPLAY_KHR, GLX14.glXGetCurrentDisplay(),
                    0
            };
            case MACOSX -> new long[]{
                    CL_CGL_SHAREGROUP_KHR, CGL.CGLGetShareGroup(CGL.CGLGetCurrentContext()),
                    CL_CONTEXT_PLATFORM, platform,
                    0
            };
            case WINDOWS -> new long[]{
                    CL_CONTEXT_PLATFORM, platform,
                    CL_GL_CONTEXT_KHR, wglGetCurrentContext(),
                    CL_WGL_HDC_KHR, wglGetCurrentDC(),
                    0
            };
        };
    }

    /** @param platform must point to a valid platform
     * @param glfwWindow if not zero, the program attempts to query CL-GL interop capable devices. This only works if the platform supports the cl_khr_gl_sharing extension.
     *                   if it is zero, the program will query all available devices
     * @param onlyCurrentGLDevice Only used if CL-GL interop is enabled. When <code>true</code>, the program returns the device which is already used for the GL context.
     * @return A list containing all devices of a platform. If <code>glfwWindow</code> is not zero, the devices are CL-GL interop capable.
     * */
    public static List<Long> queryDevicesForPlatform(long platform, long glfwWindow, boolean onlyCurrentGLDevice) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            LongBuffer devices;
            long[] properties = createPropertiesForContext(platform, glfwWindow);
            ByteBuffer byteProp = stack.malloc(properties.length * Long.BYTES);
            byteProp.asLongBuffer().put(properties);
            if(glfwWindow == 0) {
                //No CL-GL interop
                int[] numDevices = new int[1];
                int error = CL22.clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, null, numDevices);
                checkForError(ErrorType.QUERY_INFO, error);
                ByteBuffer b = stack.malloc(numDevices[0] * Long.BYTES);

                error = CL22.clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, PointerBuffer.create(b), (int[]) null);
                checkForError(ErrorType.QUERY_INFO, error);
                devices = b.asLongBuffer();
            } else {
                //Check extension:
                if(!((String)queryPlatformInfo(platform, CL_PLATFORM_EXTENSIONS)).contains("cl_khr_gl_sharing")) {
                    //Return empty arraylist: operation not supported
                    //clGetGLContextInfoKHR crashes the JVM if this extension is not supported
                    return new ArrayList<>(0);
                } else {
                    ByteBuffer bytes = stack.malloc(Long.BYTES);
                    int error = KHRGLSharing.clGetGLContextInfoKHR(PointerBuffer.create(byteProp),
                            onlyCurrentGLDevice ? CL_CURRENT_DEVICE_FOR_GL_CONTEXT_KHR : CL_DEVICES_FOR_GL_CONTEXT_KHR, (ByteBuffer) null, PointerBuffer.create(bytes));
                    checkForError(ErrorType.QUERY_INFO, error);

                    ByteBuffer value = stack.malloc((int) bytes.asLongBuffer().get(0));
                    error = KHRGLSharing.clGetGLContextInfoKHR(PointerBuffer.create(byteProp), onlyCurrentGLDevice ? CL_CURRENT_DEVICE_FOR_GL_CONTEXT_KHR :
                            CL_DEVICES_FOR_GL_CONTEXT_KHR, value, null);
                    checkForError(ErrorType.QUERY_INFO, error);
                    devices = value.asLongBuffer();
                }
            }

            ArrayList<Long> ret = new ArrayList<>(devices.remaining());
            while(devices.hasRemaining()) {
                ret.add(devices.get());
            }

            return ret;
        }
    }

    public static void createCLFromGLTexture(CLContext context, long flags, int textureType, int texture, String id) {
        int[] error = new int[1];
        long pointer = CL12GL.clCreateFromGLTexture(context.getContext(), flags, textureType,
                0, texture, error);
        checkForError(ErrorType.BUFFER_CREATION, error);
        context.addMemoryObject(id, context.new CLMemoryObject(pointer, -1));
    }

    public static void allocateMemory(CLContext context, long flags, ByteBuffer src, String id) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            IntBuffer error = stack.mallocInt(1);
            long memory = CL22.clCreateBuffer(
                    context.getContext(),
                    flags | CL_MEM_COPY_HOST_PTR,
                    src,
                    error);
            checkForError(ErrorType.BUFFER_CREATION, error);
            context.addMemoryObject(id, context.new CLMemoryObject(memory, src.remaining()));
        }
    }

    /** This function prints the shapes which are allocated on the GPU
     * Only for debugging purposes*/
    public static void testPrintGPUMemory(CLContext context, String shapesIdentifier,
                                          String shapeDataPrefix, List<Shape> testReferences) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            ByteBuffer shapes = stack.malloc(
                    context.getStructSize(Shape.SHAPE) * testReferences.size());
            context.getMemoryObject(shapesIdentifier).getValue(shapes);
            {
                System.out.println("ShapeData: ");
                CLContext.CLMemoryObject pureShapes =
                        context.getMemoryObject(shapesIdentifier);
                ByteBuffer shapesData = stack.malloc((int) pureShapes.getSize());
                pureShapes.getValue(shapesData);
                int structSize = context.getStructSize(Shape.SHAPE);
                System.out.println(structSize);
                int k = 0;
                while(shapesData.hasRemaining()) {
                    System.out.println( shapesData.getLong() + " " +
                            shapesData.getLong() + " " +
                            shapesData.getLong() + " " +
                            shapesData.getFloat() + " " +
                            shapesData.getFloat() + " " +
                            shapesData.getFloat() + " " +
                            shapesData.getFloat() + " " +
                            shapesData.getLong() + " " +
                            shapesData.getLong() + " " +
                            shapesData.getLong() + " " +
                            shapesData.getLong() + " " +
                            shapesData.getLong() + " ");
                    k++;
                    shapesData.position(k * structSize);
                }
            }
            {
                System.out.println("SphereRTC: ");
                CLContext.CLMemoryObject shapeDataSphere =
                        context.getMemoryObject(shapeDataPrefix + "SphereRTC");
                ByteBuffer shapesData = stack.malloc((int) shapeDataSphere.getSize());
                shapeDataSphere.getValue(shapesData);
                int structSize = context.getStructSize(Shape.SPHERE_RTC);
                while(shapesData.hasRemaining()) {
                    for(int j = 0; j < structSize / Float.BYTES; j++) {
                        System.out.print(shapesData.getFloat() +" ");
                    }
                    System.out.println();
                }
            }
            {
                System.out.println("TorusSDF: ");
                CLContext.CLMemoryObject shapeDataTorus =
                        context.getMemoryObject(shapeDataPrefix + "TorusSDF");
                ByteBuffer shapesData = stack.malloc((int) shapeDataTorus.getSize());
                shapeDataTorus.getValue(shapesData);
                int structSize = context.getStructSize(Shape.TORUS_SDF);
                while(shapesData.hasRemaining()) {
                    for(int j = 0; j < structSize / Float.BYTES; j++) {
                        System.out.print(shapesData.getFloat() + " ");
                    }
                    System.out.println();
                }
            }
            {
                System.out.println("PlaneRTC: ");
                CLContext.CLMemoryObject planeData =
                        context.getMemoryObject(shapeDataPrefix + "PlaneRTC");
                ByteBuffer shapesData = stack.malloc((int) planeData.getSize());
                planeData.getValue(shapesData);
                int structSize = context.getStructSize(Shape.PLANE_RTC);
                while(shapesData.hasRemaining()) {
                    for(int j = 0; j < structSize / Float.BYTES; j++) {
                        System.out.print(shapesData.getFloat() + " ");
                    }
                    System.out.println();
                }
            }
            {
                System.out.println("SubtractionSDF: ");
                CLContext.CLMemoryObject subtractionData =
                        context.getMemoryObject(shapeDataPrefix + "SubtractionSDF");
                ByteBuffer shapesData = stack.malloc((int) subtractionData.getSize());
                subtractionData.getValue(shapesData);
                int structSize = context.getStructSize(Shape.SUBTRACTION_SDF);
                while(shapesData.hasRemaining()) {
                    for(int j = 0; j < structSize / Float.BYTES; j++) {
                        System.out.print(shapesData.getFloat() + " ");
                    }
                    System.out.println();
                }
            }
            {
                System.out.println("BoxSDF: ");
                CLContext.CLMemoryObject subtractionData =
                        context.getMemoryObject(shapeDataPrefix + "BoxSDF");
                ByteBuffer shapesData = stack.malloc((int) subtractionData.getSize());
                subtractionData.getValue(shapesData);
                int structSize = context.getStructSize(Shape.BOX_SDF);
                while(shapesData.hasRemaining()) {
                    for(int j = 0; j < structSize / Float.BYTES; j++) {
                        System.out.print(shapesData.getFloat() + " ");
                    }
                    System.out.println();
                }
            }
            {
                System.out.println("UnionSDF: ");
                CLContext.CLMemoryObject subtractionData =
                        context.getMemoryObject(shapeDataPrefix + "UnionSDF");
                ByteBuffer shapesData = stack.malloc((int) subtractionData.getSize());
                subtractionData.getValue(shapesData);
                int structSize = context.getStructSize(Shape.UNION_SDF);
                while(shapesData.hasRemaining()) {
                    for(int j = 0; j < structSize / Float.BYTES; j++) {
                        System.out.print(shapesData.getFloat() + " ");
                    }
                    System.out.println();
                }
            }
            {
                System.out.println("IntersectionSDF: ");
                CLContext.CLMemoryObject subtractionData =
                        context.getMemoryObject(shapeDataPrefix + "IntersectionSDF");
                ByteBuffer shapesData = stack.malloc((int) subtractionData.getSize());
                subtractionData.getValue(shapesData);
                int structSize = context.getStructSize(Shape.INTERSECTION_SDF);
                while(shapesData.hasRemaining()) {
                    for(int j = 0; j < structSize / Float.BYTES; j++) {
                        System.out.print(shapesData.getFloat() + " ");
                    }
                    System.out.println();
                }
            }
        }
    }

    public static void runRenderKernel(CLContext context, int glTex, float[] cameraPos,
                                       float[] cameraRot, float cameraFOV,
                                       int numShapes, String shapesMemObj, String shapeDataPrefix) {
        glFinish();
        CLContext.CLKernel kernel = context.getKernelObject(CLContext.KERNEL_RENDER);
        CLManager.createCLFromGLTexture(context, CL_MEM_WRITE_ONLY, GL_TEXTURE_2D, glTex, "texture");
        context.getMemoryObject("texture").acquireFromGL();
        kernel.setParameterPointer(0, "texture");
        kernel.setParameter4f(1, cameraPos[0], cameraPos[1], cameraPos[2],0);
        kernel.setParameter4f(2, cameraRot[0], cameraRot[1], cameraRot[2],0);
        kernel.setParameter1f(3, cameraFOV);
        kernel.setParameter1i(4, numShapes);
        kernel.setParameterPointer(5, shapesMemObj);

        //TODO make image size dynamic
        kernel.run(new long[]{RayX.IMG_WID, RayX.IMG_HEI}, null);
        context.getMemoryObject("texture").releaseFromGL();
        context.freeMemoryObject("texture");
    }

    public static void allocateMemory(CLContext context, long flags, long bytesToAllocate, String id) {
        assert bytesToAllocate > 0;
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            IntBuffer error = stack.mallocInt(1);
            long memory = CL22.clCreateBuffer(context.getContext(), flags, bytesToAllocate, error);
            checkForError(ErrorType.BUFFER_CREATION, error);
            context.addMemoryObject(id, context.new CLMemoryObject(memory, bytesToAllocate));
        }
    }

    public static void allocateMemory(CLContext context, long flags, int[] source, String id) {
        int[] error = new int[1];
        long memory = CL22.clCreateBuffer(context.getContext(), flags | CL_MEM_COPY_HOST_PTR, source, error);
        checkForError(ErrorType.BUFFER_CREATION, error);
        context.addMemoryObject(id, context.new CLMemoryObject(memory, (long) source.length * Integer.BYTES));
    }

    public static void allocateMemory(CLContext context, long flags, float[] source, String id) {
        int[] error = new int[1];
        long memory = CL22.clCreateBuffer(context.getContext(), flags | CL_MEM_COPY_HOST_PTR, source, error);
        checkForError(ErrorType.BUFFER_CREATION, error);
        context.addMemoryObject(id, context.new CLMemoryObject(memory, (long) source.length * Float.BYTES));
    }

    static void freeCommandQueueInternal(long queue) {
        assert queue != 0;
        checkForError(ErrorType.FREE, CL22.clReleaseCommandQueue(queue));
    }

    static void freeMemoryInternal(long pointer) {
        assert pointer != 0;
        checkForError(ErrorType.FREE, CL22.clReleaseMemObject(pointer));
    }

    static void destroyContextInternal(CLContext context) {
        assert context.getContext() != 0;
        checkForError(ErrorType.FREE, CL22.clReleaseContext(context.getContext()));
    }

    static void destroyCLProgramInternal(long program) {
        assert program != 0;
        checkForError(ErrorType.FREE, CL22.clReleaseProgram(program));
    }

    static void destroyCLKernelInternal(long kernel) {
        assert kernel != 0;
        checkForError(ErrorType.FREE, CL22.clReleaseKernel(kernel));
    }

    static void readMemoryInternal(long commandQueue, long pointer, ByteBuffer buffer) {
        int error = CL22.clEnqueueReadBuffer(commandQueue, pointer, true,
                0, buffer, null, null);
        checkForError(ErrorType.BUFFER_READ, error);
    }

    static void acquireFromGLInternal(long commandQueue, long memoryObject) {
        checkForError(ErrorType.GL_INTEROP, CL12GL.clEnqueueAcquireGLObjects(commandQueue, memoryObject, null, null));
    }

    static void releaseFromGLInternal(long commandQueue, long memoryObject) {
        checkForError(ErrorType.GL_INTEROP, CL12GL.clEnqueueReleaseGLObjects(commandQueue, memoryObject, null, null));
    }


    static void setParameterPointerInternal(CLContext context, long kernel, int index, String memoryObject) {
        long pointer = context.getMemoryObject(memoryObject).getPointer();
        int error = CL22.clSetKernelArg1p(kernel, index, pointer);
        checkForError(ErrorType.KERNEL_PARAMETER, error);
    }

    static void setParameter1iInternal(long kernel, int index, int value) {
        int error = CL22.clSetKernelArg1i(kernel, index, value);
        checkForError(ErrorType.KERNEL_PARAMETER, error);
    }

    static void setParameter4fInternal(long kernel, int index, float d0, float d1, float d2, float d3) {
        int error = CL22.clSetKernelArg4f(kernel, index, d0, d1, d2, d3);
        checkForError(ErrorType.KERNEL_PARAMETER, error);
    }

    static void setParameter1fInternal(long kernel, int index, float d0) {
        int error = CL22.clSetKernelArg1f(kernel, index, d0);
        checkForError(ErrorType.KERNEL_PARAMETER, error);
    }
}
