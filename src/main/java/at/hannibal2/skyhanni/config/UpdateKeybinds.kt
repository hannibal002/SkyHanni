package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.json.Shimmy
import at.hannibal2.skyhanni.utils.system.PlatformUtils

@SkyHanniModule
object UpdateKeybinds {

    var keybinds: MutableSet<String> = mutableSetOf()
    private val logger = SkyHanniLogger("keybind_upgrader")

    private val glfwToSdlKeys: MutableMap<Int, Int> = mutableMapOf()
    private val sdlToGlfwKeys: MutableMap<Int, Int> = mutableMapOf()

    @HandleEvent(priority = HandleEvent.HIGH)
    private fun onConfigLoad(event: ConfigLoadEvent) {
        if (!event.firstLoad) return

        val config = SkyHanniMod.feature
        val lastMcVersion = config.lastMinecraftVersion
        val currentMcVersion = PlatformUtils.MC_VERSION
        if (!config.storage.hasPlayedBefore) {
            logger.log("User has never used SkyHanni before!")
            config.lastMinecraftVersion = currentMcVersion
            return
        }
        if (lastMcVersion == currentMcVersion) return

        when (lastMcVersion) {
            "26.2" if currentMcVersion == "26.3" -> {
                logger.log("Migrating keybinds from 26.2 to 26.3")
                migrateKeybinds(forward = true)
            }
            "26.3" if currentMcVersion == "26.2" -> {
                logger.log("Migrating keybinds from 26.3 to 26.2")
                migrateKeybinds(forward = false)
            }
        }

        config.lastMinecraftVersion = currentMcVersion
    }

    private fun migrateKeybinds(forward: Boolean) {
        createKeyMapping()

        for (keybind in keybinds) {
            migrateKeybind(keybind, forward)
        }
    }

    private fun migrateKeybind(key: String, forward: Boolean) {
        val shimmy = Shimmy(SkyHanniMod.feature, key.split(".")) ?: return
        val value = shimmy.getJson()

        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) return

        val oldKeyCode = value.asInt

        val map = if (forward) {
            glfwToSdlKeys
        } else {
            sdlToGlfwKeys
        }

        if (!map.containsKey(oldKeyCode)) return
        val newKeyCode = map[oldKeyCode]

