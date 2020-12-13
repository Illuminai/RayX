package com.rayx.opencl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWNativeGLX;
import org.lwjgl.glfw.GLFWNativeWGL;
import org.lwjgl.opencl.CL12GL;
import org.lwjgl.opencl.CL22;
import org.lwjgl.opencl.KHRGLSharing;
import org.lwjgl.opengl.CGL;
import org.lwjgl.opengl.GLX14;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.nglfwGetKeyName;
import static org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext;
import static org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext;
import static org.lwjgl.opencl.CL22.*;
import static org.lwjgl.opencl.KHRGLSharing.*;
import static org.lwjgl.opencl.KHRGLSharing.CL_GLX_DISPLAY_KHR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.WGL.wglGetCurrentContext;
import static org.lwjgl.opengl.WGL.wglGetCurrentDC;

public class CLManager {
    private CLManager() {}

    private static final MemoryStack stack;

    static {
        stack = MemoryStack.create();
    }

    public static CLContext createContext(long device, long[] propertiesPara) {
        stack.push();

        ByteBuffer properties = stack.malloc(propertiesPara.length * Long.BYTES);
        properties.asLongBuffer().put(propertiesPara);

        IntBuffer error = stack.mallocInt(1);
        long context = CL22.clCreateContext(PointerBuffer.create(properties), device, null, 0, error);
        checkForError(error);

        long commandQueue = CL22.clCreateCommandQueue(context, device, 0, error);
        checkForError(error);

        stack.pop();
        return new CLContext(device, context, commandQueue);
    }

    public static void addProgramAndKernelToContext(CLContext context, String programSourceCode,
                                                    String kernelFunction, String kernelId) {
        stack.push();
        IntBuffer error = stack.mallocInt(1);
        long program = CL22.clCreateProgramWithSource(context.getContext(), programSourceCode, error);
        checkForError(error);

        int programStatus = CL22.clBuildProgram(program, context.getDevice(), "", null, 0);
        String buildInfo = createBuildInfo(context.getDevice(), program);
        System.out.println(buildInfo);
        checkForError(programStatus);

        long kernel = CL22.clCreateKernel(program, kernelFunction, error);
        checkForError(error);

        CLContext.CLKernel kernelObj = context.new CLKernel(kernelId, program, kernel);
        context.addKernelObject(kernelId, kernelObj);

        stack.pop();
    }

    static void runKernelInternal(long kernel, long commandQueue, long[] globalSize, long[] localSize) {
        assert globalSize.length > 0;
        assert localSize == null || localSize.length == localSize.length;

        stack.push();
        ByteBuffer globalWorkSize = stack.malloc(globalSize.length * Long.BYTES),
                localWorkSize = stack.malloc(globalSize.length * Long.BYTES);

        globalWorkSize.asLongBuffer().put(globalSize);

        if(localSize != null) {
            localWorkSize.asLongBuffer().put(localSize);
        }

        CLManager.checkForError(CL22.clEnqueueNDRangeKernel(commandQueue, kernel, globalSize.length, null, PointerBuffer.create(globalWorkSize),
                localSize == null ? null: PointerBuffer.create(localWorkSize),
                null, null));

        CLManager.checkForError(CL22.clFinish(commandQueue));
        stack.pop();
    }

    private static String createBuildInfo(long device, long program){
        stack.push();
        ByteBuffer b = stack.malloc(Long.BYTES);
        int error;
        error = CL22.clGetProgramBuildInfo(program, device, CL22.CL_PROGRAM_BUILD_LOG, (IntBuffer) null,
                PointerBuffer.create(b));
        checkForError(error);

        long length = b.asLongBuffer().get();

        ByteBuffer message = stack.malloc((int) length);
        error = CL22.clGetProgramBuildInfo(program, device, CL22.CL_PROGRAM_BUILD_LOG, message, null);
        checkForError(error);

        StringBuilder builder = new StringBuilder(message.limit());
        while (message.hasRemaining()) {
            builder.append((char) message.get());
        }
        stack.pop();
        return builder.toString();
    }

    public static void checkForError(int[] error) {
        checkForError(error[0]);
    }

    public static void checkForError(int error) {
        if (error != CL22.CL_SUCCESS) {
            throw new RuntimeException("OpenCL error: " + error);
        }
    }

    public static void checkForError(IntBuffer error) {
        if (error.get(0) != CL22.CL_SUCCESS) {
            throw new RuntimeException("OpenCL error: " + error.get(0));
        }
    }

