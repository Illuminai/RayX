package com.rayx.core.ui;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL20.glViewport;

public abstract class OpenGLWindow {

    private long window;
    private int width, height;
    private String title;

    private int[] posX = new int[1];
    private int[] posY = new int[1];
    private int[] sizeX = new int[1];
    private int[] sizeY = new int[1];

    public OpenGLWindow(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
        initWindow(width, height, title);
    }

    private void initWindow(int width, int height, String title) {
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(width, height, title, 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Failed to create window!");
        }
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        glfwSetKeyCallback(window, (eventWindow, key, scancode, action, mods) -> onKeyboardEvent(key, scancode, action, mods));

        glfwSetFramebufferSizeCallback(window, (eventWindow, eventWidth, eventHeight) -> onResize(eventWidth, eventHeight));
    }

    public abstract void onRender();

    public void onKeyboardEvent(int key, int scancode, int action, int mods) {
    }

    public void onResize(int width, int height) {
        glfwPollEvents();

        glViewport(0, 0, width, height);
        onRender();

        swapBuffers();
    }

    public void setFullscreen(boolean fullscreen) {
        if (isFullscreen() == fullscreen) {
            return;
        }

        if (fullscreen) {
            glfwGetWindowPos(window, posX, posY);
            glfwGetWindowSize(window, sizeX, sizeY);

            long monitor = getCurrentMonitor();
            GLFWVidMode mode = glfwGetVideoMode(monitor);
            glfwSetWindowMonitor(window, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());

        } else {
            glfwSetWindowMonitor(window, 0, posX[0], posY[0], sizeX[0], sizeY[0], 0);
        }
    }

    public boolean isFullscreen() {
        return glfwGetWindowMonitor(window) != 0;
    }

    public long getCurrentMonitor() {
        int[] wx = {0}, wy = {0}, ww = {0}, wh = {0};
        int[] mx = {0}, my = {0}, mw = {0}, mh = {0};
        int overlap, bestoverlap;
        long bestmonitor;

        GLFWVidMode mode;

        bestoverlap = 0;
        bestmonitor = glfwGetPrimaryMonitor();

        glfwGetWindowPos(window, wx, wy);
        glfwGetWindowSize(window, ww, wh);
        PointerBuffer monitors = glfwGetMonitors();

        while (monitors.hasRemaining()) {
            long monitor = monitors.get();
            mode = glfwGetVideoMode(monitor);
            glfwGetMonitorPos(monitor, mx, my);
            mw[0] = mode.width();
            mh[0] = mode.height();

            overlap =
                    Math.max(0, Math.min(wx[0] + ww[0], mx[0] + mw[0]) - Math.max(wx[0], mx[0])) *
                            Math.max(0, Math.min(wy[0] + wh[0], my[0] + mh[0]) - Math.max(wy[0], my[0]));

            if (bestoverlap < overlap) {
                bestoverlap = overlap;
                bestmonitor = monitor;
            }
        }

        return bestmonitor;
    }

    public void destroy() {
        glfwDestroyWindow(window);
    }

    public void swapBuffers() {
        glfwSwapBuffers(window);
    }

    public void makeContextCurrent() {
        glfwMakeContextCurrent(window);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    public void setShouldClose(boolean close) {
        glfwSetWindowShouldClose(window, close);
    }

    public void hide() {
        glfwHideWindow(window);
    }

    public void show() {
        glfwShowWindow(window);
    }

    public void setTitle(String title) {
        glfwSetWindowTitle(window, title);
    }

    public int[] getWindowSize() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(window, width, height);
        return new int[]{width[0], height[0]};
    }

    public long getWindow() {
        return window;
    }
}
