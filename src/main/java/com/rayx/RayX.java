package com.rayx;

import com.rayx.examples.TestOGLWindow;
import com.rayx.glfw.OpenGLWindow;
import com.rayx.glfw.WindowManager;
import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;

import static org.lwjgl.opencl.CL22.*;
import static org.lwjgl.opengl.GL11.*;

public class RayX {
    static CLContext context;

    public static void main(String[] args) {

        WindowManager manager = WindowManager.getInstance();

        TestOGLWindow window = new TestOGLWindow(800, 600, "Test");
        manager.addWindow(window);

        manager.setSwapInterval(0);

        loop:
        for (long platform : CLManager.queryPlatforms()) {
            System.out.println("-----------------------------------------------------");
            System.out.println("Name      : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_NAME));
            System.out.println("Version   : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_VERSION));
            System.out.println("Profile   : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_PROFILE));
            System.out.println("Extensions: " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_EXTENSIONS));
            System.out.println("Device(s) :");
            for (long device : CLManager.queryDevicesForPlatform(platform, window.getWindow(), false)) {
                printDevice(device, "\t");
                context = treatDevice(window, device);
                System.out.println("--------------");
                break loop;
            }
        }

        window.setCallback((texture) -> {
            glFinish();
            CLContext.CLKernel kernel = context.getKernelObject("test");
            CLManager.allocateMemory(context, CL_MEM_READ_ONLY, new double[]{-1.5, 0.0, .5}, "center");
            CLManager.createCLFromGLTexture(context, CL_MEM_WRITE_ONLY, GL_TEXTURE_2D, texture, "tex");
            context.getMemoryObject("tex").acquireFromGL();
            kernel.setParameterPointer(0, "tex");
            kernel.setParameterPointer(1, "center");
            kernel.run(new long[]{1000, 1000}, null);
            context.getMemoryObject("tex").releaseFromGL();
            context.freeMemoryObject("tex");
            context.freeMemoryObject("center");
        });

        manager.startManager();

        freeAll();
    }

    static final String KERNEL_SOURCE = """
            float4 color(int, int);
            int calc(double2, int);
             
            __kernel void testKernel(__write_only image2d_t image, __global double* center) {
                int2 pixCo = (int2){get_global_id(0),get_global_id(1)};
                int w = get_image_width(image);
                int h = get_image_height(image);
                if(pixCo.x >= w | pixCo.y >= h) {
                    return;
                }
                double2 absCo = (double2)
                    {center[2] * ((1.0 * pixCo.x / w) - .5) + center[0],
                     center[2] * ((1.0 * (h - pixCo.y - 1) / h) - .5) + center[1]};
                        
                write_imagef(image, pixCo, color(calc(absCo, 500), 500));
            }
                        
            int calc(double2 coo, int maxIter) {
                double a = 0, b = 0, ta, tb;
                int i = 0;
                for(;(i < maxIter) & !(a * a + b * b > 4); i++) {
                    ta = a*a - b*b + coo.x;
                    tb = 2 * a * b + coo.y;
                    a = ta;
                    b = tb;
                }
                return i == maxIter ? -1 : i;
            }
                        
            float4 color(int value, int maxIterations) {
                int color;
                if(value == -1) {
                    color = 0x0;
                } else if(value == 0) {
                    color = 0x0;
                } else {
                    color = 0xffffff * (1.0*log((double)value)/log((double)maxIterations));
                }
                
                return (float4){
                    1.0 * ((color >> 0) & 0xFF) / 0xFF,
                    1.0 * ((color >> 8) & 0xFF) / 0xFF,
                    1.0 * ((color >> 16) & 0xFF) / 0xFF, 
                    1
                };
            }
            """;

    static CLContext treatDevice(OpenGLWindow window, long device) {
        long t = System.currentTimeMillis();
        CLContext context = CLManager.createContext(device, window.getWindow());

        System.out.println("Context: " + (System.currentTimeMillis() - t));
        t = System.currentTimeMillis();

        CLManager.addProgramAndKernelToContext(context, KERNEL_SOURCE, "testKernel", "test");
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
        for (long plat : CLManager.queryPlatforms()) {
            System.out.println("\t" + plat);
        }
        for (long device : CLManager.getDevices()) {
            printDevice(device, "\t");
        }
    }
}
