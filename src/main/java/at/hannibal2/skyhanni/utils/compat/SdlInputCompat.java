//? if >= 26.3 {
/*package at.hannibal2.skyhanni.utils.compat;

import org.lwjgl.sdl.SDLScancode;

// Native SDL scancodes plus distinct serialized mouse bindings for Minecraft26.3.
public final class SdlInputCompat {
    private SdlInputCompat() {}
    public static final int GLFW_KEY_UNKNOWN = -1;
    public static final int GLFW_MOUSE_BUTTON_LEFT = -100;
    public static final int GLFW_MOUSE_BUTTON_RIGHT = -101;
    public static final int GLFW_MOUSE_BUTTON_MIDDLE = -102;
    public static final int GLFW_KEY_1 = SDLScancode.SDL_SCANCODE_1;
    public static final int GLFW_KEY_2 = SDLScancode.SDL_SCANCODE_2;
    public static final int GLFW_KEY_3 = SDLScancode.SDL_SCANCODE_3;
    public static final int GLFW_KEY_4 = SDLScancode.SDL_SCANCODE_4;
    public static final int GLFW_KEY_5 = SDLScancode.SDL_SCANCODE_5;
    public static final int GLFW_KEY_6 = SDLScancode.SDL_SCANCODE_6;
    public static final int GLFW_KEY_7 = SDLScancode.SDL_SCANCODE_7;
    public static final int GLFW_KEY_8 = SDLScancode.SDL_SCANCODE_8;
    public static final int GLFW_KEY_9 = SDLScancode.SDL_SCANCODE_9;
    public static final int GLFW_KEY_A = SDLScancode.SDL_SCANCODE_A;
    public static final int GLFW_KEY_BACKSPACE = SDLScancode.SDL_SCANCODE_BACKSPACE;
    public static final int GLFW_KEY_C = SDLScancode.SDL_SCANCODE_C;
    public static final int GLFW_KEY_D = SDLScancode.SDL_SCANCODE_D;
    public static final int GLFW_KEY_DELETE = SDLScancode.SDL_SCANCODE_DELETE;
    public static final int GLFW_KEY_DOWN = SDLScancode.SDL_SCANCODE_DOWN;
    public static final int GLFW_KEY_ENTER = SDLScancode.SDL_SCANCODE_RETURN;
    public static final int GLFW_KEY_EQUAL = SDLScancode.SDL_SCANCODE_EQUALS;
    public static final int GLFW_KEY_ESCAPE = SDLScancode.SDL_SCANCODE_ESCAPE;
    public static final int GLFW_KEY_F = SDLScancode.SDL_SCANCODE_F;
    public static final int GLFW_KEY_G = SDLScancode.SDL_SCANCODE_G;
    public static final int GLFW_KEY_H = SDLScancode.SDL_SCANCODE_H;
    public static final int GLFW_KEY_I = SDLScancode.SDL_SCANCODE_I;
    public static final int GLFW_KEY_K = SDLScancode.SDL_SCANCODE_K;
    public static final int GLFW_KEY_KP_ADD = SDLScancode.SDL_SCANCODE_KP_PLUS;
    public static final int GLFW_KEY_KP_ENTER = SDLScancode.SDL_SCANCODE_KP_ENTER;
    public static final int GLFW_KEY_KP_SUBTRACT = SDLScancode.SDL_SCANCODE_KP_MINUS;
    public static final int GLFW_KEY_LEFT = SDLScancode.SDL_SCANCODE_LEFT;
    public static final int GLFW_KEY_LEFT_ALT = SDLScancode.SDL_SCANCODE_LALT;
    public static final int GLFW_KEY_LEFT_CONTROL = SDLScancode.SDL_SCANCODE_LCTRL;
    public static final int GLFW_KEY_LEFT_SHIFT = SDLScancode.SDL_SCANCODE_LSHIFT;
    public static final int GLFW_KEY_LEFT_SUPER = SDLScancode.SDL_SCANCODE_LGUI;
    public static final int GLFW_KEY_M = SDLScancode.SDL_SCANCODE_M;
    public static final int GLFW_KEY_MINUS = SDLScancode.SDL_SCANCODE_MINUS;
    public static final int GLFW_KEY_N = SDLScancode.SDL_SCANCODE_N;
    public static final int GLFW_KEY_O = SDLScancode.SDL_SCANCODE_O;
    public static final int GLFW_KEY_R = SDLScancode.SDL_SCANCODE_R;
    public static final int GLFW_KEY_RIGHT = SDLScancode.SDL_SCANCODE_RIGHT;
    public static final int GLFW_KEY_RIGHT_ALT = SDLScancode.SDL_SCANCODE_RALT;
    public static final int GLFW_KEY_RIGHT_CONTROL = SDLScancode.SDL_SCANCODE_RCTRL;
    public static final int GLFW_KEY_RIGHT_SHIFT = SDLScancode.SDL_SCANCODE_RSHIFT;
    public static final int GLFW_KEY_RIGHT_SUPER = SDLScancode.SDL_SCANCODE_RGUI;
    public static final int GLFW_KEY_S = SDLScancode.SDL_SCANCODE_S;
    public static final int GLFW_KEY_SPACE = SDLScancode.SDL_SCANCODE_SPACE;
    public static final int GLFW_KEY_TAB = SDLScancode.SDL_SCANCODE_TAB;
    public static final int GLFW_KEY_UP = SDLScancode.SDL_SCANCODE_UP;
    public static final int GLFW_KEY_V = SDLScancode.SDL_SCANCODE_V;
    public static final int GLFW_KEY_W = SDLScancode.SDL_SCANCODE_W;
    public static final int GLFW_KEY_X = SDLScancode.SDL_SCANCODE_X;
    public static final int GLFW_KEY_Y = SDLScancode.SDL_SCANCODE_Y;
    public static final int GLFW_KEY_Z = SDLScancode.SDL_SCANCODE_Z;
}
*///?}
