package com.rayx.glfw;

import org.lwjgl.glfw.GLFWErrorCallback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class WindowManager {

    private static WindowManager instance;

    private boolean running;

    private final List<OpenGLWindow> windows;

    private final List<OpenGLWindow> newWindows;

    private WindowManager() {
        windows = new ArrayList<>();
        newWindows = new ArrayList<>();
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
        while (running && (!windows.isEmpty() || !newWindows.isEmpty())) {
            pollEvents();

            windows.stream()
                    .filter(OpenGLWindow::shouldClose)
                    .forEach(OpenGLWindow::destroy);

            windows.removeIf(OpenGLWindow::shouldClose);

            if (!newWindows.isEmpty()) {
                windows.addAll(newWindows);
                newWindows.clear();
            }

            for (Iterator<OpenGLWindow> i = windows.iterator(); i.hasNext(); ) {
                OpenGLWindow window = i.next();
                window.makeContextCurrent();
                window.onRender();
                window.swapBuffers();
            }
        }
    }

    public void addWindow(OpenGLWindow window) {
        newWindows.add(window);
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
