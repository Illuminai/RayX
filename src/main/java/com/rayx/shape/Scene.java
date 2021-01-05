package com.rayx.shape;

import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.glFinish;

public class Scene {
    private final ArrayList<Shape> objects;

    private final String shapesIdentifier, shapesDataPrefix;

    public Scene() {
        objects = new ArrayList<>();
        shapesIdentifier = "shapes" + hashCode();
        shapesDataPrefix = "shapesData" + hashCode();
    }

    public void add(Shape s) {
        objects.add(s);
    }

    public boolean remove(Shape s) {
        return objects.remove(s);
    }

    public void render(CLContext context, int glTexture, boolean debug) {
        CLManager.transferShapesToRAM(context, shapesIdentifier,
                shapesDataPrefix, objects);
        if(debug)
        CLManager.testPrintGPUMemory(context, shapesIdentifier,
                shapesDataPrefix, objects);
        CLManager.runRenderKernel(context, glTexture,
                new double[]{-2, 0, 0},
                new double[]{0, 0, 0},
                1,
                objects.size(),
                shapesIdentifier,
                shapesDataPrefix
        );
    }

    public void deleteRenderMemory(CLContext context) {
        //TODO
        context.freeAllMemoryObjects();
    }
}
