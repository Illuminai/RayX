package com.rayx;

import com.rayx.examples.TestOGLWindow;
import com.rayx.glfw.OpenGLWindow;
import com.rayx.glfw.WindowManager;
import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;
import com.rayx.shape.*;

import java.util.ArrayList;

import static org.lwjgl.opencl.CL22.*;

public class RayX {
    public static final int IMG_WID = 1000, IMG_HEI = 1000;
    static CLContext context;
    static final Scene scene;
    static TorusSDF torus;
    static {
        scene = new Scene();

        int N = 5;
        for(int i = 0; i < N; i++) {
            scene.add(new SphereRTC(2.0 * i / N - 1,
                    Math.sin(1.0 * i/N * 2 * Math.PI),
                    Math.cos(1.0 * i/N * 2 * Math.PI),.01));
        }

        scene.add(new PlaneRTC(
                new Vector3d(0,0,-3),
                new Vector3d(0,0,3).normalized()));
        scene.add(new PlaneRTC(
                new Vector3d(0,0,3),
                new Vector3d(0,0,-3).normalized()));
        scene.add(new PlaneRTC(
                new Vector3d(0,-3,0),
                new Vector3d(0,3,0).normalized()));
        scene.add(new PlaneRTC(
                new Vector3d(0,3,0),
                new Vector3d(0,-3,0).normalized()));
        scene.add(new PlaneRTC(
                new Vector3d(3,0,0),
                new Vector3d(-3,0,0).normalized()));
        scene.add(new PlaneRTC(
                new Vector3d(-3,0,0),
                new Vector3d(3,0,0).normalized()));

        scene.add(torus = new TorusSDF(
                new Vector3d(1, 1, 1),
                new Vector3d(1, 1, 1),.1,.5));
        scene.add(new SubtractionSDF(
                new Vector3d(0,0,0),
                new TorusSDF( new Vector3d(0,0,2),
                    new Vector3d(0, 0,Math.PI),.1,.5),
                new TorusSDF( new Vector3d(0,0,2),
                    new Vector3d(0, 0,Math.PI),.1,.5)));
    }

    static double t = 0;

    public static void main(String[] args) {
        WindowManager manager = WindowManager.getInstance();

        TestOGLWindow window = new TestOGLWindow(1000, 1000, "Test");
        manager.addWindow(window);

        manager.setSwapInterval(0);

        loop: for(long platform: CLManager.queryPlatforms()) {
            System.out.println("-----------------------------------------------------");
            System.out.println("Name      : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_NAME));
            System.out.println("Version   : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_VERSION));
            System.out.println("Profile   : " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_PROFILE));
            System.out.println("Extensions: " + CLManager.queryPlatformInfo(platform, CL_PLATFORM_EXTENSIONS));
            System.out.println("Device(s) :");
            for(long device: CLManager.queryDevicesForPlatform(platform, window.getWindow(), false)) {
                printDevice(device, "\t");
                context = treatDevice(window, device);
                System.out.println("--------------");
                break loop;
            }
        }

        window.setCallback((texture) -> {
            scene.deleteRenderMemory(context);

            scene.render(context, texture, t == 0);
            t += Math.PI / 50;
        });

        manager.startManager();

        freeAll();
    }

    static CLContext treatDevice(OpenGLWindow window, long device) {
        long t = System.currentTimeMillis();
        CLContext context = CLManager.createContext(device, window.getWindow());

        System.out.println("Context: " + (System.currentTimeMillis() - t));
        t = System.currentTimeMillis();
        context.initialize();

        test(context);

        CLManager.putProgramFromFile(context, new String[]{
                        "clcode/default/headers/mandelbrot.h"
                },
                "clcode/main.cl",
                "-D ITERATIONS=200");
        CLManager.putExecutableProgram(context, new String[] {
                "clcode/default/implementation/mandelbrot.cl",
                "clcode/main.cl",
        },"testProgram");
        CLManager.putKernel(context, "testKernel",
                "testKernel", "testProgram");

        System.out.println("Kernel: " + (System.currentTimeMillis() - t));
        return context;
    }

    static void test(CLContext context) {
        System.out.println("Shape struct: " + context.getStructSize(Shape.SHAPE));
        System.out.println("Sphere struct: " + context.getStructSize(Shape.SPHERE_RTC));
        System.out.println("Torus struct: " + context.getStructSize(Shape.TORUS_SDF));
        System.out.println("Plane struct: " + context.getStructSize(Shape.PLANE_RTC));
        System.out.println("Subtraction struct: " + context.getStructSize(Shape.SUBTRACTION_SDF));
    }

    static void freeAll() {
        long t = System.currentTimeMillis();

        context.destroyKernel("testKernel");
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
        for(long plat: CLManager.queryPlatforms()) {
            System.out.println("\t" + plat);
        }
        for(long device: CLManager.getDevices()) {
            printDevice(device, "\t");
        }
    }
}
