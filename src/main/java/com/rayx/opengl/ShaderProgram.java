package com.rayx.opengl;


import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {

    private int program;

    public ShaderProgram() {
        program = glCreateProgram();
    }

    public void attachShader(Shader shader) {
        glAttachShader(program, shader.getShader());
    }

    public void linkProgram() {
        glLinkProgram(program);
        glValidateProgram(program);
    }

    public void useProgram() {
        glUseProgram(program);
    }

    public int getProgram() {
        return program;
    }


}
