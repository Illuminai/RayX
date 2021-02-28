package com.rayx.examples;

import com.rayx.RayX;
import com.rayx.core.gl.Shader;
import com.rayx.core.gl.ShaderProgram;
import com.rayx.core.gl.ShaderType;
import com.rayx.core.math.Matrix3x3;
import com.rayx.core.math.Vector3d;
import com.rayx.core.ui.OpenGLWindow;
import com.rayx.scene.Camera;
import com.rayx.scene.Scene;
import com.rayx.scene.shape.Shape;
import com.rayx.scene.shape.ShapeType;
import imgui.*;
import imgui.extension.imnodes.ImNodes;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opencl.CL10;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Map;
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

    private int maxSamples = 5;

    public TestOGLWindow(int width, int height, String title) {
        super(width, height, title);

        scene = new Scene.DemoScene();

        renderCallback = objs -> {
            Scene.DemoScene scene = (Scene.DemoScene) objs[3];
            scene.deleteRenderMemory(RayX.context);

            //scene.render(RayX.context, (int) objs[0], (boolean) objs[5], (Camera) objs[4], (int) objs[1], (int) objs[2]);
            rendering = true;
            event = scene.enqueueRender(RayX.context, (int) objs[0], (boolean) objs[5], (Camera) objs[4], (int) objs[1], (int) objs[2], (int) objs[6]);

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

        setupTheme();

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

    private boolean toggleFullscreen;

    private void setupTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setTabRounding(0.0f);
        style.setFrameBorderSize(1.0f);
        style.setScrollbarRounding(0.0f);
        style.setScrollbarSize(10.0f);

        style.setColor(ImGuiCol.Text, 0.95f, 0.95f, 0.95f, 1.00f);
        style.setColor(ImGuiCol.TextDisabled, 0.50f, 0.50f, 0.50f, 1.00f);
        style.setColor(ImGuiCol.WindowBg, 0.12f, 0.12f, 0.12f, 1.00f);
        style.setColor(ImGuiCol.ChildBg, 0.04f, 0.04f, 0.04f, 0.50f);
        style.setColor(ImGuiCol.PopupBg, 0.12f, 0.12f, 0.12f, 0.94f);
        style.setColor(ImGuiCol.Border, 0.25f, 0.25f, 0.27f, 0.50f);
        style.setColor(ImGuiCol.BorderShadow, 0.00f, 0.00f, 0.00f, 0.00f);
        style.setColor(ImGuiCol.FrameBg, 0.20f, 0.20f, 0.22f, 0.50f);
        style.setColor(ImGuiCol.FrameBgHovered, 0.25f, 0.25f, 0.27f, 0.75f);
        style.setColor(ImGuiCol.FrameBgActive, 0.30f, 0.30f, 0.33f, 1.00f);
        style.setColor(ImGuiCol.TitleBg, 0.04f, 0.04f, 0.04f, 1.00f);
        style.setColor(ImGuiCol.TitleBgActive, 0.04f, 0.04f, 0.04f, 1.00f);
        style.setColor(ImGuiCol.TitleBgCollapsed, 0.04f, 0.04f, 0.04f, 0.75f);
        style.setColor(ImGuiCol.MenuBarBg, 0.18f, 0.18f, 0.19f, 1.00f);
        style.setColor(ImGuiCol.ScrollbarBg, 0.24f, 0.24f, 0.26f, 0.75f);
        style.setColor(ImGuiCol.ScrollbarGrab, 0.41f, 0.41f, 0.41f, 0.75f);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 0.62f, 0.62f, 0.62f, 0.75f);
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.94f, 0.92f, 0.94f, 0.75f);
        style.setColor(ImGuiCol.CheckMark, 0.60f, 0.60f, 0.60f, 1.00f);
        style.setColor(ImGuiCol.SliderGrab, 0.41f, 0.41f, 0.41f, 0.75f);
        style.setColor(ImGuiCol.SliderGrabActive, 0.62f, 0.62f, 0.62f, 0.75f);
        style.setColor(ImGuiCol.Button, 0.20f, 0.20f, 0.22f, 1.00f);
        style.setColor(ImGuiCol.ButtonHovered, 0.25f, 0.25f, 0.27f, 1.00f);
        style.setColor(ImGuiCol.ButtonActive, 0.41f, 0.41f, 0.41f, 1.00f);
        style.setColor(ImGuiCol.Header, 0.18f, 0.18f, 0.19f, 1.00f);
        style.setColor(ImGuiCol.HeaderHovered, 0.25f, 0.25f, 0.27f, 1.00f);
        style.setColor(ImGuiCol.HeaderActive, 0.41f, 0.41f, 0.41f, 1.00f);
        style.setColor(ImGuiCol.Separator, 0.25f, 0.25f, 0.27f, 1.00f);
        style.setColor(ImGuiCol.SeparatorHovered, 0.41f, 0.41f, 0.41f, 1.00f);
        style.setColor(ImGuiCol.SeparatorActive, 0.62f, 0.62f, 0.62f, 1.00f);
        style.setColor(ImGuiCol.ResizeGrip, 0.30f, 0.30f, 0.33f, 0.75f);
        style.setColor(ImGuiCol.ResizeGripHovered, 0.41f, 0.41f, 0.41f, 0.75f);
        style.setColor(ImGuiCol.ResizeGripActive, 0.62f, 0.62f, 0.62f, 0.75f);
        style.setColor(ImGuiCol.Tab, 0.21f, 0.21f, 0.22f, 1.00f);
        style.setColor(ImGuiCol.TabHovered, 0.37f, 0.37f, 0.39f, 1.00f);
        style.setColor(ImGuiCol.TabActive, 0.30f, 0.30f, 0.33f, 1.00f);
        style.setColor(ImGuiCol.TabUnfocused, 0.12f, 0.12f, 0.12f, 0.97f);
        style.setColor(ImGuiCol.TabUnfocusedActive, 0.18f, 0.18f, 0.19f, 1.00f);
        style.setColor(ImGuiCol.DockingPreview, 0.26f, 0.59f, 0.98f, 0.50f);
        style.setColor(ImGuiCol.DockingEmptyBg, 0.20f, 0.20f, 0.20f, 1.00f);
        style.setColor(ImGuiCol.PlotLines, 0.61f, 0.61f, 0.61f, 1.00f);
        style.setColor(ImGuiCol.PlotLinesHovered, 1.00f, 0.43f, 0.35f, 1.00f);
        style.setColor(ImGuiCol.PlotHistogram, 0.90f, 0.70f, 0.00f, 1.00f);
        style.setColor(ImGuiCol.PlotHistogramHovered, 1.00f, 0.60f, 0.00f, 1.00f);
        style.setColor(ImGuiCol.TextSelectedBg, 0.26f, 0.59f, 0.98f, 0.50f);
        style.setColor(ImGuiCol.DragDropTarget, 1.00f, 1.00f, 0.00f, 0.90f);
        style.setColor(ImGuiCol.NavHighlight, 0.26f, 0.59f, 0.98f, 1.00f);
        style.setColor(ImGuiCol.NavWindowingHighlight, 1.00f, 1.00f, 1.00f, 0.70f);
        style.setColor(ImGuiCol.NavWindowingDimBg, 0.80f, 0.80f, 0.80f, 0.20f);
        style.setColor(ImGuiCol.ModalWindowDimBg, 0, 0, 0, 127);

    }

    @Override
    public void onRender() {

        if (first) {
            show();
        }

        glClearColor(0, 0, 0, 1);
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
            int bottomBar = imgui.internal.ImGui.dockBuilderSplitNode(dockspaceId.get(), ImGuiDir.Down, 0.40f, null, dockspaceId);
            ImInt sidebar = new ImInt(imgui.internal.ImGui.dockBuilderSplitNode(dockspaceId.get(), ImGuiDir.Right, 0.15f, null, dockspaceId));

            int properties = imgui.internal.ImGui.dockBuilderSplitNode(sidebar.get(), ImGuiDir.Down, 0.40f, null, sidebar);

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
                if (ImGui.menuItem("Settings")) {
                    //TODO: Settings
                }
                ImGui.separator();
                if (ImGui.menuItem("Exit")) {
                    System.exit(0);
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Add")) {
                for (ShapeType shapeType : RayX.context.getRegisteredShapes()) {
                    if (ImGui.menuItem(shapeType.getShapeName())) {
                        Shape shape = new Shape(shapeType, 100, new Vector3d(0, 0, 0), new Vector3d(0, 0, 0));
                        for (ShapeType.CLField key : shapeType.getFields()) {
                            if (key.getType() == ShapeType.CLFieldType.FLOAT3) {

                                shape.getProperties().put(key.getName(), new Vector3d(0.1, 0.1, 0.1));
                            } else if (key.getType() == ShapeType.CLFieldType.FLOAT) {

                                shape.getProperties().put(key.getName(), 0.1f);
                            }
                        }
                        scene.add(shape);
                    }
                }

                ImGui.endMenu();
            }
            if (ImGui.beginMenu("View")) {
                if (ImGui.beginMenu("Appearance Mode")) {
                    if (ImGui.menuItem("Fullscreen", "", isFullscreen())) {
                        toggleFullscreen = true;
                    }

                    ImGui.endMenu();
                }

                if (ImGui.menuItem("Demo Window", "", demoOpen)) {
                    demoOpen = !demoOpen;
                }

                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }


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


            if (!rendering) {
                renderCallback.accept(new Object[]{texture, frameWidth, frameHeight, scene, scene.getCamera(), debugViewport, maxSamples});
            } else {
                int[] execStatus = new int[1];
                CL10.clGetEventInfo(event, CL10.CL_EVENT_COMMAND_EXECUTION_STATUS, execStatus, null);
                if (execStatus[0] == CL10.CL_COMPLETE) {
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
                } else {
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

            int flags = ImGuiTableFlags.BordersV
                    | ImGuiTableFlags.BordersOuterH
                    | ImGuiTableFlags.Resizable
                    | ImGuiTableFlags.RowBg
                    | ImGuiTableFlags.NoBordersInBody
                    | ImGuiTableFlags.ScrollY;
            if (ImGui.beginTable("Test Title", 1, flags)) {
                ImGui.tableSetupColumn("Name", ImGuiTableColumnFlags.NoHide);
                ImGui.tableHeadersRow();

                for (Shape shape : scene.getVisibleObjects()) {
                    displayShapeTable(shape);
                }

                ImGui.endTable();
            }

            //ImGui.listBox("List", selectedSceneItem, scene.getVisibleObjects().stream().map(shape -> "" + shape.getClass().getSimpleName()).toArray(String[]::new), scene.getVisibleObjects().size());

            /*

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

            }*/

            //ImGui.listBox("OOF", selectedSceneItem, CLManager.queryPlatforms().stream().map(aLong -> CLManager.queryPlatformInfo(aLong, CL10.CL_DEVICE_NAME).toString()).toArray(String[]::new), CLManager.queryPlatforms().size());

            ImGui.end();
        }

        {
            ImGui.begin("Properties");

            if (ImGui.checkbox("Debug Preview", debugViewport)) {
                debugViewport = !debugViewport;
            }

            int[] samples = new int[]{
                    maxSamples
            };
            if (ImGui.sliderInt("Samples", samples, 1, 10)) {
                maxSamples = samples[0];
            }

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

        if (demoOpen) {
            ImGui.showDemoWindow();
        }

        /*if (beginStatusBar()) {
            ImGui.button("Test");
            endStatusBar();
        }*/

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

        if (toggleFullscreen) {
            toggleFullscreen = false;
            if (isFullscreen()) {
                //ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
            } else {
                ImGui.getIO().removeConfigFlags(ImGuiConfigFlags.ViewportsEnable);
            }
            setFullscreen(!isFullscreen());
        }
    }

    private boolean demoOpen;

    /*private boolean beginStatusBar() {
        ImGuiViewport viewport = ImGui.getMainViewport();

        int flags = ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.MenuBar;


        ImGui.setNextWindowSize(viewport.getSizeX(), 1.0f);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowMinSize, 0, 0);
        boolean open = ImGui.begin("##StatusBar", flags) && ImGui.beginMenuBar();
        ImGui.popStyleVar(2);

        if (!open) {
            ImGui.end();
            return false;
        }

        return true;
    }

    private void endStatusBar() {
        ImGuiViewport viewport = ImGui.getMainViewport();

        ImGui.setWindowPos(viewport.getPosX() ,viewport.getPosY() + viewport.getSizeY() - ImGui.getWindowHeight());


        //viewport.seWorkOffsetMin(viewport.getWorkOffsetMinX(), viewport.getWorkOffsetMinY() - ImGui.getWindowHeight());
        viewport.setWorkSize(viewport.getWorkSizeX(), viewport.getWorkSizeY() - ImGui.getWindowSizeY());

        ImGui.endMenuBar();
        ImGui.end();
    }*/

    private void createNodeTree(Shape shape) {
        createSingleNode(shape);

        for (Shape subShape : shape.getSubShapes()) {
            createNodeTree(subShape);
            ImNodes.link(counter++, (int) shape.getId() * 2 + 1, (int) subShape.getId() * 2);
        }
    }

    private void createSingleNode(Shape shape) {
        ImNodes.beginNode((int) shape.getId());

        ImNodes.beginNodeTitleBar();
        ImGui.text(shape.getType().getShapeName());

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

        for (Map.Entry<String, Object> entry : shape.getProperties().entrySet()) {
            if (entry.getValue() instanceof Float) {
                float[] value = new float[]{(float) entry.getValue()};
                ImGui.pushItemWidth(120.f);
                if (ImGui.dragFloat(entry.getKey() + "##" + shape.hashCode(), value, 0.01f)) {
                    entry.setValue(value[0]);
                }
                ImGui.popItemWidth();
            } else if (entry.getValue() instanceof Vector3d) {
                Vector3d vector = (Vector3d) entry.getValue();
                float[] values = new float[]{
                        vector.getX(),
                        vector.getY(),
                        vector.getZ()
                };
                ImGui.pushItemWidth(120.f);
                if (ImGui.dragFloat3(entry.getKey() + "##" + shape.hashCode(), values, 0.01f)) {
                    entry.setValue(new Vector3d(values[0], values[1], values[2]));
                }
                ImGui.popItemWidth();
            }
        }

        ImNodes.endNode();
    }

    private int counter;

    private void displayShapeTable(Shape shape) {
        ImGui.tableNextRow();
        ImGui.tableNextColumn();

        if (!shape.getSubShapes().isEmpty()) {
            boolean open = ImGui.treeNodeEx(shape.getType().getShapeName(),
                    ImGuiTreeNodeFlags.OpenOnDoubleClick
                            | ImGuiTreeNodeFlags.OpenOnArrow);
            if (open) {
                for (Shape subShape : shape.getSubShapes()) {
                    displayShapeTable(subShape);
                }
                ImGui.treePop();
            }
        } else {
            ImGui.treeNodeEx(shape.getType().getShapeName(), ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen | ImGuiTreeNodeFlags.SpanFullWidth);
        }


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
