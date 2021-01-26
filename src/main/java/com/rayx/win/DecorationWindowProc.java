package com.rayx.win;

import com.rayx.win.ex.User32Ex;
import com.rayx.win.winapi.MARGINS;
import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import static com.sun.jna.platform.win32.WinUser.*;

public class DecorationWindowProc implements WinUser.WindowProc {

    final int WM_NCCALCSIZE = 0x0083;
    final int WM_NCHITTEST = 0x0084;
    final int WM_SETCURSOR = 0x0020;
    final int WM_ACTIVATE = 0x0006;
    final int WM_ERASEBKGND = 0x0014;

    final User32Ex INSTANCEEx;
    WinDef.HWND hwnd = new WinDef.HWND();
    BaseTSD.LONG_PTR defWndProc;

    NativeLibrary dwm;
    NativeLibrary wingdi;

    public DecorationWindowProc() {
        INSTANCEEx = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

        dwm = NativeLibrary.getInstance("dwmapi");
        wingdi = NativeLibrary.getInstance("wingdi");
    }

    public void init(WinDef.HWND hwnd) {
        this.hwnd = hwnd;
        if (is64Bit())
            defWndProc = INSTANCEEx.SetWindowLongPtr(hwnd, User32Ex.GWLP_WNDPROC, this);
        else
            defWndProc = INSTANCEEx.SetWindowLong(hwnd, User32Ex.GWLP_WNDPROC, this);

        MARGINS margins = new MARGINS();
        margins.cxLeftWidth = 0;
        margins.cxRightWidth = 0;
        margins.cyBottomHeight = 0;
        margins.cyTopHeight = 1;
        Function extendFrameFunc = dwm.getFunction("DwmExtendFrameIntoClientArea");
        extendFrameFunc.invoke(new Object[]{hwnd, margins});

        RECT rcWindow = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rcWindow);

        INSTANCEEx.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
                SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
    }

    public void init(long hwnd) {
        HWND nativeHwnd = new WinDef.HWND();
        nativeHwnd.setPointer(new Pointer(hwnd));
        init(nativeHwnd);
    }

    @Override
    public LRESULT callback(HWND hwnd, int uMsg, WPARAM wparam, LPARAM lparam) {
        LRESULT lresult;
        switch (uMsg) {
            case WM_ERASEBKGND:
                /*RECT rc = new RECT();
                User32.INSTANCE.GetClientRect(hwnd, rc);
                IntByReference ir = new IntByReference(0x00ff00ff);
                Function createSolidBrush = wingdi.getFunction("CreateSolidBrush");
                HBRUSH brush = (HBRUSH) createSolidBrush.invoke(HBRUSH.class, new Object[]{ir});
                Function fillRect = wingdi.getFunction("FillRect");
                fillRect.invoke(new Object[]{

                });*/
            case WM_NCCALCSIZE:
                /*if (wparam.intValue() == 0){
                    INSTANCEEx.SetWindowLong(hwnd, DWL_MSGRESULT,0);
                    return new LRESULT(1);
                }*/
                return new LRESULT(0);
            case WM_NCHITTEST:
                lresult = this.BorderLessHitTest(hwnd, uMsg, wparam, lparam);
                if (lresult.intValue() == new LRESULT(0).intValue()) {
                    return INSTANCEEx.CallWindowProc(defWndProc, hwnd, uMsg, wparam, lparam);
                }
                return lresult;
            case WM_DESTROY:
                if (is64Bit())
                    INSTANCEEx.SetWindowLongPtr(hwnd, User32Ex.GWLP_WNDPROC, defWndProc);
                else
                    INSTANCEEx.SetWindowLong(hwnd, User32Ex.GWLP_WNDPROC, defWndProc);
                return new LRESULT(0);
            default:
                lresult = INSTANCEEx.CallWindowProc(defWndProc, hwnd, uMsg, wparam, lparam);
                return lresult;
        }
    }

    LRESULT BorderLessHitTest(HWND hWnd, int message, WPARAM wParam, LPARAM lParam) {
        WinDef.POINT ptMouse = new WinDef.POINT();
        User32.INSTANCE.GetCursorPos(ptMouse);

        RECT rcWindow = new RECT();
        User32.INSTANCE.GetWindowRect(hWnd, rcWindow);

        RECT rcFrame = new RECT();
        User32.INSTANCE.AdjustWindowRectEx(rcFrame, new DWORD(WS_OVERLAPPEDWINDOW & ~WS_CAPTION), new BOOL(false), null);

        int uRow = 1, uCol = 1;
        boolean fOnResizeBorder = false, fOnFrameDrag = false;

        int borderOffset = DecorationProperties.getMaximizedWindowFrameThickness();
        int borderThickness = DecorationProperties.getFrameResizeBorderThickness();
        int topOffset = DecorationProperties.getTitleBarHeight() == 0 ? borderThickness : DecorationProperties.getTitleBarHeight();
        if (ptMouse.y >= rcWindow.top && ptMouse.y < rcWindow.top + topOffset + borderOffset) {
            fOnResizeBorder = (ptMouse.y < (rcWindow.top + borderThickness));  // Top Resizing
            if (!fOnResizeBorder) {
                fOnFrameDrag = (ptMouse.y <= rcWindow.top + DecorationProperties.getTitleBarHeight() + borderOffset)
                        && (ptMouse.x < (rcWindow.right - (DecorationProperties.getControlBoxWidth()
                        + borderOffset + DecorationProperties.getExtraRightReservedWidth())))
                        && (ptMouse.x > (rcWindow.left + DecorationProperties.getIconWidth()
                        + borderOffset + DecorationProperties.getExtraLeftReservedWidth()));
            }
            uRow = 0; // Top Resizing or Caption Moving
        } else if (ptMouse.y < rcWindow.bottom && ptMouse.y >= rcWindow.bottom - borderThickness)
            uRow = 2; // Bottom Resizing
        if (ptMouse.x >= rcWindow.left && ptMouse.x < rcWindow.left + borderThickness)
            uCol = 0; // Left Resizing
        else if (ptMouse.x < rcWindow.right && ptMouse.x >= rcWindow.right - borderThickness)
            uCol = 2; // Right Resizing

        final int HTTOPLEFT = 13, HTTOP = 12, HTCAPTION = 2, HTTOPRIGHT = 14, HTLEFT = 10, HTNOWHERE = 0,
                HTRIGHT = 11, HTBOTTOMLEFT = 16, HTBOTTOM = 15, HTBOTTOMRIGHT = 17, HTSYSMENU = 3;

        int[][] hitTests = {
                {HTTOPLEFT, fOnResizeBorder ? HTTOP : fOnFrameDrag ? HTCAPTION : HTNOWHERE, HTTOPRIGHT},
                {HTLEFT, HTNOWHERE, HTRIGHT},
                {HTBOTTOMLEFT, HTBOTTOM, HTBOTTOMRIGHT},
        };

        return new LRESULT(hitTests[uRow][uCol]);
    }

    public static final boolean is64Bit() {
        String model = System.getProperty("sun.arch.data.model",
                System.getProperty("com.ibm.vm.bitmode"));
        if (model != null) {
            return "64".equals(model);
        }
        return false;
    }

}
