package com.rayx;

import com.rayx.opencl.OpenCLHelper;

import static org.lwjgl.opencl.CL22.*;

public class RayX {

    public static void main(String[] args) {
        long device = OpenCLHelper.getAllAvailableDevices().get(0);
        System.out.println("CL: Name: " + OpenCLHelper.queryDeviceInfo(device, CL_DEVICE_NAME));
    }

}
