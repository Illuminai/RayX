package com.rayx.shape;

import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;
import com.rayx.shape.material.Material;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;

public class Scene {
    private final ArrayList<Shape> visibleObjects, allObjects;
    private final String shapesIdentifier, shapesDataPrefix;
    private Vector3d cameraPos, cameraRot;
    private float cameraFOV;

    private int currentId;

    public Scene() {
        visibleObjects = new ArrayList<>();
        this.allObjects = new ArrayList<>();
        shapesIdentifier = "shapes" + hashCode();
        shapesDataPrefix = "shapesData" + hashCode();
        //TODO default camera position
        this.cameraPos = new Vector3d(-.2,0,0);
        this.cameraRot = new Vector3d(0,0,0);
        cameraFOV = 1;
    }

    public void add(Shape s) {
        visibleObjects.add(s);
    }

    public boolean remove(Shape s) {
        return visibleObjects.remove(s);
    }

    public void render(CLContext context, int glTexture, boolean debug) {
        transferShapesToRAM(context);

        if(debug) {
            CLManager.testPrintGPUMemory(context, shapesIdentifier,
                    shapesDataPrefix, allObjects);
        }

        CLManager.runRenderKernel(context, glTexture,
                new float[]{cameraPos.getX(), cameraPos.getY(), cameraPos.getZ()},
                new float[]{cameraRot.getX(), cameraRot.getY(), cameraRot.getZ()},
                cameraFOV,
                allObjects.size(),
                shapesIdentifier,
                shapesDataPrefix
        );
    }

    public void move(float amountX, float amountY, float amountZ) {
        cameraPos = new Vector3d(cameraPos.getX() + amountX, cameraPos.getY() + amountY, cameraPos.getZ() + amountZ);
    }

    public void turn(float amountX, float amountY, float amountZ) {
         cameraRot = new Vector3d(cameraRot.getX() + amountX, cameraRot.getY() + amountY, cameraRot.getZ() + amountZ);
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
                    allObjects.stream().filter(u -> u.getShape() == Shape.SPHERE).mapToInt(u -> context.getStructSize(u.getShape())).sum(),
                    shapesDataPrefix + "SphereRTC");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getShape() == Shape.TORUS).mapToInt(u -> context.getStructSize(u.getShape())).sum(),
                    shapesDataPrefix + "TorusSDF");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getShape() == Shape.PLANE).mapToInt(u -> context.getStructSize(u.getShape())).sum(),
                    shapesDataPrefix + "PlaneRTC");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getShape() == Shape.SUBTRACTION).mapToInt(u -> context.getStructSize(u.getShape())).sum(),
                    shapesDataPrefix + "SubtractionSDF");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getShape() == Shape.BOX).mapToInt(u -> context.getStructSize(u.getShape())).sum(),
                    shapesDataPrefix + "BoxSDF");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getShape() == Shape.UNION).mapToInt(u -> context.getStructSize(u.getShape())).sum(),
                    shapesDataPrefix + "UnionSDF");
            CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                    allObjects.stream().filter(u -> u.getShape() == Shape.INTERSECTION).mapToInt(u -> context.getStructSize(u.getShape())).sum(),
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

            exhibition(t);
        }

        private void exhibition(double t) {
            Material material = new Material(Material.MATERIAL_REFLECTION, new Vector3d(1,0,1), 1);
            Plane bottom = new Plane(new Vector3d(0,0,-.3), new Vector3d(0,0,.3).normalized(), material);
            add(bottom);
            Plane top = new Plane(new Vector3d(0,0,.3), new Vector3d(0,0,-.3).normalized(), material);
            add(top);

            Plane back = new Plane(new Vector3d(.3,0,0), new Vector3d(-.3,0,0).normalized(),material);
            add(back);
            Plane front = new Plane(new Vector3d(-.3,0,0), new Vector3d(.3,0,0).normalized(),material);
            add(front);

            Plane left = new Plane(new Vector3d(0,-.3,0), new Vector3d(0,.3,0).normalized(),material);
            add(left);
            Plane right = new Plane(new Vector3d(0,.3,0), new Vector3d(0,-.3,0).normalized(),material);
            add(right);

            add(new TorusSDF(new Vector3d(0, -.1, 0), new Vector3d(0, 0, 0),.005f,.03f));

            add(new SubtractionSDF(
                    new Vector3d(0, -.1,.1),
                    new Vector3d(0, 0,0),
                    new TorusSDF( new Vector3d(-.03,0,0),
                            new Vector3d(0, 0,0),.005f,.02f),
                    new BoxSDF(new Vector3d(0,0,0), new Vector3d(0,0,0), new Vector3d(.03,.03,.03))));
            add(new Sphere(0,-.1f,-.1f,.03f));
            add(new BoxSDF(
                    new Vector3d(0,0,-.1),
                    new Vector3d(0,t,t),
                    new Vector3d(.03,.03,.03)));

            add(new UnionSDF(
                    new Vector3d(0, 0,0),
                    new Vector3d(0, t,0),
                    new TorusSDF( new Vector3d(0,0,0),
                            new Vector3d(0, 0, 0),.005f,.03f),
                    new BoxSDF(
                            new Vector3d(0,0,0),
                            new Vector3d(0,0,0),
                            new Vector3d(.01,.01,.01))));

            add(new IntersectionSDF(
                    new Vector3d(0, 0,.1),
                    new Vector3d(0, t,0),
                    new TorusSDF( new Vector3d(0,.03,0),
                            new Vector3d(0, 0, 0),.01f,.03f),
                    new BoxSDF(
                            new Vector3d(0,0,0),
                            new Vector3d(0,0,0),
                            new Vector3d(.03,.03 + .01 * Math.sin(t),.03 + .01 * Math.sin(t)))));

            add(new SubtractionSDF(
                    new Vector3d(0, .1,-.1),
                    new Vector3d(0, 0,0),
                    new UnionSDF(
                            new Vector3d(-.03, 0,0),
                            new Vector3d(t, 0,0),
                            new TorusSDF( new Vector3d(0,0,0),
                                    new Vector3d(0, 0, 0),.005f,.02f),
                            new BoxSDF(
                                    new Vector3d(0,0,0),
                                    new Vector3d(0,0,0),
                                    new Vector3d(.01,.01,.01))),
                    new BoxSDF(new Vector3d(0,0,0), new Vector3d(0,0,0), new Vector3d(.03,.03,.03))));
        }
    }
}
