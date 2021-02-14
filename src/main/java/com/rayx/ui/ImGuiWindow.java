package com.rayx.ui;

import com.rayx.glfw.OpenGLWindow;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;

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

        imGuiImplGlfw.init(getWindow(), true);
        imGuiImplGl3.init("#version 460");

        firstFrame = true;
    }

    @Override
    public void onRender() {
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
