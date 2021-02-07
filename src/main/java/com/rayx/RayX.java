package com.rayx;

import com.rayx.examples.TestOGLWindow;
import com.rayx.glfw.OpenGLWindow;
import com.rayx.glfw.WindowManager;
import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;
import com.rayx.scene.Camera;
import com.rayx.scene.Scene;
import com.rayx.scene.shape.Shape;

import static org.lwjgl.opencl.CL22.*;

public class RayX {
    static CLContext context;


    static double t = 0;

    public static void main(String[] args) {
        WindowManager manager = WindowManager.getInstance();

        TestOGLWindow window = new TestOGLWindow(1600, 900, "Test");
        manager.addWindow(window);

        manager.setSwapInterval(1);

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

        window.setCallback((objs) -> {
            Scene.DemoScene scene = (Scene.DemoScene) objs[3];
            scene.deleteRenderMemory(context);

            scene.set(context, t);

            scene.render(context, (int) objs[0], (boolean) objs[5], (Camera) objs[4], (int) objs[1], (int) objs[2]);
            t += Math.PI / 50;
        });

        manager.startManager();

        freeAll();
    }

    public static CLContext treatDevice(OpenGLWindow window, long device) {
        long t = System.currentTimeMillis();
        CLContext context = CLManager.createContext(device, window.getWindow());

        System.out.println("Context: " + (System.currentTimeMillis() - t));
        t = System.currentTimeMillis();
        context.initialize();

        test(context);

        System.out.println("Kernel: " + (System.currentTimeMillis() - t));
        return context;
    }

    static void test(CLContext context) {
        System.out.println("Shape struct: " + context.getStructSize(Shape.SHAPE));
        System.out.println("Sphere struct: " + context.getStructSize(Shape.SPHERE));
        System.out.println("Torus struct: " + context.getStructSize(Shape.TORUS));
        System.out.println("Plane struct: " + context.getStructSize(Shape.PLANE));
        System.out.println("Subtraction struct: " + context.getStructSize(Shape.SUBTRACTION));
        System.out.println("Box struct: " + context.getStructSize(Shape.BOX));
        System.out.println("Union struct: " + context.getStructSize(Shape.UNION));
        System.out.println("Intersection struct: " + context.getStructSize(Shape.INTERSECTION));
    }

    static void freeAll() {
        long t = System.currentTimeMillis();
        //TODO destroy default kernels
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
        System.out.println(prefix + "\tMax Work Group Size: " +
                CLManager.queryDeviceInfo(device, CL_DEVICE_MAX_WORK_GROUP_SIZE));
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
