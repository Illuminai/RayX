package com.rayx.shape;

public class Vector3d {
    private final double x, y, z;

    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vector3d normalized() {
        double l = length();
        if(l != 0) {
            return new Vector3d(x / l, y / l, z / l);
        } else {
            //Return null vector
            return this;
        }
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
