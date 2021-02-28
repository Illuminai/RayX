package com.rayx.core.ui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.opengl.GL11.*;

public abstract class ImGuiWindow extends OpenGLWindow {

    private ImGuiImplGlfw imGuiImplGlfw;
    private ImGuiImplGl3 imGuiImplGl3;

    private boolean firstFrame;

    public ImGuiWindow(int width, int height, String title) {
        super(width, height, title);
        initImGui();
    }

    private void initImGui() {
        imGuiImplGlfw = new ImGuiImplGlfw();
        imGuiImplGl3 = new ImGuiImplGl3();

        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);

        imGuiImplGlfw.init(getWindow(), true);
        imGuiImplGl3.init("#version 460");

        firstFrame = true;
    }

    @Override
    public void onRender() {
        glClearColor(0, 0,0,1);
        glClear(GL_COLOR_BUFFER_BIT);

        imGuiImplGlfw.newFrame();
        ImGui.newFrame();

        renderUI();

        ImGui.render();

        imGuiImplGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }

        if(firstFrame) {
            show();
            firstFrame = false;
        }
    }

    protected abstract void renderUI();

}
