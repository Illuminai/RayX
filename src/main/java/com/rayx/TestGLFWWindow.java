package com.rayx;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWNativeGLX;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class TestGLFWWindow {
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

    static String readFromFile(String file) {
        BufferedReader r = new BufferedReader(new InputStreamReader(RayX.class.getResourceAsStream(file)));
        StringBuilder b = new StringBuilder();
        String line;
        try {
            while((line = r.readLine()) != null) {
                b.append(line);
                b.append('\n');
            }
            r.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return b.toString();
    }

    static int w = 1000, h = 800, texture;
    public static long window;
    static int VAO, EBO,VBO;

    public static void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit()) {
            throw new RuntimeException();
        }
        window = glfwCreateWindow(w,h,"GPU-Hugger", 0,0);
        glfwSetKeyCallback(window, (winPar, key, scancode, action, mods) -> {
            if(action == GLFW_RELEASE) {
                if(key == GLFW_KEY_C) {
                    callback.accept(new Object[]{texture, w, h});
                }
            }

            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(winPar, true); // We will detect this in the rendering loop
        });
        glfwSetWindowSizeCallback(window, (paraWin, nH, nW) -> {
            glViewport(0,0, nH, nW);

            if(nH != h || nW != w) {
                w = nW;
                h = nW;
                makeTex();
                System.out.println("Resized: " + w + " " + h);
                callback.accept(new Object[]{texture, w, h});
            }
        });
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

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

    static Consumer<Object[]> callback;

    public static void loop(Consumer<Object[]> callback) {
        TestGLFWWindow.callback = callback;
        callback.accept(new Object[]{texture, w, h});

        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
            glBindTexture(GL_TEXTURE_2D, texture);
            glBindVertexArray(VAO);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
            //glDrawArrays(GL_TRIANGLES, 0, 3);
            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    static int createShader() {
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

    static int compileShader(int type, String src) {
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

    static void makeTex() {
        if(texture != 0) {
            glDeleteTextures(texture);
        }
        texture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h,
                0, GL_BGR, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }
}
