package com.rayx.examples;

import com.rayx.glfw.OpenGLWindow;

import static org.lwjgl.opengl.GL11.*;

public class TestOGLWindow extends OpenGLWindow {

    public TestOGLWindow(int width, int height, String title) {
        super(width, height, title);
    }

    @Override
    public void onRender() {
        glClearColor(0,1,1,1);
        glClear(GL_COLOR_BUFFER_BIT);
    }

}
