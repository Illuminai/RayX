package com.rayx.examples;

import com.rayx.RayX;
import com.rayx.glfw.OpenGLWindow;
import com.rayx.opengl.Shader;
import com.rayx.opengl.ShaderProgram;
import com.rayx.opengl.ShaderType;
import com.rayx.shape.Camera;
import com.rayx.shape.Matrix3x3;
import com.rayx.shape.Scene;
import com.rayx.shape.Vector3d;
import imgui.*;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;

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
    private int previewTexture;
    private int VAO, EBO, VBO, FBO;
    private long lastPrint = 0;
    private int frames;

    private Consumer<Object[]> renderCallback;

    private ImGuiImplGlfw imGuiImplGlfw;
    private ImGuiImplGl3 imGuiImplGl3;

    private int dockspace;

    private Scene.DemoScene scene;

    private Camera camera;

    public TestOGLWindow(int width, int height, String title) {
        super(width, height, title);

        scene = new Scene.DemoScene();
        camera = new Camera(new Vector3d(-.2f, 0, 0), new Vector3d(0, 0, 0), 1);

        ShaderProgram program = createProgram();
        program.useProgram();

        float[] vertices = {
                1.0f, 1.0f, 0.0f, 1f, 0f,// top right
                1.0f, -1.0f, 0.0f, 1f, 1f,// bottom right
                -1.0f, -1.0f, 0.0f, 0f, 1f,// bottom left
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

        previewTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, previewTexture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 2048, 2048,
                0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        FBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, previewTexture, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        imGuiImplGlfw = new ImGuiImplGlfw();
        imGuiImplGl3 = new ImGuiImplGl3();

        initImGui();
    }

    private void initImGui() {
        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.setConfigViewportsNoTaskBarIcon(true);

        final ImFontAtlas fontAtlas = io.getFonts();
        //fontAtlas.addFontDefault();
        //fontAtlas.addFontFromMemoryTTF(loadFromResources("Karla.ttf"), 14.0f);
        fontAtlas.addFontFromFileTTF("C:\\Windows\\Fonts\\Verdana.ttf", 13.0f);
        ImGuiFreeType.buildFontAtlas(fontAtlas);


        ImGuiStyle style = ImGui.getStyle();
        //style.setColor(ImGuiCol.TabActive, 0,119, 200, 255);

        if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            style.setWindowRounding(0.0f);
            style.setColor(ImGuiCol.WindowBg, ImGui.getColorU32(ImGuiCol.WindowBg, 1));
        }

        imGuiImplGlfw.init(getWindow(), true);
        imGuiImplGl3.init("#version 450");
    }

    private int frameWidth;
    private int frameHeight;

    private boolean debugViewport;

    private boolean first = true;
    private ImInt selectedSceneItem = new ImInt();

    @Override
    public void onRender() {

        if (first) {
            show();
            first = false;
        }

        glClearColor(0, 1, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        imGuiImplGlfw.newFrame();
        ImGui.newFrame();

        ImGuiIO io = ImGui.getIO();

        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getPosX(), viewport.getPosY());
        ImGui.setNextWindowSize(viewport.getSizeX(), viewport.getSizeY());
        ImGui.setNextWindowViewport(viewport.getID());

        int windowFlags = ImGuiWindowFlags.MenuBar
                | ImGuiWindowFlags.NoDocking
                | ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoBringToFrontOnFocus
                | ImGuiWindowFlags.NoNavFocus
                | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoDocking;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.begin("Dock", windowFlags);
        ImGui.popStyleVar(3);

        if (imgui.internal.ImGui.dockBuilderGetNode(ImGui.getID("MainDockspace")) == null) {
            dockspace = ImGui.getID("MainDockspace");

            imgui.internal.ImGui.dockBuilderRemoveNode(dockspace);
            imgui.internal.ImGui.dockBuilderAddNode(dockspace);

            ImInt dockspaceId = new ImInt(dockspace);
            int sidebar = imgui.internal.ImGui.dockBuilderSplitNode(dockspaceId.get(), ImGuiDir.Left, 0.20f, null, dockspaceId);
            int bottomBar = imgui.internal.ImGui.dockBuilderSplitNode(dockspaceId.get(), ImGuiDir.Down, 0.20f, null, dockspaceId);

            imgui.internal.ImGui.dockBuilderDockWindow("Scene", sidebar);
            imgui.internal.ImGui.dockBuilderDockWindow("Log Output", bottomBar);
            imgui.internal.ImGui.dockBuilderDockWindow("Viewport", dockspaceId.get());
            imgui.internal.ImGui.dockBuilderFinish(dockspace);
        }
        int dockFlags = ImGuiDockNodeFlags.PassthruCentralNode
                | imgui.internal.flag.ImGuiDockNodeFlags.NoWindowMenuButton
                | imgui.internal.flag.ImGuiDockNodeFlags.NoCloseButton;
        ImGui.dockSpace(ImGui.getID("MainDockspace"), 0, 0, dockFlags);
        ImGui.end();


        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Exit")) {
                    System.exit(0);
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("Test")) {
                }
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }

        {
            ImGui.begin("Log Output");
            ImGui.text("Test test");
            ImGui.end();
        }

        {
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
            ImGui.begin("Viewport");
            ImGui.popStyleVar();


            if ((ImGui.getContentRegionAvailX() != frameWidth || ImGui.getContentRegionAvailY() != frameHeight)) {
                recreateTexture((int) ImGui.getContentRegionAvailX(), (int) ImGui.getContentRegionAvailY());

            }
            frameWidth = (int) ImGui.getContentRegionAvailX();
            frameHeight = (int) ImGui.getContentRegionAvailY();

            glBindFramebuffer(GL_FRAMEBUFFER, FBO);

            glViewport(0, 0, frameWidth, frameHeight);
            renderCallback.accept(new Object[]{texture, frameWidth, frameHeight, scene, camera, debugViewport});

            glClearColor(0, 1, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
            glBindTexture(GL_TEXTURE_2D, texture);
            glBindVertexArray(VAO);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            ImGui.image(previewTexture, frameWidth, frameHeight, 0, 1, 1, 0);

            if (ImGui.isItemHovered() && io.getMouseWheel() != 0) {
                double factor = io.getMouseWheel() * 0.05 /*stepSize*/;

                // TODO: Rotationsmatrix
                Matrix3x3 rotation = Matrix3x3.createRotationMatrix(camera.getRotation());

                Vector3d direction = rotation.transformed(new Vector3d(1, 0, 0).normalized());

                camera.setPosition(camera.getPosition().add(direction.scale(factor)));

                System.out.println("Debug Mouse: " + io.getMouseWheel());
            }

            ImGui.end();
        }

        {
            ImGui.begin("Scene");
            ImGui.text("FPS: " + frames);
            //ImGui.listBox("List", selectedSceneItem, scene.getAllObjects().stream().map(shape -> "" + shape.getName()).toArray(String[]::new), scene.getAllObjects().size());

            float[] positions = new float[]{
                    camera.getPosition().getX(),
                    camera.getPosition().getY(),
                    camera.getPosition().getZ()
            };

            if (ImGui.dragFloat3("Position", positions, 0.01f)) {
                camera.setPosition(new Vector3d(positions[0], positions[1], positions[2]));
            }

            float[] rotations = new float[]{
                    camera.getRotation().getX(),
                    camera.getRotation().getY(),
                    camera.getRotation().getZ()
            };

            if (ImGui.dragFloat3("Rotation", rotations, 0.01f)) {
                camera.setRotation(new Vector3d(rotations[0], rotations[1], rotations[2]));
            }

            if(ImGui.checkbox("Debug Preview", debugViewport)){
                debugViewport = !debugViewport;
            }

            ImGui.end();
        }

        //ImGui.showDemoWindow();
        //ImGui.showMetricsWindow();

        ImGui.render();

        imGuiImplGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }

        frames++;
        if (System.currentTimeMillis() - lastPrint > 1000) {
            frames = 0;
            lastPrint = System.currentTimeMillis();
        }
    }

    @Override
    public void onKeyboardEvent(int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_A) {
                System.out.println("Key 'A' pressed");
            } else if (key == GLFW.GLFW_KEY_B && mods == GLFW.GLFW_MOD_SHIFT) {
                System.out.println("Key 'SHIFT + B' pressed");
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            if (key == GLFW.GLFW_KEY_C) {
                System.out.println("Key 'C' released");
            }
        } else if (action == GLFW.GLFW_REPEAT) {
            if (key == GLFW.GLFW_KEY_D) {
                System.out.println("Key 'D' repeated");
            }
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
    }

    private ShaderProgram createProgram() {
        ShaderProgram program = new ShaderProgram();
        Shader vertexShader = new Shader(VS, ShaderType.VERTEX_SHADER);
        Shader fragmentShader = new Shader(FS, ShaderType.FRAGMENT_SHADER);

        program.attachShader(vertexShader);
        program.attachShader(fragmentShader);
        program.linkProgram();

        vertexShader.deleteShader();
        fragmentShader.deleteShader();

        return program;
    }

    private void recreateTexture(int width, int height) {


        if (texture != 0) {

            glDeleteTextures(texture);
        }
        texture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height,
                0, GL_BGR, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);


        if (previewTexture != 0) {
            glDeleteTextures(previewTexture);
        }
        previewTexture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, previewTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height,
                0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, previewTexture, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void makeTex() {
        if (texture != 0) {
            glDeleteTextures(texture);
        }
        texture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 2000, 2000,
                0, GL_BGR, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }


    public void setCallback(Consumer<Object[]> renderCallback) {
        this.renderCallback = renderCallback;
    }

    private byte[] loadFromResources(final String fileName) {
        try (InputStream is = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(fileName));
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            final byte[] data = new byte[16384];

            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            return buffer.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
