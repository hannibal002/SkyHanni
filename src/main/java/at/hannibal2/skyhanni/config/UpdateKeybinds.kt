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

    private val olderToNewerKeys: MutableMap<Int, Int> = mutableMapOf()
    private val newerToOlderKeys: MutableMap<Int, Int> = mutableMapOf()

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
                createKeyMapping()
                migrateKeybinds(olderToNewerKeys)
            }
            "26.3" if currentMcVersion == "26.2" -> {
                logger.log("Migrating keybinds from 26.3 to 26.2")
                createKeyMapping()
                migrateKeybinds(newerToOlderKeys)
            }
        }

        config.lastMinecraftVersion = currentMcVersion
    }

    private fun migrateKeybinds(map: Map<Int, Int>) {
        for (keybind in keybinds) {
            migrateKeybind(keybind, map)
        }
    }

    private fun migrateKeybind(key: String, map: Map<Int, Int>) {
        val shimmy = Shimmy(SkyHanniMod.feature, key.split(".")) ?: return
        val value = shimmy.getJson()

        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) return

        val oldKeyCode = value.asInt

        if (!map.containsKey(oldKeyCode)) return
        val newKeyCode = map[oldKeyCode]

        shimmy.setJson(ConfigManager.gson.toJsonTree(newKeyCode))
    }

    private fun mapKeyCode(oldKeyCode: Int, newKeyCode: Int) {
        olderToNewerKeys[oldKeyCode] = newKeyCode
        newerToOlderKeys[newKeyCode] = oldKeyCode
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
    @Suppress("LongMethod")
    private fun createKeyMapping() {
        mapKeyCode(-1, 0) // UNKNOWN
        mapKeyCode(0, 1) // MOUSE_LEFT
        mapKeyCode(1, 3) // MOUSE_RIGHT
        mapKeyCode(32, 44) // SPACE
        mapKeyCode(39, 52) // APOSTROPHE
        mapKeyCode(44, 54) // COMMA
        mapKeyCode(46, 55) // PERIOD
        mapKeyCode(47, 56) // SLASH
        mapKeyCode(48, 39) // 0
        mapKeyCode(49, 30) // 1
        mapKeyCode(50, 31) // 2
        mapKeyCode(51, 32) // 3
        mapKeyCode(52, 33) // 4
        mapKeyCode(53, 34) // 5
        mapKeyCode(54, 35) // 6
        mapKeyCode(55, 36) // 7
        mapKeyCode(56, 37) // 8
        mapKeyCode(57, 38) // 9
        mapKeyCode(59, 51) // SEMICOLON
        mapKeyCode(61, 46) // EQUAL
        mapKeyCode(65, 4) // A
        mapKeyCode(66, 5) // B
        mapKeyCode(67, 6) // C
        mapKeyCode(68, 7) // D
        mapKeyCode(69, 8) // E
        mapKeyCode(70, 9) // F
        mapKeyCode(71, 10) // G
        mapKeyCode(72, 11) // H
        mapKeyCode(73, 12) // I
        mapKeyCode(74, 13) // J
        mapKeyCode(75, 14) // K
        mapKeyCode(76, 15) // L
        mapKeyCode(77, 16) // M
        mapKeyCode(78, 17) // N
        mapKeyCode(79, 18) // O
        mapKeyCode(80, 19) // P
        mapKeyCode(81, 20) // Q
        mapKeyCode(82, 21) // R
        mapKeyCode(83, 22) // S
        mapKeyCode(84, 23) // T
        mapKeyCode(85, 24) // U
        mapKeyCode(86, 25) // V
        mapKeyCode(87, 26) // W
        mapKeyCode(88, 27) // X
        mapKeyCode(89, 28) // Y
        mapKeyCode(90, 29) // Z
        mapKeyCode(91, 47) // LEFT_BRACKET
        mapKeyCode(92, 49) // BACKSLASH
        mapKeyCode(93, 48) // RIGHT_BRACKET
        mapKeyCode(96, 53) // GRAVE_ACCENT
        mapKeyCode(161, 100) // WORLD_1
        mapKeyCode(162, 50) // WORLD_2
        mapKeyCode(256, 41) // ESCAPE
        mapKeyCode(257, 40) // ENTER
        mapKeyCode(258, 43) // TAB
        mapKeyCode(259, 42) // BACKSPACE
        mapKeyCode(260, 73) // INSERT
        mapKeyCode(261, 76) // DELETE
        mapKeyCode(262, 79) // RIGHT
        mapKeyCode(263, 80) // LEFT
        mapKeyCode(264, 81) // DOWN
        mapKeyCode(265, 82) // UP
        mapKeyCode(266, 75) // PAGE_UP
        mapKeyCode(267, 78) // PAGE_DOWN
        mapKeyCode(268, 74) // HOME
        mapKeyCode(269, 77) // END
        mapKeyCode(280, 57) // CAPS_LOCK
        mapKeyCode(281, 71) // SCROLL_LOCK
        mapKeyCode(282, 83) // NUM_LOCK
        mapKeyCode(283, 70) // PRINT_SCREEN
        mapKeyCode(284, 72) // PAUSE
        mapKeyCode(290, 58) // F1
        mapKeyCode(291, 59) // F2
        mapKeyCode(292, 60) // F3
        mapKeyCode(293, 61) // F4
        mapKeyCode(294, 62) // F5
        mapKeyCode(295, 63) // F6
        mapKeyCode(296, 64) // F7
        mapKeyCode(297, 65) // F8
        mapKeyCode(298, 66) // F9
        mapKeyCode(299, 67) // F10
        mapKeyCode(300, 68) // F11
        mapKeyCode(301, 69) // F12
        mapKeyCode(302, 104) // F13
        mapKeyCode(303, 105) // F14
        mapKeyCode(304, 106) // F15
        mapKeyCode(305, 107) // F16
        mapKeyCode(306, 108) // F17
        mapKeyCode(307, 109) // F18
        mapKeyCode(308, 110) // F19
        mapKeyCode(309, 111) // F20
        mapKeyCode(310, 112) // F21
        mapKeyCode(311, 113) // F22
        mapKeyCode(312, 114) // F23
        mapKeyCode(313, 115) // F24
        mapKeyCode(320, 98) // KEYPAD_0
        mapKeyCode(321, 89) // KEYPAD_1
        mapKeyCode(322, 90) // KEYPAD_2
        mapKeyCode(323, 91) // KEYPAD_3
        mapKeyCode(324, 92) // KEYPAD_4
        mapKeyCode(325, 93) // KEYPAD_5
        mapKeyCode(326, 94) // KEYPAD_6
        mapKeyCode(327, 95) // KEYPAD_7
        mapKeyCode(328, 96) // KEYPAD_8
        mapKeyCode(329, 97) // KEYPAD_9
        mapKeyCode(330, 220) // KEYPAD_DECIMAL
        mapKeyCode(331, 84) // KEYPAD_DIVIDE
        mapKeyCode(332, 85) // KEYPAD_MULTIPLY
        mapKeyCode(333, 86) // KEYPAD_SUBTRACT
        mapKeyCode(334, 87) // KEYPAD_ADD
        mapKeyCode(335, 88) // KEYPAD_ENTER
        mapKeyCode(336, 103) // KEYPAD_EQUAL
        mapKeyCode(340, 225) // LEFT_SHIFT
        mapKeyCode(341, 224) // LEFT_CONTROL
        mapKeyCode(342, 226) // LEFT_ALT
        mapKeyCode(343, 227) // LEFT_WIN
        mapKeyCode(344, 229) // RIGHT_SHIFT
        mapKeyCode(345, 228) // RIGHT_CONTROL
        mapKeyCode(346, 230) // RIGHT_ALT
        mapKeyCode(347, 231) // RIGHT_WIN
        mapKeyCode(348, 118) // MENU
    }
}
