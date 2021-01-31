package com.rayx.shape;

import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.MemoryStack;

import java.beans.IntrospectionException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;

public class Scene {
    private final ArrayList<Shape> visibleObjects, allObjects;
    private final String shapesIdentifier, shapesDataPrefix;
    private int currentId;

    public Scene() {
        visibleObjects = new ArrayList<>();
        this.allObjects = new ArrayList<>();
        shapesIdentifier = "shapes" + hashCode();
        shapesDataPrefix = "shapesData" + hashCode();
    }

    public ArrayList<Shape> getAllObjects() {
        return allObjects;
    }

    public void add(Shape s) {
        visibleObjects.add(s);
    }

    public boolean remove(Shape s) {
        return visibleObjects.remove(s);
    }

    public void render(CLContext context, int glTexture, boolean debug, Camera camera, int width, int height) {
        transferShapesToRAM(context);

        /*if (debug) {
            CLManager.testPrintGPUMemory(context, shapesIdentifier,
                    shapesDataPrefix, allObjects);
        }*/

        CLManager.runRenderKernel(context, glTexture,
                camera,
                allObjects.size(),
                shapesIdentifier,
                shapesDataPrefix,
                width, height, debug
        );
    }

    private void addToAllObjects(Shape shape) {
        shape.getSubShapes().forEach(this::addToAllObjects);

        allObjects.add(shape);
        shape.setId(currentId);
        currentId++;
    }

