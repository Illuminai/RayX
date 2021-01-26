package com.rayx.win.winapi;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public class COLORREF extends HANDLE {
    public COLORREF() {

    }

    public COLORREF(Pointer p) {
        super(p);
    }
}