    public static Object queryPlatformInfo(long platform, int info) {
        int error;
        stack.push();
        PointerBuffer length = stack.mallocPointer(1);
        error = CL22.clGetPlatformInfo(platform, info, (long[]) null, length);
        checkForError(error);

        ByteBuffer rawInfo = stack.malloc((int)length.get(0));
        error = CL22.clGetPlatformInfo(platform, info, rawInfo, null);
        checkForError(error);

        Object ret =  switch (info) {
            case CL22.CL_PLATFORM_NAME, CL22.CL_PLATFORM_VERSION, CL22.CL_PLATFORM_VENDOR,
                    CL22.CL_PLATFORM_PROFILE, CL22.CL_PLATFORM_EXTENSIONS -> byteBufferToString(rawInfo);
            case CL22.CL_PLATFORM_HOST_TIMER_RESOLUTION -> byteBufferToULong(rawInfo);
            default -> throw new IllegalArgumentException("Unsupported platform info: "+ info);
        };
        stack.pop();
        return ret;
    }

    public static Object queryDeviceInfo(long device, int info) {
        int error;
        stack.push();
        PointerBuffer length = stack.mallocPointer(1);
        error = CL22.clGetDeviceInfo(device, info, (long[]) null, length);
        checkForError(error);

        ByteBuffer rawInfo = stack.malloc((int)length.get(0));
        error = CL22.clGetDeviceInfo(device, info, rawInfo, null);
        checkForError(error);

        Object ret =  switch (info) {
            case CL22.CL_DEVICE_BUILT_IN_KERNELS, CL22.CL_DEVICE_VERSION, CL22.CL_DEVICE_NAME, CL22.CL_DEVICE_EXTENSIONS -> byteBufferToString(rawInfo);
            case CL22.CL_DEVICE_GLOBAL_MEM_CACHE_SIZE -> byteBufferToULong(rawInfo);
            case CL22.CL_DEVICE_MAX_COMPUTE_UNITS, CL22.CL_DEVICE_MAX_CLOCK_FREQUENCY -> byteBufferToUInt(rawInfo);
            case CL22.CL_DEVICE_TYPE -> byteBufferToDeviceType(rawInfo);
            case CL22.CL_DEVICE_PLATFORM -> byteBufferToLong(rawInfo);
            default -> throw new IllegalArgumentException("Unsupported device info: "+ info);
        };
        stack.pop();
        return ret;
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
        stack.push();
        List<Long> platforms = queryPlatforms();
        List<Long> devices = new ArrayList<>();
        platforms.forEach((p) -> {
            int error;
            int[] amount = new int[]{-1};
            error = CL22.clGetDeviceIDs(p, CL22.CL_DEVICE_TYPE_ALL, null, amount);
            checkForError(error);

            PointerBuffer bufferDevices = stack.mallocPointer(amount[0]);
            error = CL22.clGetDeviceIDs(p, CL22.CL_DEVICE_TYPE_ALL, bufferDevices, (int[]) null);
            checkForError(error);

            while (bufferDevices.hasRemaining()) {
                devices.add(bufferDevices.get());
            }
        });
        stack.pop();

        return devices;
    }

    public static List<Long> queryPlatforms() {
        stack.push();
        IntBuffer amount = stack.mallocInt(1);
        int error = CL22.clGetPlatformIDs(null, amount);
        checkForError(error);

        PointerBuffer bufferPlatforms = stack.mallocPointer(amount.get(0));
        error = CL22.clGetPlatformIDs(bufferPlatforms, (IntBuffer) null);
        checkForError(error);
        ArrayList<Long> platforms = new ArrayList<>(bufferPlatforms.remaining());
        while(bufferPlatforms.hasRemaining()) {
            platforms.add(bufferPlatforms.get());
        }
        stack.pop();
        return platforms;
    }

