package com.rayx;

import com.rayx.examples.TestOGLWindow;
import com.rayx.glfw.WindowManager;

public class RayX {


    public static void main(String[] args) {
        WindowManager manager = WindowManager.getInstance();

        TestOGLWindow window = new TestOGLWindow(800, 600, "Test");
        manager.addWindow(window);

        manager.startManager();
    }


}
