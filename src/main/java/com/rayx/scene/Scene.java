package com.rayx.scene;

import com.rayx.core.math.Vector3d;
import com.rayx.opencl.CLContext;
import com.rayx.opencl.CLManager;
import com.rayx.scene.material.Material;
import com.rayx.scene.shape.*;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;

public class Scene {
    private final ArrayList<Shape> visibleObjects, allObjects;
    private final String shapesIdentifier, shapesDataPrefix;
    private Camera camera;

    private int currentId;

    public Scene() {
        visibleObjects = new ArrayList<>();
        this.allObjects = new ArrayList<>();
        shapesIdentifier = "shapes" + hashCode();
        shapesDataPrefix = "shapesData" + hashCode();
        camera = new Camera(
                new Vector3d(-.2, 0, 0),
                new Vector3d(0, 0, 0),
                1
        );
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public ArrayList<Shape> getAllObjects() {
        return allObjects;
    }

    public ArrayList<Shape> getVisibleObjects() {
        return visibleObjects;
    }

    public void add(Shape s) {
        visibleObjects.add(s);
    }

    public boolean remove(Shape s) {
        return visibleObjects.remove(s);
    }

    public long enqueueRender(CLContext context, int glTexture, boolean debug, Camera camera, int width, int height) {
        transferShapesToRAM(context);

        /*if (debug) {
            CLManager.testPrintGPUMemory(context, shapesIdentifier,
                    shapesDataPrefix, allObjects);
        }*/

        return CLManager.enqueueRenderKernel(context, glTexture,
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
                    context.getStructSize(-1) * allObjects.size(),
                    shapesIdentifier);


            List<ShapeType> shapes = context.getRegisteredShapes();
            for(int i = 0; i < context.getRegisteredShapes().size(); i++) {
                ShapeType s = shapes.get(i);
                CLManager.allocateMemory(context, CL_MEM_READ_WRITE,
                        allObjects.stream().filter(u -> u.getType() == s).count() * s.getTransferSize(),
                        shapesDataPrefix + s.getDataPointerName());
            }

            CLContext.CLKernel kernel = context.getKernelObject(CLContext.KERNEL_FILL_BUFFER_DATA);
            kernel.setParameter1i(0, allObjects.size());
            kernel.setParameterPointer(1, "inputData");
            kernel.setParameterPointer(2, shapesIdentifier);

            for(int i = 0; i < shapes.size(); i++) {
                kernel.setParameterPointer(i + 3, shapesDataPrefix + shapes.get(i).getDataPointerName());
            }

            kernel.run(new long[]{1}, null);

            context.freeMemoryObject("inputData");
        }
    }

    public void deleteRenderMemory(CLContext context) {
        allObjects.clear();

        //TODO
        context.freeAllMemoryObjects();
    }

    public static class DemoScene extends Scene {
        double t = 0;
        public DemoScene() {
            super();
        }

        @Override
        public long enqueueRender(CLContext context, int glTexture, boolean debug,
                                  Camera camera, int width, int height) {
            exhibition(context);

            return super.enqueueRender(context, glTexture, debug, camera, width, height);
        }

        private void exhibition(CLContext context) {
            //System.out.println(Arrays.toString(context.getRegisteredShapes().stream().map(ShapeType::getShapeName).toArray()));
            //[sphere, torus, plane, box, octahedron, subtraction, union, intersection, mandelbulb]

            if(t != 0) {
                return;
            }

            getVisibleObjects().clear();
            t += 0.01;

            Shape sphere = new Shape(context.getShapeType(0),1, new Vector3d(0,0,1),
                    new Vector3d(0,0,0), Material.reflectionMaterial(new Vector3d(1,0,0), 1));

            Shape box = new Shape(context.getShapeType(3),1, new Vector3d(0,0,0),
                    new Vector3d(0,0,0), Material.reflectionMaterial(new Vector3d(1,0,0), 1));
            box.getProperties().put("dimensions", new Vector3d(1,1,1));

            Shape union = new Shape(context.getShapeType(6),1, new Vector3d(0,0,0),
                    new Vector3d(0,0,0), Material.reflectionMaterial(new Vector3d(1,1,1), 1));
            union.getProperties().put("shape1", box);
            union.getProperties().put("shape2", sphere);
            add(union);
        }
    }
}