    public static long[] createPropertiesForContext(long platform) {
        return createPropertiesForContext(platform, 0);
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
                    CL_GL_CONTEXT_KHR, glfwGetWGLContext(glfwWindow),
                    CL_WGL_HDC_KHR, wglGetCurrentDC(),
                    0
            };
        };
    }

    public static List<Long> queryDevicesForPlatform(long platform, long[] properties, boolean clGLInterop) {
        stack.push();
        LongBuffer devices;

        ByteBuffer byteProp = stack.malloc(properties.length * Long.BYTES);
        byteProp.asLongBuffer().put(properties);

        if(clGLInterop) {
            //Check extension:
            if(!((String)queryPlatformInfo(platform, CL_PLATFORM_EXTENSIONS)).contains("cl_khr_gl_sharing")) {
                //Return empty arraylist: operation not supported
                //clGetGLContextInfoKHR crashes the JVM if this extension is not supported
                return new ArrayList<>(1);
            }
            ByteBuffer bytes = stack.malloc(Long.BYTES);
            int error = KHRGLSharing.clGetGLContextInfoKHR(PointerBuffer.create(byteProp),
                    CL_DEVICES_FOR_GL_CONTEXT_KHR, (ByteBuffer) null, PointerBuffer.create(bytes));
            checkForError(error);

            ByteBuffer value = stack.malloc((int) bytes.asLongBuffer().get(0));
            error = KHRGLSharing.clGetGLContextInfoKHR(PointerBuffer.create(byteProp), CL_DEVICES_FOR_GL_CONTEXT_KHR, value, null);
            checkForError(error);
            devices = value.asLongBuffer();
        } else {
            int[] numDevices = new int[1];
            int error = CL22.clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, null, numDevices);
            checkForError(error);
            ByteBuffer b = stack.malloc(numDevices[0] * Long.BYTES);

            error = CL22.clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, PointerBuffer.create(b), (int[]) null);
            checkForError(error);
            devices = b.asLongBuffer();
        }

        ArrayList<Long> ret = new ArrayList<>();
        while(devices.hasRemaining()) {
            ret.add(devices.get());
        }

        stack.pop();
        return ret;
    }

    /** Searches all available platforms*/
    public static List<Long> queryDevicesFromAllPlatformsForGLWindow(long window) {
        ArrayList<Long> ret = new ArrayList<>();
        for(long p: queryPlatforms()) {
            ret.addAll(queryDevicesForPlatform(p, createPropertiesForContext(p, window), true));
        }
        return ret;
    }

    public static void createCLFromGLTexture(CLContext context, long flags, int textureType, int texture, String id) {
        int[] error = new int[1];
        long pointer = CL12GL.clCreateFromGLTexture(context.getContext(), flags, textureType,
                0, texture, error);
        checkForError(error);
        context.addMemoryObject(id, context.new CLMemoryObject(pointer, -1));
    }

    public static void allocateMemory(CLContext context, long flags, long bytesToAllocate, String id) {
        stack.push();
        IntBuffer error = stack.mallocInt(1);
        long memory = CL22.clCreateBuffer(context.getContext(), flags, bytesToAllocate, error);
        checkForError(error);
        context.addMemoryObject(id, context.new CLMemoryObject(memory, bytesToAllocate));
        stack.pop();
    }

    public static void allocateMemory(CLContext context, long flags, int[] source, String id) {
        int[] error = new int[1];
        long memory = CL22.clCreateBuffer(context.getContext(), flags | CL_MEM_COPY_HOST_PTR, source, error);
        checkForError(error);
        context.addMemoryObject(id, context.new CLMemoryObject(memory, (long) source.length * Integer.BYTES));
    }

    public static void allocateMemory(CLContext context, long flags, double[] source, String id) {
        int[] error = new int[1];
        long memory = CL22.clCreateBuffer(context.getContext(), flags | CL_MEM_COPY_HOST_PTR, source, error);
        checkForError(error);
        context.addMemoryObject(id, context.new CLMemoryObject(memory, (long) source.length * Double.BYTES));
    }

    static void freeCommandQueueInternal(long queue) {
        assert queue != 0;
        checkForError(CL22.clReleaseCommandQueue(queue));
    }

    static void freeMemoryInternal(long pointer) {
        assert pointer != 0;
        checkForError(CL22.clReleaseMemObject(pointer));
    }

    static void destroyContextInternal(CLContext context) {
        assert context.getContext() != 0;
        checkForError(CL22.clReleaseContext(context.getContext()));
    }

    static void destroyCLKernelInternal(long program, long kernel) {
        assert program != 0;
        assert kernel != 0;
        checkForError(CL22.clReleaseKernel(kernel));
        checkForError(CL22.clReleaseProgram(program));
    }

    static void readMemoryInternal(long commandQueue, long pointer, ByteBuffer buffer) {
        int error = CL22.clEnqueueReadBuffer(commandQueue, pointer, true,
                0, buffer, null, null);
        checkForError(error);
    }

    static void acquireFromGLInternal(long commandQueue, long memoryObject) {
        checkForError(CL12GL.clEnqueueAcquireGLObjects(commandQueue, memoryObject, null, null));
    }

    static void releaseFromGLInternal(long commandQueue, long memoryObject) {
        checkForError(CL12GL.clEnqueueReleaseGLObjects(commandQueue, memoryObject, null, null));
    }
}
