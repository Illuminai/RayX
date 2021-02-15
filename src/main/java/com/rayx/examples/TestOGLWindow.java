package com.rayx.examples;

import com.rayx.RayX;
import com.rayx.core.math.Matrix3x3;
import com.rayx.core.math.Vector3d;
import com.rayx.glfw.OpenGLWindow;
import com.rayx.opencl.CLManager;
import com.rayx.opengl.Shader;
import com.rayx.opengl.ShaderProgram;
import com.rayx.opengl.ShaderType;
import com.rayx.scene.Camera;
import com.rayx.scene.Scene;
import com.rayx.scene.shape.Shape;
import imgui.*;
import imgui.extension.imnodes.ImNodes;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImInt;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL11;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
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

    private int t = 0;
    private long event;
    private boolean rendering;

    public TestOGLWindow(int width, int height, String title) {
        super(width, height, title);

        scene = new Scene.DemoScene();

        renderCallback = objs -> {
            Scene.DemoScene scene = (Scene.DemoScene) objs[3];
            scene.deleteRenderMemory(RayX.context);

            //scene.render(RayX.context, (int) objs[0], (boolean) objs[5], (Camera) objs[4], (int) objs[1], (int) objs[2]);
            rendering = true;
            event = scene.enqueueRender(RayX.context, (int) objs[0], (boolean) objs[5], (Camera) objs[4], (int) objs[1], (int) objs[2]);

            t += Math.PI / 50;
        };

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

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 256, 256,
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
        // Load custom Font
        // TODO: Load Linux system font
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            fontAtlas.addFontFromFileTTF("C:\\Windows\\Fonts\\Verdana.ttf", 13.0f);
        } else {
            fontAtlas.addFontDefault();
        }
        ImGuiFreeType.buildFontAtlas(fontAtlas);

        ImGuiStyle style = ImGui.getStyle();
        style.setColor(ImGuiCol.TabActive, 0, 119, 200, 255);
        style.setColor(ImGuiCol.ModalWindowDimBg, 0, 0, 0, 127);
        style.setWindowRounding(0);

        imGuiImplGlfw.init(getWindow(), true);
        imGuiImplGl3.init("#version 450");

        ImNodes.initialize();
    }

    private int frameWidth;
    private int frameHeight;

    private int textureWidth;
    private int textureHeight;

    private boolean debugViewport;

    private boolean first = true;
    private ImInt selectedSceneItem = new ImInt();

    @Override
    public void onRender() {

        if (first) {
            show();
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
            ImInt sidebar = new ImInt(imgui.internal.ImGui.dockBuilderSplitNode(dockspaceId.get(), ImGuiDir.Right, 0.15f, null, dockspaceId));
            int bottomBar = imgui.internal.ImGui.dockBuilderSplitNode(dockspaceId.get(), ImGuiDir.Down, 0.50f, null, dockspaceId);

            int properties = imgui.internal.ImGui.dockBuilderSplitNode(sidebar.get(), ImGuiDir.Down, 0.20f, null, sidebar);

            imgui.internal.ImGui.dockBuilderDockWindow("Scene", sidebar.get());
            imgui.internal.ImGui.dockBuilderDockWindow("Node Editor", bottomBar);
            imgui.internal.ImGui.dockBuilderDockWindow("Properties", properties);
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
                if (ImGui.menuItem("Sphere")) {
                    //scene.add(new Sphere(new Vector3d(0, 0, 0), 0.03f));
                }
                if (ImGui.menuItem("Box")) {
                    //scene.add(new BoxSDF(new Vector3d(0, 0, 0), new Vector3d(0, 0, 0), new Vector3d(0.03, 0.03, 0.03)));
                }
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }

        /*{
            ImGui.begin("Log Output");
            ImGui.text("Test test");
            ImGui.end();
        }*/

        {
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);

            ImGuiWindowClass windowClass = new ImGuiWindowClass();
            windowClass.setDockNodeFlagsOverrideSet(imgui.internal.flag.ImGuiDockNodeFlags.NoTabBar);
            ImGui.setNextWindowClass(windowClass);

            ImGui.begin("Viewport");
            ImGui.popStyleVar();


            if ((ImGui.getContentRegionAvailX() != frameWidth || ImGui.getContentRegionAvailY() != frameHeight)) {
                recreateTexture((int) ImGui.getContentRegionAvailX(), (int) ImGui.getContentRegionAvailY());
            }
            frameWidth = (int) ImGui.getContentRegionAvailX();
            frameHeight = (int) ImGui.getContentRegionAvailY();



            if(!rendering){
                renderCallback.accept(new Object[]{texture, frameWidth, frameHeight, scene, scene.getCamera(), debugViewport});
            }else{
                int[] execStatus = new int[1];
                CL11.clGetEventInfo(event, CL10.CL_EVENT_COMMAND_EXECUTION_STATUS, execStatus, null );
                if(execStatus[0] == CL11.CL_COMPLETE) {
                    //System.out.println("Nice");

                    glBindFramebuffer(GL_FRAMEBUFFER, FBO);
                    glViewport(0, 0, frameWidth, frameHeight);

                    // GL Clear slow?
                    // glClearColor(0, 1, 0, 1);
                    // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
                    glBindTexture(GL_TEXTURE_2D, texture);
                    glBindVertexArray(VAO);
                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
                    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
                    glBindFramebuffer(GL_FRAMEBUFFER, 0);
                    rendering = false;
                }else{
                    //System.out.println("Wait");
                }
            }


            /*glClearColor(0, 1, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
            glBindTexture(GL_TEXTURE_2D, texture);
            glBindVertexArray(VAO);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);*/

            ImGui.image(previewTexture, frameWidth, frameHeight, 0, 1, 1, 0);

            if (ImGui.isItemHovered() && io.getMouseWheel() != 0) {
                double factor = io.getMouseWheel() * 0.05 /*stepSize*/;

                // TODO: Rotationsmatrix
                Matrix3x3 rotation = Matrix3x3.createRotationMatrix(scene.getCamera().getRotation());

                Vector3d direction = rotation.transformed(new Vector3d(1, 0, 0).normalized());

                scene.getCamera().setPosition(scene.getCamera().getPosition().add(direction.scale(factor)));

                //System.out.println("Debug Mouse: " + io.getMouseWheel());
            }

            ImGui.end();
        }

        {
            ImGui.begin("Scene");
            ImGui.text("FPS: " + fps);

            ImGui.listBox("List", selectedSceneItem, scene.getVisibleObjects().stream().map(shape -> "" + shape.getClass().getSimpleName()).toArray(String[]::new), scene.getVisibleObjects().size());

            float[] positions = new float[]{
                    scene.getCamera().getPosition().getX(),
                    scene.getCamera().getPosition().getY(),
                    scene.getCamera().getPosition().getZ()
            };

            if (ImGui.dragFloat3("Position", positions, 0.01f)) {
                scene.getCamera().setPosition(new Vector3d(positions[0], positions[1], positions[2]));
            }

            float[] rotations = new float[]{
                    scene.getCamera().getRotation().getX(),
                    scene.getCamera().getRotation().getY(),
                    scene.getCamera().getRotation().getZ()
            };

            if (ImGui.dragFloat3("Rotation", rotations, 0.01f)) {
                scene.getCamera().setRotation(new Vector3d(rotations[0], rotations[1], rotations[2]));
            }

            float[] fov = new float[]{
                    scene.getCamera().getFov()
            };
            if (ImGui.dragFloat("FOV", fov, 0.01f, 0.01f, 5f)) {
                scene.getCamera().setFov(fov[0]);
            }

            if (ImGui.checkbox("Debug Preview", debugViewport)) {
                debugViewport = !debugViewport;
            }

            if (ImGui.button("Open Popup")) {
                ImGui.openPopup("Settings");
            }

            if (ImGui.beginPopupModal("Settings")) {
                ImGui.text("OOF");

                Object[] devices = CLManager.queryPlatforms().stream().map(aLong -> CLManager.queryDevicesForPlatform(aLong, getWindow(), false).toArray()).flatMap(Arrays::stream).toArray(Object[]::new);
                String[] printDevices = Arrays.stream(devices).map(o -> CLManager.queryDeviceInfo((Long) o, CL10.CL_DEVICE_NAME)).map(String::valueOf).toArray(String[]::new);
                ImGui.listBox("Devices", selectedDevice, printDevices, devices.length);

                ImGui.separator();
                if (ImGui.button("Close")) {
                    ImGui.closeCurrentPopup();
                }
                ImGui.endPopup();

            }

            //ImGui.listBox("OOF", selectedSceneItem, CLManager.queryPlatforms().stream().map(aLong -> CLManager.queryPlatformInfo(aLong, CL10.CL_DEVICE_NAME).toString()).toArray(String[]::new), CLManager.queryPlatforms().size());

            ImGui.end();
        }

        {
            ImGui.begin("Properties");

            Shape shape = scene.getVisibleObjects().get(selectedSceneItem.get());
            ImGui.text("Item " + shape.getClass().getSimpleName());

            float[] positions = new float[]{
                    shape.getPosition().getX(),
                    shape.getPosition().getY(),
                    shape.getPosition().getZ()
            };

            if (ImGui.dragFloat3("Position", positions, 0.01f)) {
                shape.setPosition(new Vector3d(positions[0], positions[1], positions[2]));
            }

            float[] rotations = new float[]{
                    shape.getRotation().getX(),
                    shape.getRotation().getY(),
                    shape.getRotation().getZ()
            };

            if (ImGui.dragFloat3("Rotation", rotations, 0.01f)) {
                shape.setRotation(new Vector3d(rotations[0], rotations[1], rotations[2]));
            }

            renderShapeInformation(shape);

            ImGui.end();
        }

        {
            ImGui.begin("Node Editor");

            ImNodes.beginNodeEditor();

            counter = 0;
            createNodeTree(scene.getVisibleObjects().get(selectedSceneItem.get()));


            ImNodes.endNodeEditor();

            ImGui.end();
        }

        ImGui.showDemoWindow();
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
            fps = frames;
            //System.out.println("FPS: " + frames);
            frames = 0;
            lastPrint = System.currentTimeMillis();
        }
        first = false;
    }

    private void createNodeTree(Shape shape) {
        createSingleNode(shape);

        for (Shape subShape : shape.getSubShapes()) {
            createNodeTree(subShape);
            ImNodes.link(counter++, (int) shape.getId() * 2 + 1, (int) subShape.getId() * 2);
        }
    }

    private int counter;


    private void createSingleNode(Shape shape) {
        ImNodes.beginNode((int) shape.getId());

        ImNodes.beginNodeTitleBar();
        ImGui.text(shape.getClass().getSimpleName());

        ImGui.sameLine();

        ImNodes.beginInputAttribute((int) shape.getId() * 2);
        ImGui.text("");
        ImNodes.endInputAttribute();

        ImGui.sameLine();

        ImNodes.beginOutputAttribute((int) shape.getId() * 2 + 1);
        ImGui.text("");
        ImNodes.endOutputAttribute();

        ImNodes.endNodeTitleBar();

        ImGui.pushItemWidth(120.f);
        float[] positions = new float[]{
                shape.getPosition().getX(),
                shape.getPosition().getY(),
                shape.getPosition().getZ()
        };

        if (ImGui.dragFloat3("Position", positions, 0.01f)) {
            shape.setPosition(new Vector3d(positions[0], positions[1], positions[2]));
        }

        float[] rotations = new float[]{
                shape.getRotation().getX(),
                shape.getRotation().getY(),
                shape.getRotation().getZ()
        };

        if (ImGui.dragFloat3("Rotation", rotations, 0.01f)) {
            shape.setRotation(new Vector3d(rotations[0], rotations[1], rotations[2]));
        }

        ImGui.popItemWidth();

        ImGui.button("", 120, 1);

        try {
            for (Field field : shape.getClass().getDeclaredFields()) {
                if (field.getGenericType() == Float.TYPE) {
                    field.setAccessible(true);
                    float[] value = new float[]{field.getFloat(shape)};
                    ImGui.pushItemWidth(120.f);
                    if (ImGui.dragFloat(field.getName() + "##" + shape.hashCode(), value, 0.01f)) {
                        field.setFloat(shape, value[0]);
                    }
                    ImGui.popItemWidth();
                }
                if (field.getType() == Vector3d.class) {
                    field.setAccessible(true);
                    Vector3d vector = (Vector3d) field.get(shape);
                    float[] values = new float[]{
                            vector.getX(),
                            vector.getY(),
                            vector.getZ()
                    };
                    ImGui.pushItemWidth(120.f);
                    if (ImGui.dragFloat3(field.getName() + "##" + shape.hashCode(), values, 0.01f)) {
                        field.set(shape, new Vector3d(values[0], values[1], values[2]));
                    }
                    ImGui.popItemWidth();
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        ImNodes.endNode();
    }


    private Shape renderShapeInformation(Shape shape) {
        try {
            ImGui.text(shape.getClass().getSimpleName());
            for (Field field : shape.getClass().getDeclaredFields()) {
                if (field.getGenericType() == Float.TYPE) {
                    field.setAccessible(true);
                    float[] value = new float[]{field.getFloat(shape)};
                    if (ImGui.dragFloat(field.getName() + "##" + shape.hashCode(), value, 0.01f)) {
                        field.setFloat(shape, value[0]);
                    }
                }
                if (field.getType() == Vector3d.class) {
                    field.setAccessible(true);
                    Vector3d vector = (Vector3d) field.get(shape);
                    float[] values = new float[]{
                            vector.getX(),
                            vector.getY(),
                            vector.getZ()
                    };

                    if (ImGui.dragFloat3(field.getName() + "##" + shape.hashCode(), values, 0.01f)) {
                        field.set(shape, new Vector3d(values[0], values[1], values[2]));
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        shape.getSubShapes().replaceAll(subShape -> {
            ImGui.beginChild("##" + subShape.hashCode());
            Shape ret = renderShapeInformation(subShape);

            ImGui.endChild();
            return ret;
        });

        return shape;

    }

    private int fps;

    private ImInt selectedDevice = new ImInt(0);

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

        //textureWidth = nearestPowerOfTwo(width);
        //textureHeight = nearestPowerOfTwo(height);


        if (texture != 0) {
            glDeleteTextures(texture);
        }
        texture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
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
        //glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
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

    private int nearestPowerOfTwo(int number) {
        int n = number;
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n;
    }

    private void makeTex() {
        if (texture != 0) {
            glDeleteTextures(texture);
        }
        texture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 256, 256,
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
