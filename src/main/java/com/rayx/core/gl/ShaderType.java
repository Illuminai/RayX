package com.rayx.core.gl;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

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
