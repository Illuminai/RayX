package com.rayx.opengl;

import static org.lwjgl.opengl.GL20.*;

public enum ShaderType {
    FRAGMENT_SHADER(GL_FRAGMENT_SHADER),
    VERTEX_SHADER(GL_VERTEX_SHADER);

    private final int glShaderType;

    ShaderType(int glShaderType) {
        this.glShaderType = glShaderType;
    }

    public int getGLShaderType() {
        return glShaderType;
    }

}
