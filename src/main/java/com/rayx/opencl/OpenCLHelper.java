package com.rayx.opencl;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL22;
import org.lwjgl.opengl.CGL;
import org.lwjgl.opengl.GLX14;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.Platform;
import org.lwjgl.system.linux.X11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.opencl.CL22.*;
import static org.lwjgl.opencl.KHRGLSharing.*;

public class OpenCLHelper {
    /*
    public static long createProgramFromString(long context, String kernelSource) {
        IntBuffer error = BufferUtils.createIntBuffer(1);

        long program = CL22.clCreateProgramWithSource(context, kernelSource, error);
        //https://www.codeproject.com/articles/86551/part-1-programming-your-graphics-card-gpu-with-jav
        OpenCLHelper.check(error);
        return program;
    }


    public static void buildProgram(long device, long program) {
        int error = CL22.clBuildProgram(program, null,
                "", null, 0);

        if (error != CL22.CL_SUCCESS) {
            OpenCLHelper.printBuildError(program, device);
            throw new RuntimeException("OpenCL error: " + error);
        }
    }

    public static long createKernel(long program, String kernelName) {
        IntBuffer berr = BufferUtils.createIntBuffer(1);
        long kernel = CL22.clCreateKernel(program, kernelName, berr);
        assert berr.get(0) == CL22.CL_SUCCESS : berr.get(0);
        return kernel;
    }

    public static ArrayList<Long> getAllAvailableDevices() {
        ArrayList<Long> ret = new ArrayList<>();
        IntBuffer amount = BufferUtils.createIntBuffer(1);
        int error = CL22.clGetPlatformIDs(null, amount);
        check(error);

        PointerBuffer platforms = PointerBuffer.allocateDirect(amount.get());
        CL22.clGetPlatformIDs(platforms, (IntBuffer) null);
        check(error);

        while (platforms.hasRemaining()) {
            ret.addAll(getDevicesFromPlatform(platforms.get()));
        }

        return ret;
    }

    private static ArrayList<Long> getDevicesFromPlatform(long platform) {
        ArrayList<Long> ret = new ArrayList<>();

        int error;
        int[] amount = new int[]{-1};
        error = CL22.clGetDeviceIDs(platform, CL22.CL_DEVICE_TYPE_ALL, null, amount);
        check(error);

        PointerBuffer devices = PointerBuffer.allocateDirect(amount[0]);
        error = CL22.clGetDeviceIDs(platform, CL22.CL_DEVICE_TYPE_ALL, devices, (int[]) null);
        check(error);

        while (devices.hasRemaining()) {
            ret.add(devices.get());
        }

        return ret;
    }

    public static void printBuildError(long program, long device) {
        ByteBuffer b = BufferUtils.createByteBuffer(Long.BYTES);
        int error;
        error = CL22.clGetProgramBuildInfo(program, device, CL22.CL_PROGRAM_BUILD_LOG, (IntBuffer) null,
                PointerBuffer.create(b));
        assert error == CL22.CL_SUCCESS : error;
        int length = b.asIntBuffer().get();

        ByteBuffer message = BufferUtils.createByteBuffer(length);
        error = CL22.clGetProgramBuildInfo(program, device, CL22.CL_PROGRAM_BUILD_LOG, message, null);
        assert error == CL22.CL_SUCCESS : error;

        while (message.hasRemaining()) {
            System.out.print((char) message.get());
        }
    }

    public static void createCLGLContext(CLDevice device) {
        String extensions = (String) queryDeviceInfo(device.getDevice(), CL_DEVICE_EXTENSIONS);
        if (!extensions.contains("cl_khr_gl_sharing")) {
            throw new RuntimeException("No GL-CL sharing supported");
        }
        long[] parameters = switch (Platform.get()) {
            case LINUX -> new long[]{
                    CL_GL_CONTEXT_KHR,
                    GLX14.glXGetCurrentContext(),
                    CL_GLX_DISPLAY_KHR,
                    GLX14.glXGetCurrentDisplay(),
                    CL_CONTEXT_PLATFORM,
                    OpenCLHelper.getDevicePlatform(device.getDevice()),
            };
            case MACOSX -> new long[]{
                    CL_CGL_SHAREGROUP_KHR,
                    CGL.CGLGetShareGroup(CGL.CGLGetCurrentContext()),
                    CL_CONTEXT_PLATFORM,
                    OpenCLHelper.getDevicePlatform(device.getDevice()),
            };
            case WINDOWS -> new long[]{
                    CL_GL_CONTEXT_KHR,
                    WGL.wglGetCurrentContext(),
                    CL_GLX_DISPLAY_KHR,
                    WGL.wglGetCurrentDC(),
                    CL_CONTEXT_PLATFORM,
                    OpenCLHelper.getDevicePlatform(device.getDevice()),
            };
            default -> throw new RuntimeException("Unknown type");
        };

        createContext(device, parameters);
    }

    public static void createContext(CLDevice device, long[] properties) {
        PointerBuffer pDevice = PointerBuffer.allocateDirect(1);
        pDevice.put(device.getDevice());
        IntBuffer error = BufferUtils.createIntBuffer(1);
        //TODO: No idea how to use properties, but must be present for program
        ByteBuffer propertiesParam = BufferUtils.createByteBuffer((properties.length + 1) * Long.BYTES);
        LongBuffer tmp = propertiesParam.asLongBuffer();
        tmp.put(properties);
        tmp.position(0);
        device.setContext(CL22.clCreateContext(PointerBuffer.create(propertiesParam), device.getDevice(), null, 0, error));
        OpenCLHelper.check(error);
    }

     */
}
