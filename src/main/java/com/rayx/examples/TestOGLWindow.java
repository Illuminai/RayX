package com.rayx.examples;

import com.rayx.RayX;
import com.rayx.glfw.OpenGLWindow;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class TestOGLWindow extends OpenGLWindow {

    static final String VS, FS;

    static {
        VS = """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                layout (location = 1) in vec2 aTexCoord;
                                
                out vec2 TexCoord;
                void main() {
                    gl_Position = vec4(aPos, 1.0);
                    TexCoord = aTexCoord;
                }
                """;
        FS = """
                #version 330 core
                out vec4 FragColor;
                in vec2 TexCoord;
                uniform sampler2D ourTexture;
                void main()
                {
                    FragColor = texture(ourTexture, TexCoord);
                }
                """;
    }

    private int texture;
    private int VAO, EBO, VBO;
    private long lastPrint = 0;
    private int frames;

    private Consumer<Integer> callback;

    public TestOGLWindow(int width, int height, String title) {
        super(width, height, title);

        int shader = createShader();
        glLinkProgram(shader);
        glUseProgram(shader);

        float[] vertices = {
                1.0f, 1.0f, 0.0f, 1f, 0f,// top right
                1.0f, -1.0f, 0.0f, 1f, 1f,// bottom right
                -1.0f, -1.0f, 0.0f, 0f,1f,// bottom left
                -1.0f, 1.0f, 0.0f, 0f, 0f // top left
        };
        int[] indices = { // note that we start from 0!
                0, 1, 3, // first triangle
                1, 2, 3 // second triangle
        };
        EBO = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        VAO = glGenVertexArrays();
        glBindVertexArray(VAO);

        VBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false,
                5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false,
                5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        makeTex();

        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void onRender() {
        glClearColor(0, 1, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        callback.accept(texture);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
        glBindTexture(GL_TEXTURE_2D, texture);
        glBindVertexArray(VAO);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        frames++;
        if(System.currentTimeMillis() - lastPrint > 1000) {
            System.out.println("FPS: "+frames);
            frames = 0;
            lastPrint = System.currentTimeMillis();
        }
    }

    private int createShader() {
        int program = glCreateProgram();
        int vs = compileShader(GL_VERTEX_SHADER, VS);
        int fs = compileShader(GL_FRAGMENT_SHADER, FS);

        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        glValidateProgram(program);

        glDeleteShader(vs);
        glDeleteShader(fs);

        return program;
    }

    private int compileShader(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        int[] res = new int[1];
        glGetShaderiv(id, GL_COMPILE_STATUS, res);
        if(res[0] == GL_FALSE) {
            System.out.println(glGetShaderInfoLog(id));
            glDeleteShader(id);
            assert false: "Shader";
        }

        return id;
    }

    private void makeTex() {
        if(texture != 0) {
            glDeleteTextures(texture);
        }
        texture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, RayX.IMG_WID, RayX.IMG_HEI,
                0, GL_BGR, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    public void setCallback(Consumer<Integer> callback) {
        this.callback = callback;
    }
}
