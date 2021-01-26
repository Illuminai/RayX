package com.rayx.win.ex;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;

public interface GDI32Ex extends Library {

    static GDI32Ex INSTANCE = (GDI32Ex) Native.load("GDI32", GDI32Ex.class, W32APIOptions.DEFAULT_OPTIONS);
}
