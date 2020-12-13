package com.rayx.glfw;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class WindowManager {

    private static WindowManager instance;

    private boolean running;

    private List<OpenGLWindow> windows;

    private WindowManager() {
        windows = new ArrayList<>();
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW!");
        }
    }

    public void startManager() {
        if (!running) {
            running = true;
            loopManager();
        }
    }

    public void stopManager() {
        if (running) {
            windows.forEach(OpenGLWindow::destroy);
            windows.clear();
            running = false;
        }
    }

    private void loopManager() {
        while (running && !windows.isEmpty()) {
            pollEvents();

            windows.stream()
                    .filter(OpenGLWindow::shouldClose)
                    .forEach(OpenGLWindow::destroy);

            windows.removeIf(OpenGLWindow::shouldClose);

            for (OpenGLWindow window : windows) {
                window.makeContextCurrent();
                window.onRender();
                window.swapBuffers();
            }
        }
    }

    public void addWindow(OpenGLWindow window) {
        windows.add(window);
    }

    public static WindowManager getInstance() {
        if (WindowManager.instance == null) {
            WindowManager.instance = new WindowManager();
        }
        return WindowManager.instance;
    }

    public void setSwapInterval(int interval) {
        glfwSwapInterval(interval);
    }

    private void pollEvents() {
        glfwPollEvents();
    }

    public boolean isRunning() {
        return running;
    }

}