        shimmy.setJson(ConfigManager.gson.toJsonTree(newKeyCode))
    }

    private fun mapKeyCode(oldKeyCode: Int, newKeyCode: Int) {
        glfwToSdlKeys[oldKeyCode] = newKeyCode
        sdlToGlfwKeys[newKeyCode] = oldKeyCode
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetkeybinds") {
            category = USERS_RESET
            description = "Resets all of your skyhanni keybinds"
            aliases = listOf("shkeybindreset")
            simpleCallback {
                for (keybind in keybinds) {
                    SkyHanniConfigSearchResetCommand.resetCommand(arrayOf("reset", "config.$keybind"))
                }
                ChatUtils.chat("§aSuccessfully reset all SkyHanni Keybinds")
            }
        }
    }

    // Has been prefiltered to not include any keybinds that are the same in both versions
    fun createKeyMapping() {
        mapKeyCode(0, 1) // MOUSE_BUTTON_LEFT
        mapKeyCode(1, 3) // MOD_SHIFT
        mapKeyCode(3, 4) // MOUSE_BUTTON_4
        mapKeyCode(4, 768) // MOD_ALT
        mapKeyCode(5, 6) // MOUSE_BUTTON_6
        mapKeyCode(6, 7) // MOUSE_BUTTON_7
        mapKeyCode(7, 8) // MOUSE_BUTTON_8
        mapKeyCode(8, 3072) // MOD_SUPER
        mapKeyCode(16, 8192) // MOD_CAPS_LOCK
        mapKeyCode(32, 44) // KEY_SPACE (KEYCODE_SPACE)
        mapKeyCode(39, 52) // KEY_APOSTROPHE (KEYCODE_APOSTROPHE)
        mapKeyCode(44, 54) // KEY_COMMA (KEYCODE_COMMA)
        mapKeyCode(46, 55) // KEY_PERIOD (KEYCODE_PERIOD)
        mapKeyCode(47, 56) // KEY_SLASH (KEYCODE_SLASH)
        mapKeyCode(48, 39) // KEY_0 (KEYCODE_0)
        mapKeyCode(49, 30) // KEY_1 (KEYCODE_1)
        mapKeyCode(50, 31) // KEY_2 (KEYCODE_2)
        mapKeyCode(51, 32) // KEY_3 (KEYCODE_3)
        mapKeyCode(52, 33) // KEY_4 (KEYCODE_4)
        mapKeyCode(53, 34) // KEY_5 (KEYCODE_5)
        mapKeyCode(54, 35) // KEY_6 (KEYCODE_6)
        mapKeyCode(55, 36) // KEY_7 (KEYCODE_7)
        mapKeyCode(56, 37) // KEY_8 (KEYCODE_8)
        mapKeyCode(57, 38) // KEY_9 (KEYCODE_9)
        mapKeyCode(59, 51) // KEY_SEMICOLON (KEYCODE_SEMICOLON)
        mapKeyCode(61, 46) // KEY_EQUALS (KEYCODE_EQUALS)
        mapKeyCode(65, 97) // KEY_A (KEYCODE_A)
        mapKeyCode(66, 98) // KEY_B (KEYCODE_B)
        mapKeyCode(67, 99) // KEY_C (KEYCODE_C)
        mapKeyCode(68, 7) // KEY_D (KEYCODE_D)
        mapKeyCode(69, 101) // KEY_E (KEYCODE_E)
        mapKeyCode(70, 102) // KEY_F (KEYCODE_F)
        mapKeyCode(71, 10) // KEY_G (KEYCODE_G)
        mapKeyCode(72, 11) // KEY_H (KEYCODE_H)
        mapKeyCode(73, 12) // KEY_I (KEYCODE_I)
        mapKeyCode(74, 13) // KEY_J (KEYCODE_J)
        mapKeyCode(75, 14) // KEY_K (KEYCODE_K)
        mapKeyCode(76, 108) // KEY_L (KEYCODE_L)
        mapKeyCode(77, 109) // KEY_M (KEYCODE_M)
        mapKeyCode(78, 17) // KEY_N (KEYCODE_N)
        mapKeyCode(79, 111) // KEY_O (KEYCODE_O)
        mapKeyCode(80, 19) // KEY_P (KEYCODE_P)
        mapKeyCode(81, 20) // KEY_Q (KEYCODE_Q)
        mapKeyCode(82, 114) // KEY_R (KEYCODE_R)
        mapKeyCode(83, 22) // KEY_S (KEYCODE_S)
        mapKeyCode(84, 23) // KEY_T (KEYCODE_T)
        mapKeyCode(85, 117) // KEY_U (KEYCODE_U)
        mapKeyCode(86, 118) // KEY_V (KEYCODE_V)
        mapKeyCode(87, 119) // KEY_W (KEYCODE_W)
        mapKeyCode(88, 120) // KEY_X (KEYCODE_X)
        mapKeyCode(89, 121) // KEY_Y (KEYCODE_Y)
        mapKeyCode(90, 122) // KEY_Z (KEYCODE_Z)
        mapKeyCode(91, 47) // KEY_LBRACKET (KEYCODE_LBRACKET)
        mapKeyCode(92, 49) // KEY_BACKSLASH (KEYCODE_BACKSLASH)
        mapKeyCode(93, 48) // KEY_RBRACKET (KEYCODE_RBRACKET)
        mapKeyCode(96, 53) // KEY_GRAVE (KEYCODE_GRAVE)
        mapKeyCode(256, 41) // KEY_ESCAPE (KEYCODE_ESCAPE)
        mapKeyCode(257, 13) // KEY_RETURN (KEYCODE_RETURN)
        mapKeyCode(258, 9) // KEY_TAB (KEYCODE_TAB)
        mapKeyCode(259, 8) // KEY_BACKSPACE (KEYCODE_BACKSPACE)
        mapKeyCode(260, 73) // KEY_INSERT (KEYCODE_INSERT)
        mapKeyCode(261, 127) // KEY_DELETE (KEYCODE_DELETE)
        mapKeyCode(262, 1073741903) // KEY_RIGHT (KEYCODE_RIGHT)
        mapKeyCode(263, 1073741904) // KEY_LEFT (KEYCODE_LEFT)
        mapKeyCode(264, 1073741905) // KEY_DOWN (KEYCODE_DOWN)
        mapKeyCode(265, 1073741906) // KEY_UP (KEYCODE_UP)
        mapKeyCode(266, 1073741899) // KEY_PAGEUP (KEYCODE_PAGEUP)
        mapKeyCode(267, 1073741902) // KEY_PAGEDOWN (KEYCODE_PAGEDOWN)
        mapKeyCode(268, 1073741898) // KEY_HOME (KEYCODE_HOME)
        mapKeyCode(269, 1073741901) // KEY_END (KEYCODE_END)
        mapKeyCode(280, 57) // KEY_CAPSLOCK (KEYCODE_CAPSLOCK)
        mapKeyCode(281, 71) // KEY_SCROLLLOCK (KEYCODE_SCROLLLOCK)
        mapKeyCode(282, 83) // KEY_NUMLOCK (KEYCODE_NUMLOCK)
        mapKeyCode(283, 70) // KEY_PRINTSCREEN (KEYCODE_PRINTSCREEN)
        mapKeyCode(284, 72) // KEY_PAUSE (KEYCODE_PAUSE)
        mapKeyCode(290, 58) // KEY_F1 (KEYCODE_F1)
        mapKeyCode(291, 59) // KEY_F2 (KEYCODE_F2)
        mapKeyCode(292, 60) // KEY_F3 (KEYCODE_F3)
        mapKeyCode(293, 61) // KEY_F4 (KEYCODE_F4)
        mapKeyCode(294, 1073741886) // KEY_F5 (KEYCODE_F5)
        mapKeyCode(295, 63) // KEY_F6 (KEYCODE_F6)
        mapKeyCode(296, 64) // KEY_F7 (KEYCODE_F7)
        mapKeyCode(297, 65) // KEY_F8 (KEYCODE_F8)
        mapKeyCode(298, 66) // KEY_F9 (KEYCODE_F9)
        mapKeyCode(299, 67) // KEY_F10 (KEYCODE_F10)
        mapKeyCode(300, 68) // KEY_F11 (KEYCODE_F11)
        mapKeyCode(301, 69) // KEY_F12 (KEYCODE_F12)
        mapKeyCode(302, 104) // KEY_F13 (KEYCODE_F13)
        mapKeyCode(303, 105) // KEY_F14 (KEYCODE_F14)
        mapKeyCode(304, 106) // KEY_F15 (KEYCODE_F15)
        mapKeyCode(305, 107) // KEY_F16 (KEYCODE_F16)
        mapKeyCode(306, 108) // KEY_F17 (KEYCODE_F17)
        mapKeyCode(307, 109) // KEY_F18 (KEYCODE_F18)
        mapKeyCode(308, 110) // KEY_F19 (KEYCODE_F19)
        mapKeyCode(309, 111) // KEY_F20 (KEYCODE_F20)
        mapKeyCode(310, 112) // KEY_F21 (KEYCODE_F21)
        mapKeyCode(311, 113) // KEY_F22 (KEYCODE_F22)
        mapKeyCode(312, 114) // KEY_F23 (KEYCODE_F23)
        mapKeyCode(313, 115) // KEY_F24 (KEYCODE_F24)
        mapKeyCode(320, 98) // KEY_NUMPAD0 (KEYCODE_NUMPAD0)
        mapKeyCode(321, 89) // KEY_NUMPAD1 (KEYCODE_NUMPAD1)
        mapKeyCode(322, 90) // KEY_NUMPAD2 (KEYCODE_NUMPAD2)
        mapKeyCode(323, 1073741915) // KEY_NUMPAD3 (KEYCODE_NUMPAD3)
        mapKeyCode(324, 92) // KEY_NUMPAD4 (KEYCODE_NUMPAD4)
        mapKeyCode(325, 93) // KEY_NUMPAD5 (KEYCODE_NUMPAD5)
        mapKeyCode(326, 94) // KEY_NUMPAD6 (KEYCODE_NUMPAD6)
        mapKeyCode(327, 95) // KEY_NUMPAD7 (KEYCODE_NUMPAD7)
        mapKeyCode(328, 96) // KEY_NUMPAD8 (KEYCODE_NUMPAD8)
        mapKeyCode(329, 1073741921) // KEY_NUMPAD9 (KEYCODE_NUMPAD9)
        mapKeyCode(330, 220) // KEY_NUMPADCOMMA (KEYCODE_NUMPADCOMMA)
        mapKeyCode(332, 85) // KEY_MULTIPLY (KEYCODE_MULTIPLY)
        mapKeyCode(333, 86) // KEY_SUB (KEYCODE_SUB)
        mapKeyCode(334, 87) // KEY_ADD (KEYCODE_ADD)
        mapKeyCode(335, 1073741912) // KEY_NUMPADENTER (KEYCODE_NUMPADENTER)
        mapKeyCode(336, 103) // KEY_NUMPADEQUALS (KEYCODE_NUMPADEQUALS)
        mapKeyCode(340, 225) // KEY_LSHIFT (KEYCODE_LSHIFT)
        mapKeyCode(341, 1073742048) // KEY_LCONTROL (KEYCODE_LCONTROL)
        mapKeyCode(342, 226) // KEY_LALT (KEYCODE_LALT)
        mapKeyCode(344, 229) // KEY_RSHIFT (KEYCODE_RSHIFT)
        mapKeyCode(345, 1073742052) // KEY_RCONTROL (KEYCODE_RCONTROL)
        mapKeyCode(346, 230) // KEY_RALT (KEYCODE_RALT)
    }
}
