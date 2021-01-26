package com.rayx.win;


import java.util.concurrent.atomic.AtomicInteger;

public class DecorationProperties {
    private static AtomicInteger titleBarHeight = new AtomicInteger(19);
    private static AtomicInteger controlBoxWidth = new AtomicInteger(150);
    private static AtomicInteger iconWidth = new AtomicInteger(20);
    private static AtomicInteger extraLeftReservedWidth = new AtomicInteger(0);
    private static AtomicInteger extraRightReservedWidth = new AtomicInteger(0);
    private static AtomicInteger maximizedWindowFrameThickness = new AtomicInteger(10);
    private static AtomicInteger frameResizeBorderThickness = new AtomicInteger(1);
    private static AtomicInteger frameBorderThickness = new AtomicInteger(2);

    public static int getControlBoxWidth() {
        return controlBoxWidth.get();
    }

    public static void setControlBoxWidth(int value) {
        controlBoxWidth.set(value);
    }

    public static int getIconWidth() {
        return iconWidth.get();
    }

    public static void setIconWidth(int value) {
        iconWidth.set(value);
    }

    public static int getExtraLeftReservedWidth() {
        return extraLeftReservedWidth.get();
    }

    public static void setExtraLeftReservedWidth(int value) {
        extraLeftReservedWidth.set(value);
    }

    public static int getExtraRightReservedWidth() {
        return extraRightReservedWidth.get();
    }

    public static void setExtraRightReservedWidth(int value) {
        extraRightReservedWidth.set(value);
    }

    public static int getMaximizedWindowFrameThickness() {
        return maximizedWindowFrameThickness.get();
    }

    public static void setMaximizedWindowFrameThickness(int maximizedWindowFrameThickness) {
        DecorationProperties.maximizedWindowFrameThickness.set(maximizedWindowFrameThickness);
    }

    public static int getTitleBarHeight() {
        return titleBarHeight.get();
    }

    public static void setTitleBarHeight(int value) {
        DecorationProperties.titleBarHeight.set(value);
    }

    public static int getFrameResizeBorderThickness() {
        return frameResizeBorderThickness.get();
    }

    public static void setFrameResizeBorderThickness(int value) {
        DecorationProperties.frameResizeBorderThickness.set(value);
    }

    public static int getFrameBorderThickness() {
        return frameBorderThickness.get();
    }

    public static void setFrameBorderThickness(int value) {
        DecorationProperties.frameBorderThickness.set(value);
    }
}