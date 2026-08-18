package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
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

    private var hasUpdated = false

    @HandleEvent(priority = HandleEvent.HIGH)
    private fun onConfigLoad() {
        if (hasUpdated) return
        hasUpdated = true

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
    private fun createKeyMapping() {
        mapKeyCode(65, 97)
        mapKeyCode(66, 98)
        mapKeyCode(67, 99)
        mapKeyCode(68, 100)
        mapKeyCode(69, 101)
        mapKeyCode(70, 102)
        mapKeyCode(71, 103)
        mapKeyCode(72, 104)
        mapKeyCode(73, 105)
        mapKeyCode(74, 106)
        mapKeyCode(75, 107)
        mapKeyCode(76, 108)
        mapKeyCode(77, 109)
        mapKeyCode(78, 110)
        mapKeyCode(79, 111)
        mapKeyCode(80, 112)
        mapKeyCode(81, 113)
        mapKeyCode(82, 114)
        mapKeyCode(83, 115)
        mapKeyCode(84, 116)
        mapKeyCode(85, 117)
        mapKeyCode(86, 118)
        mapKeyCode(87, 119)
        mapKeyCode(88, 120)
        mapKeyCode(89, 121)
        mapKeyCode(90, 122)
        mapKeyCode(256, 27)
        mapKeyCode(257, 13)
        mapKeyCode(258, 9)
        mapKeyCode(259, 8)
        mapKeyCode(260, 1073741897)
        mapKeyCode(261, 127)
        mapKeyCode(262, 1073741903)
        mapKeyCode(263, 1073741904)
        mapKeyCode(264, 1073741905)
        mapKeyCode(265, 1073741906)
        mapKeyCode(266, 1073741899)
        mapKeyCode(267, 1073741902)
        mapKeyCode(268, 1073741898)
        mapKeyCode(269, 1073741901)
        mapKeyCode(280, 1073741881)
        mapKeyCode(281, 1073741895)
        mapKeyCode(282, 1073741907)
        mapKeyCode(283, 1073741894)
        mapKeyCode(284, 1073741896)
        mapKeyCode(290, 1073741882)
        mapKeyCode(291, 1073741883)
        mapKeyCode(292, 1073741884)
        mapKeyCode(293, 1073741885)
        mapKeyCode(294, 1073741886)
        mapKeyCode(295, 1073741887)
        mapKeyCode(296, 1073741888)
        mapKeyCode(297, 1073741889)
        mapKeyCode(298, 1073741890)
        mapKeyCode(299, 1073741891)
        mapKeyCode(300, 1073741892)
        mapKeyCode(301, 1073741893)
        mapKeyCode(302, 1073741928)
        mapKeyCode(303, 1073741929)
        mapKeyCode(304, 1073741930)
        mapKeyCode(305, 1073741931)
        mapKeyCode(306, 1073741932)
        mapKeyCode(307, 1073741933)
        mapKeyCode(308, 1073741934)
        mapKeyCode(309, 1073741935)
        mapKeyCode(310, 1073741936)
        mapKeyCode(311, 1073741937)
        mapKeyCode(312, 1073741938)
        mapKeyCode(313, 1073741939)
        mapKeyCode(314, 1073741940)
        mapKeyCode(320, 1073741922)
        mapKeyCode(321, 1073741923)
        mapKeyCode(322, 1073741924)
        mapKeyCode(323, 1073741925)
        mapKeyCode(324, 1073741926)
        mapKeyCode(325, 1073741927)
        mapKeyCode(326, 1073741928)
        mapKeyCode(327, 1073741929)
        mapKeyCode(328, 1073741930)
        mapKeyCode(329, 1073741931)
        mapKeyCode(330, 1073741923)
        mapKeyCode(331, 1073741908)
        mapKeyCode(332, 1073741909)
        mapKeyCode(333, 1073741910)
        mapKeyCode(334, 1073741911)
        mapKeyCode(335, 1073741912)
        mapKeyCode(340, 1073742049)
        mapKeyCode(341, 1073742048)
        mapKeyCode(342, 1073742050)
        mapKeyCode(343, 1073742051)
        mapKeyCode(344, 1073742053)
        mapKeyCode(345, 1073742052)
        mapKeyCode(346, 1073742054)
        mapKeyCode(347, 1073742055)
    }
}
