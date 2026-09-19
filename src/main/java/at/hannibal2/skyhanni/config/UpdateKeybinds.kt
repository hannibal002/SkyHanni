package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import com.google.gson.JsonPrimitive

@SkyHanniModule
object UpdateKeybinds {
    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        if (event.oldVersion >= 147) {
            return
        }
        for (keybindPath in SkyHanniConfigSearchResetCommand.allKeybinds) {
            event.transform(147, keybindPath) { element ->
                val oldCode = element.asInt
                val newStringName = glfwKeyName(oldCode) ?: run {
                    val defaultValue = SkyHanniConfigSearchResetCommand.getDefaultValue("config.$keybindPath") as Int
                    glfwKeyName(defaultValue) ?: glfwKeyName(-1)
                }
                JsonPrimitive(newStringName)
            }
        }
    }

    // https://github.com/Polyfrost/OneConfig/blob/feat/26.3/minecraft/src/main/java/org/polyfrost/oneconfig/api/ui/v1/keybind/internal/MinecraftKeybindCodec.java
    // Commented out the 2 keycodes that changed from 26.2 to 26.3 since I don't want a Minecraft version migration here.
    private fun glfwKeyName(code: Int): String? {
        if (code == -1) return "key.keyboard.unknown"
        if (code in 3..7) return "key.mouse." + (code + 1)
        if (code in 48..57) return "key.keyboard." + code.toChar()
        if (code in 65..90) return "key.keyboard." + (code + 32).toChar()
        if (code in 290..314) return "key.keyboard.f" + (code - 289)
        if (code in 320..329) return "key.keyboard.keypad." + (code - 320)
        return when (code) {
            0 -> "key.mouse.left"
            1 -> "key.mouse.right"
            2 -> "key.mouse.middle"
            32 -> "key.keyboard.space"
            39 -> "key.keyboard.apostrophe"
            44 -> "key.keyboard.comma"
            45 -> "key.keyboard.minus"
            46 -> "key.keyboard.period"
            47 -> "key.keyboard.slash"
            59 -> "key.keyboard.semicolon"
            61 -> "key.keyboard.equal"
            91 -> "key.keyboard.left.bracket"
            92 -> "key.keyboard.backslash"
            93 -> "key.keyboard.right.bracket"
            96 -> "key.keyboard.grave.accent"
            161 -> "key.keyboard.world.1"
            162 -> "key.keyboard.world.2"
            256 -> "key.keyboard.escape"
            257 -> "key.keyboard.enter"
            258 -> "key.keyboard.tab"
            259 -> "key.keyboard.backspace"
            260 -> "key.keyboard.insert"
            261 -> "key.keyboard.delete"
            262 -> "key.keyboard.right"
            263 -> "key.keyboard.left"
            264 -> "key.keyboard.down"
            265 -> "key.keyboard.up"
            266 -> "key.keyboard.page.up"
            267 -> "key.keyboard.page.down"
            268 -> "key.keyboard.home"
            269 -> "key.keyboard.end"
            280 -> "key.keyboard.caps.lock"
            281 -> "key.keyboard.scroll.lock"
            282 -> "key.keyboard.num.lock"
            283 -> "key.keyboard.print.screen"
            284 -> "key.keyboard.pause"
//            330 -> "key.keyboard.keypad.period"
            331 -> "key.keyboard.keypad.divide"
            332 -> "key.keyboard.keypad.multiply"
            333 -> "key.keyboard.keypad.subtract"
            334 -> "key.keyboard.keypad.add"
            335 -> "key.keyboard.keypad.enter"
            336 -> "key.keyboard.keypad.equal"
            340 -> "key.keyboard.left.shift"
            341 -> "key.keyboard.left.control"
            342 -> "key.keyboard.left.alt"
            343 -> "key.keyboard.left.win"
            344 -> "key.keyboard.right.shift"
            345 -> "key.keyboard.right.control"
            346 -> "key.keyboard.right.alt"
            347 -> "key.keyboard.right.win"
            // 348 -> "key.keyboard.application"
            else -> null
        }
    }
}
