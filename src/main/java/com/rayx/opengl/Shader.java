package com.rayx.opengl;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

public class Shader {

    private final int shader;

    public Shader(String source, ShaderType type) {
        shader = glCreateShader(type.getGLShaderType());
        glShaderSource(shader, source);
        glCompileShader(shader);

        int[] res = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, res);
        if (res[0] == GL_FALSE) {
            System.err.println(glGetShaderInfoLog(shader));
            glDeleteShader(shader);
        }
    }

    public void deleteShader() {
        glDeleteShader(shader);
    }

    public int getShader() {
        return shader;
    }
}