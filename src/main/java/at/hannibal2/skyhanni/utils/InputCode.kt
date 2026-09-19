package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.input.KeyEvent

/**
 * Represents a key or mouse button input code.
 *
 * Intentionally missing "key.keyboard.menu" and "key.keyboard.decimal" due to being changed between 26.2 and 26.3.
 * @property key The corresponding InputConstants.Key for this input code.
 */
@Suppress("unused")
enum class InputCode(
    val key: InputConstants.Key,
) {
    UNKNOWN(InputConstants.UNKNOWN),

    KEY_0(InputConstants.KEY_0),
    KEY_1(InputConstants.KEY_1),
    KEY_2(InputConstants.KEY_2),
    KEY_3(InputConstants.KEY_3),
    KEY_4(InputConstants.KEY_4),
    KEY_5(InputConstants.KEY_5),
    KEY_6(InputConstants.KEY_6),
    KEY_7(InputConstants.KEY_7),
    KEY_8(InputConstants.KEY_8),
    KEY_9(InputConstants.KEY_9),
    KEY_A(InputConstants.KEY_A),
    KEY_B(InputConstants.KEY_B),
    KEY_C(InputConstants.KEY_C),
    KEY_D(InputConstants.KEY_D),
    KEY_E(InputConstants.KEY_E),
    KEY_F(InputConstants.KEY_F),
    KEY_G(InputConstants.KEY_G),
    KEY_H(InputConstants.KEY_H),
    KEY_I(InputConstants.KEY_I),
    KEY_J(InputConstants.KEY_J),
    KEY_K(InputConstants.KEY_K),
    KEY_L(InputConstants.KEY_L),
    KEY_M(InputConstants.KEY_M),
    KEY_N(InputConstants.KEY_N),
    KEY_O(InputConstants.KEY_O),
    KEY_P(InputConstants.KEY_P),
    KEY_Q(InputConstants.KEY_Q),
    KEY_R(InputConstants.KEY_R),
    KEY_S(InputConstants.KEY_S),
    KEY_T(InputConstants.KEY_T),
    KEY_U(InputConstants.KEY_U),
    KEY_V(InputConstants.KEY_V),
    KEY_W(InputConstants.KEY_W),
    KEY_X(InputConstants.KEY_X),
    KEY_Y(InputConstants.KEY_Y),
    KEY_Z(InputConstants.KEY_Z),
    KEY_F1(InputConstants.KEY_F1),
    KEY_F2(InputConstants.KEY_F2),
    KEY_F3(InputConstants.KEY_F3),
    KEY_F4(InputConstants.KEY_F4),
    KEY_F5(InputConstants.KEY_F5),
    KEY_F6(InputConstants.KEY_F6),
    KEY_F7(InputConstants.KEY_F7),
    KEY_F8(InputConstants.KEY_F8),
    KEY_F9(InputConstants.KEY_F9),
    KEY_F10(InputConstants.KEY_F10),
    KEY_F11(InputConstants.KEY_F11),
    KEY_F12(InputConstants.KEY_F12),
    KEY_F13(InputConstants.KEY_F13),
    KEY_F14(InputConstants.KEY_F14),
    KEY_F15(InputConstants.KEY_F15),
    KEY_F16(InputConstants.KEY_F16),
    KEY_F17(InputConstants.KEY_F17),
    KEY_F18(InputConstants.KEY_F18),
    KEY_F19(InputConstants.KEY_F19),
    KEY_F20(InputConstants.KEY_F20),
    KEY_F21(InputConstants.KEY_F21),
    KEY_F22(InputConstants.KEY_F22),
    KEY_F23(InputConstants.KEY_F23),
    KEY_F24(InputConstants.KEY_F24),
    KEY_NUMLOCK(InputConstants.KEY_NUMLOCK),
    KEY_NUMPAD0(InputConstants.KEY_NUMPAD0),
    KEY_NUMPAD1(InputConstants.KEY_NUMPAD1),
    KEY_NUMPAD2(InputConstants.KEY_NUMPAD2),
    KEY_NUMPAD3(InputConstants.KEY_NUMPAD3),
    KEY_NUMPAD4(InputConstants.KEY_NUMPAD4),
    KEY_NUMPAD5(InputConstants.KEY_NUMPAD5),
    KEY_NUMPAD6(InputConstants.KEY_NUMPAD6),
    KEY_NUMPAD7(InputConstants.KEY_NUMPAD7),
    KEY_NUMPAD8(InputConstants.KEY_NUMPAD8),
    KEY_NUMPAD9(InputConstants.KEY_NUMPAD9),
    KEY_NUMPADCOMMA(InputConstants.KEY_NUMPADCOMMA),
    KEY_NUMPADENTER(InputConstants.KEY_NUMPADENTER),
    KEY_NUMPADEQUALS(InputConstants.KEY_NUMPADEQUALS),
    KEY_DOWN(InputConstants.KEY_DOWN),
    KEY_LEFT(InputConstants.KEY_LEFT),
    KEY_RIGHT(InputConstants.KEY_RIGHT),
    KEY_UP(InputConstants.KEY_UP),
    KEY_ADD(InputConstants.KEY_ADD),
    KEY_SUBTRACT("key.keyboard.keypad.subtract"),
    KEY_DIVIDE("key.keyboard.keypad.divide"),
    KEY_APOSTROPHE(InputConstants.KEY_APOSTROPHE),
    KEY_BACKSLASH(InputConstants.KEY_BACKSLASH),
    KEY_COMMA(InputConstants.KEY_COMMA),
    KEY_EQUALS(InputConstants.KEY_EQUALS),
    KEY_GRAVE(InputConstants.KEY_GRAVE),
    KEY_LBRACKET(InputConstants.KEY_LBRACKET),
    KEY_MINUS(InputConstants.KEY_MINUS),
    KEY_MULTIPLY(InputConstants.KEY_MULTIPLY),
    KEY_PERIOD(InputConstants.KEY_PERIOD),
    KEY_RBRACKET(InputConstants.KEY_RBRACKET),
    KEY_SEMICOLON(InputConstants.KEY_SEMICOLON),
    KEY_SLASH(InputConstants.KEY_SLASH),
    KEY_SPACE(InputConstants.KEY_SPACE),
    KEY_TAB(InputConstants.KEY_TAB),
    KEY_LALT(InputConstants.KEY_LALT),
    KEY_LCONTROL(InputConstants.KEY_LCONTROL),
    KEY_LSHIFT(InputConstants.KEY_LSHIFT),
    //~ if < 26.3 'InputConstants.KEY_LGUI' -> 'InputConstants.KEY_LSUPER'
    KEY_LSUPER(InputConstants.KEY_LGUI),
    KEY_RALT(InputConstants.KEY_RALT),
    KEY_RCONTROL(InputConstants.KEY_RCONTROL),
    KEY_RSHIFT(InputConstants.KEY_RSHIFT),
    //~ if < 26.3 'InputConstants.KEY_RGUI' -> 'InputConstants.KEY_RSUPER'
    KEY_RSUPER(InputConstants.KEY_RGUI),
    KEY_RETURN(InputConstants.KEY_RETURN),
    KEY_ESCAPE(InputConstants.KEY_ESCAPE),
    KEY_BACKSPACE(InputConstants.KEY_BACKSPACE),
    KEY_DELETE(InputConstants.KEY_DELETE),
    KEY_END(InputConstants.KEY_END),
    KEY_HOME(InputConstants.KEY_HOME),
    KEY_INSERT(InputConstants.KEY_INSERT),
    KEY_PAGEDOWN(InputConstants.KEY_PAGEDOWN),
    KEY_PAGEUP(InputConstants.KEY_PAGEUP),
    KEY_CAPSLOCK(InputConstants.KEY_CAPSLOCK),
    KEY_PAUSE(InputConstants.KEY_PAUSE),
    KEY_SCROLLLOCK(InputConstants.KEY_SCROLLLOCK),
    KEY_PRINTSCREEN(InputConstants.KEY_PRINTSCREEN),
    KEY_WORLD_1("key.keyboard.world.1"),
    KEY_WORLD_2("key.keyboard.world.2"),
    LEFT_MOUSE(InputConstants.MOUSE_BUTTON_LEFT, MOUSE),
    RIGHT_MOUSE(InputConstants.MOUSE_BUTTON_RIGHT, MOUSE),
    MIDDLE_MOUSE(InputConstants.MOUSE_BUTTON_MIDDLE, MOUSE),
    MOUSE_BUTTON_4(InputConstants.MOUSE_BUTTON_4, MOUSE),
    MOUSE_BUTTON_5(InputConstants.MOUSE_BUTTON_5, MOUSE),
    MOUSE_BUTTON_6(InputConstants.MOUSE_BUTTON_6, MOUSE),
    MOUSE_BUTTON_7(InputConstants.MOUSE_BUTTON_7, MOUSE),
    MOUSE_BUTTON_8(InputConstants.MOUSE_BUTTON_8, MOUSE),
    ;

    //~ if < 26.3 'KEYBOARD' -> 'KEYSYM'
    constructor(value: Int, type: InputConstants.Type = InputConstants.Type.KEYBOARD) : this(type.getOrCreate(value))

    constructor(name: String) : this(InputConstants.getKey(name))

    val value: Int
        get() = key.value

    val displayName: String
        get() = KeyboardManager.getKeyName(key)

    fun isKeyHeld(): Boolean = key.isKeyHeld()
    fun isKeyClicked(): Boolean = key.isKeyClicked()

    fun toKeyIdentifier(): String = key.name

    override fun toString(): String = key.value.toString()

    companion object {
        fun fromKeyIdentifier(identifier: String): InputCode {
            val key = runCatching { InputConstants.getKey(identifier) }.getOrNull()
            return key?.let { fromKey(it) } ?: UNKNOWN
        }

        //~ if < 26.3 'KEYBOARD' -> 'KEYSYM' {
        fun fromKeyCode(keyCode: Int): InputCode =
            fromKey(InputConstants.Type.KEYBOARD.getOrCreate(keyCode))
        //~}

        @JvmStatic
        fun fromKeyEvent(keyEvent: KeyEvent): InputCode =
            fromKey(InputConstants.getKey(keyEvent))

        fun fromMouseButton(mouseButton: Int): InputCode =
            fromKey(InputConstants.Type.MOUSE.getOrCreate(mouseButton))

        // TODO: Optimize
        fun fromKey(key: InputConstants.Key): InputCode =
            entries.firstOrNull { it.key == key } ?: UNKNOWN
    }
}
