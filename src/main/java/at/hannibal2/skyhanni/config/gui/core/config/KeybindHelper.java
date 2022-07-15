package at.hannibal2.skyhanni.config.gui.core.config;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeybindHelper {

    public static String getKeyName(int keyCode) {
        if (keyCode == 0) {
            return "NONE";
        } else if (keyCode < 0) {
            return "Button " + (keyCode + 101);
        } else {
            String keyName = Keyboard.getKeyName(keyCode);
            if (keyName == null) {
                keyName = "???";
            } else if (keyName.equalsIgnoreCase("LMENU")) {
                keyName = "LALT";
            } else if (keyName.equalsIgnoreCase("RMENU")) {
                keyName = "RALT";
            }
            return keyName;
        }
    }

    public static boolean isKeyValid(int keyCode) {
        return keyCode != 0;
    }

    public static boolean isKeyDown(int keyCode) {
        if (!isKeyValid(keyCode)) {
            return false;
        } else if (keyCode < 0) {
            return Mouse.isButtonDown(keyCode + 100);
        } else {
            return Keyboard.isKeyDown(keyCode);
        }
    }

    public static boolean isKeyPressed(int keyCode) {
        if (!isKeyValid(keyCode)) {
            return false;
        } else if (keyCode < 0) {
            return Mouse.getEventButtonState() && Mouse.getEventButton() == keyCode + 100;
        } else {
            return Keyboard.getEventKeyState() && Keyboard.getEventKey() == keyCode;
        }
    }
}
