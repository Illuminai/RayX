package com.rayx;

import com.rayx.core.ui.OpenGLWindow;
import com.rayx.core.ui.WindowManager;
import com.rayx.examples.TestOGLWindow;
import com.rayx.modular.plugin.Plugin;
import com.rayx.modular.plugin.PluginLoader;
import com.rayx.modular.plugin.PluginManager;
import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;
import com.rayx.scene.shape.ShapeType;
import com.rayx.ui.MainWindow;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import static org.lwjgl.opencl.CL22.*;

public class RayX {
    public static CLContext context;


    public static void main(String[] args) {
        /*
        ShapeType type = new ShapeType("testShape",1,
                """
                return cos(point).x; """, new ShapeType.CLField[]{
                        new ShapeType.CLField("field1", ShapeType.CLFieldType.FLOAT),
                        new ShapeType.CLField("field2", ShapeType.CLFieldType.FLOAT3),
                        new ShapeType.CLField("pointerToShape", ShapeType.CLFieldType.POINTER_SHAPE)});

        System.out.println("--------------------------");
        System.out.println(type.generateStruct());
        System.out.println("--------------------------");
        System.out.println(type.generateCLConverter());
        System.out.println("--------------------------");
        System.out.println(type.generateDistanceShaderFunction());
        System.out.println("--------------------------");

        System.exit(0);*/

        WindowManager manager = WindowManager.getInstance();

        TestOGLWindow window = new TestOGLWindow(1400, 600, "RayX Engine");
        //MainWindow window = new MainWindow(1600, 900, "RayX Engine");
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

        manager.startManager();

        freeAll();

        /*try {
            URL url = RayX.class.getResource("/plugins/basic-shapes/plugin.yml");
            File file = new File(url.getFile());
            PluginManager pm = new PluginManager();

            PluginLoader pl = new PluginLoader();
            Plugin plug = pl.loadPlugin(file);
        } catch ( FileNotFoundException e) {
            e.printStackTrace();
        }*/

    }

    public static CLContext treatDevice(OpenGLWindow window, long device) {
        long t = System.currentTimeMillis();
        CLContext context = CLManager.createContext(device, window.getWindow());

        System.out.println("Context allocation: " + (System.currentTimeMillis() - t));
        t = System.currentTimeMillis();
        context.initialize();

        test(context);

        System.out.println("Context creation: " + (System.currentTimeMillis() - t));
        return context;
    }

    static void test(CLContext context) {
        System.out.println("shape struct: " + context.getStructSize(-1));

        for (ShapeType shape : context.getRegisteredShapes()) {
            System.out.println(context.getTypeName(shape.getType()) + " struct: " + context.getStructSize(shape.getType()));
        }
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
