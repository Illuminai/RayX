package com.rayx;

import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;

import static org.lwjgl.opencl.CL22.*;
import static org.lwjgl.opengl.GL11.*;

public class RayX {
    static long window;
    static CLContext context;
    public static void main(String[] args) {
        System.out.println("Starting");
        //printAllDevicesAndPlatforms();
        TestGLFWWindow.init();

        window = TestGLFWWindow.window;

        loop: for(long platform: CLManager.queryPlatforms()) {
            System.out.println("-----------------------------------------------------");
            System.out.println("Name      : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_NAME));
            System.out.println("Version   : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_VERSION));
            System.out.println("Profile   : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_PROFILE));
            System.out.println("Extensions: " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_EXTENSIONS));
            System.out.println("Device(s) :");
            for(long device: CLManager.queryDevicesForPlatform(platform,
                    CLManager.createPropertiesForContext(platform, window), true)) {
                printDevice(device, "\t");
                context = treatDevice(device);
                System.out.println("--------------");
                break loop;
            }
        }

        TestGLFWWindow.loop((texture) -> {
            CLContext.CLKernel kernel = context.getKernelObject("test");
            CLManager.allocateMemory(context, CL_MEM_READ_ONLY, new int[]{(int) texture[1], (int) texture[2]}, "dim");
            glFinish();
            CLManager.createCLFromGLTexture(context, CL_MEM_WRITE_ONLY, GL_TEXTURE_2D, (Integer) texture[0], "tex");
            context.getMemoryObject("tex").acquireFromGL();
            kernel.setParameterPointer(0, "tex");
            kernel.setParameterPointer(1, "dim");
            kernel.run(new long[]{(int) texture[1], (int) texture[2]}, null);
            context.getMemoryObject("tex").releaseFromGL();
            context.freeMemoryObject("tex");
            context.freeMemoryObject("dim");
            System.out.println((System.currentTimeMillis() - last));
            last = System.currentTimeMillis();

        });

        freeAll();
    }

    static long last = 0;

    static final String KERNEL_SRC = """
            __kernel void asdf(__write_only image2d_t image, __constant int* dim) {
                int2 pixCo = (int2){get_global_id(0),get_global_id(1)};
                if(pixCo.x >= dim[0] || pixCo.y >= dim[1]) {
                    return;
                }
                float4 color = (float4){1,1,1,1};
                if(pixCo.x % 40 < 20){
                    color.x /= 2;
                }
                if(pixCo.y % 40 < 20){
                    color.y /= 2;
                }
                write_imagef(image, pixCo, color);
            }
            """;

    static CLContext treatDevice(long device) {
        long t = System.currentTimeMillis();
        CLContext context = CLManager.createContext(device, CLManager.createPropertiesForContext(
                (Long) CLManager.queryDeviceInfo(device, CL_DEVICE_PLATFORM), window));

        System.out.println("Context: " + (System.currentTimeMillis() - t));
        t = System.currentTimeMillis();

        CLManager.addProgramAndKernelToContext(context, KERNEL_SRC, "asdf", "test");
        System.out.println("Kernel: " + (System.currentTimeMillis() - t));
        return context;
    }

    static void freeAll() {
        long t = System.currentTimeMillis();

        context.destroyKernel("test");
        context.destroy();
        System.out.println("Destroy: " + (System.currentTimeMillis() - t));
    }

    static void printDevice(long device, String prefix) {
        System.out.println(prefix + "Device: " + CLManager.queryDeviceInfo(device, CL_DEVICE_NAME));
        System.out.println(prefix + "\tPlatform     : " +
                CLManager.queryDeviceInfo(device, CL_DEVICE_PLATFORM));
        System.out.println(prefix + "\tKernels      : " +
                CLManager.queryDeviceInfo(device, CL_DEVICE_BUILT_IN_KERNELS));
        System.out.println(prefix + "\tExtensions   : " +
                CLManager.queryDeviceInfo(device, CL_DEVICE_EXTENSIONS));
        System.out.println(prefix + "\tGlobal mem   : " +
                CLManager.queryDeviceInfo(device, CL_DEVICE_GLOBAL_MEM_CACHE_SIZE));
        System.out.println(prefix + "\tCompute Units: " +
                CLManager.queryDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS));
        System.out.println(prefix + "\tMax clock    : " +
                CLManager.queryDeviceInfo(device, CL_DEVICE_MAX_CLOCK_FREQUENCY));
        System.out.println(prefix + "\tDevice type  : " +
                CLManager.queryDeviceInfo(device, CL_DEVICE_TYPE));
    }

    static void printAllDevicesAndPlatforms() {
        System.out.println("Available Platforms:");
        for(long plat: CLManager.queryPlatforms()) {
            System.out.println("\t" + plat);
        }
        for(long device: CLManager.getDevices()) {
            printDevice(device, "\t");
        }
    }
}
