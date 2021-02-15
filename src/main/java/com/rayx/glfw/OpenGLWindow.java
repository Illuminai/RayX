package com.rayx.glfw;

import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL20.glViewport;

public abstract class OpenGLWindow {

    private long window;

    public OpenGLWindow(int width, int height, String title) {
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