    private void transferShapesToRAM(CLContext context) {
        try (MemoryStack stack = CLManager.nextStackFrame()) {
            assert !visibleObjects.isEmpty();
            currentId = 0;
            allObjects.clear();

            visibleObjects.forEach(this::addToAllObjects);

            allObjects.forEach(u -> u.setShouldRender(false));
            visibleObjects.forEach(u -> u.setShouldRender(true));

            int hostByteSize = allObjects.stream().mapToInt(Shape::bytesToInBuffer).sum();
            ByteBuffer inputData = stack.malloc(hostByteSize);
            allObjects.forEach(u -> u.writeToByteBuffer(inputData));
            for (int i = 0; i < allObjects.size(); i++) {
                assert allObjects.get(i).getId() == i;
            }
            inputData.position(0);

            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    inputData, "inputData");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    (long) context.getStructSize(Shape.SHAPE) * allObjects.size(),
                    shapesIdentifier);
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getName() == Shape.SPHERE_RTC).mapToInt(u -> context.getStructSize(u.getName())).sum(),
                    shapesDataPrefix + "SphereRTC");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getName() == Shape.TORUS_SDF).mapToInt(u -> context.getStructSize(u.getName())).sum(),
                    shapesDataPrefix + "TorusSDF");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getName() == Shape.PLANE_RTC).mapToInt(u -> context.getStructSize(u.getName())).sum(),
                    shapesDataPrefix + "PlaneRTC");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getName() == Shape.SUBTRACTION_SDF).mapToInt(u -> context.getStructSize(u.getName())).sum(),
                    shapesDataPrefix + "SubtractionSDF");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getName() == Shape.BOX_SDF).mapToInt(u -> context.getStructSize(u.getName())).sum(),
                    shapesDataPrefix + "BoxSDF");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getName() == Shape.UNION_SDF).mapToInt(u -> context.getStructSize(u.getName())).sum(),
                    shapesDataPrefix + "UnionSDF");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getName() == Shape.INTERSECTION_SDF).mapToInt(u -> context.getStructSize(u.getName())).sum(),
                    shapesDataPrefix + "IntersectionSDF");

            CLContext.CLKernel kernel = context.getKernelObject(CLContext.KERNEL_FILL_BUFFER_DATA);
            kernel.setParameter1i(0, allObjects.size());
            kernel.setParameterPointer(1, "inputData");
            kernel.setParameterPointer(2, shapesIdentifier);
            kernel.setParameterPointer(3, shapesDataPrefix + "SphereRTC");
            kernel.setParameterPointer(4, shapesDataPrefix + "TorusSDF");
            kernel.setParameterPointer(5, shapesDataPrefix + "PlaneRTC");
            kernel.setParameterPointer(6, shapesDataPrefix + "SubtractionSDF");
            kernel.setParameterPointer(7, shapesDataPrefix + "BoxSDF");
            kernel.setParameterPointer(8, shapesDataPrefix + "UnionSDF");
            kernel.setParameterPointer(9, shapesDataPrefix + "IntersectionSDF");

            kernel.run(new long[]{1}, null);

            context.freeMemoryObject("inputData");
        }
    }

    public void deleteRenderMemory(CLContext context) {
        allObjects.clear();

        //TODO
        context.freeAllMemoryObjects();
    }

    public void clearVisibleObjects() {
        visibleObjects.clear();
    }

    public static class DemoScene extends Scene {
        public DemoScene() {
            super();
        }

        public void set(CLContext context, double t) {
            assert context != null;
            deleteRenderMemory(context);
            clearVisibleObjects();

            exhibition(t / 10);
        }

        private void exhibition(double t) {
            float lumen = .1f;
            PlaneRTC bottom = new PlaneRTC(new Vector3d(0, 0, -.3), new Vector3d(0, 0, .3).normalized());
            bottom.setLumen(lumen);
            add(bottom);
            PlaneRTC top = new PlaneRTC(new Vector3d(0, 0, .3), new Vector3d(0, 0, -.3).normalized());
            top.setLumen(lumen);
            add(top);

            PlaneRTC back = new PlaneRTC(new Vector3d(.3, 0, 0), new Vector3d(-.3, 0, 0).normalized());
            back.setLumen(lumen);
            add(back);
            PlaneRTC front = new PlaneRTC(new Vector3d(-.3, 0, 0), new Vector3d(.3, 0, 0).normalized());
            front.setLumen(lumen);
            add(front);

            PlaneRTC left = new PlaneRTC(new Vector3d(0, -.3, 0), new Vector3d(0, .3, 0).normalized());
            left.setLumen(lumen);
            add(left);
            PlaneRTC right = new PlaneRTC(new Vector3d(0, .3, 0), new Vector3d(0, -.3, 0).normalized());
            right.setLumen(lumen);
            add(right);

            add(new TorusSDF(new Vector3d(0, -.1, 0), new Vector3d(0, 0, 0), .005f, .03f));

            add(new SubtractionSDF(
                    new Vector3d(0, -.1, .1),
                    new Vector3d(0, 0, 0),
                    new TorusSDF(new Vector3d(-.03, 0, 0),
                            new Vector3d(0, 0, 0), .005f, .02f),
                    new BoxSDF(new Vector3d(0, 0, 0), new Vector3d(0, 0, 0), new Vector3d(.03, .03, .03))));
            SphereRTC p;
            add(p = new SphereRTC(0, -.1f, -.1f, .03f));
            p.setLumen(10);
            add(new BoxSDF(
                    new Vector3d(0, 0, -.1),
                    new Vector3d(0, t, t),
                    new Vector3d(.03, .03, .03)));

            add(new UnionSDF(
                    new Vector3d(0, 0, 0),
                    new Vector3d(0, t, 0),
                    new TorusSDF(new Vector3d(0, 0, 0),
                            new Vector3d(0, 0, 0), .005f, .03f),
                    new BoxSDF(
                            new Vector3d(0, 0, 0),
                            new Vector3d(0, 0, 0),
                            new Vector3d(.01, .01, .01))));

            add(new IntersectionSDF(
                    new Vector3d(0, 0, .1),
                    new Vector3d(0, t, 0),
                    new TorusSDF(new Vector3d(0, .03, 0),
                            new Vector3d(0, 0, 0), .01f, .03f),
                    new BoxSDF(
                            new Vector3d(0, 0, 0),
                            new Vector3d(0, 0, 0),
                            new Vector3d(.03, .03 + .01 * Math.sin(t), .03 + .01 * Math.sin(t)))));

            add(new SubtractionSDF(
                    new Vector3d(0, .1, -.1),
                    new Vector3d(0, 0, 0),
                    new UnionSDF(
                            new Vector3d(-.03, 0, 0),
                            new Vector3d(t, 0, 0),
                            new TorusSDF(new Vector3d(0, 0, 0),
                                    new Vector3d(0, 0, 0), .005f, .02f),
                            new BoxSDF(
                                    new Vector3d(0, 0, 0),
                                    new Vector3d(0, 0, 0),
                                    new Vector3d(.01, .01, .01))),
                    new BoxSDF(new Vector3d(0, 0, 0), new Vector3d(0, 0, 0), new Vector3d(.03, .03, .03))));
        }
    }
}
