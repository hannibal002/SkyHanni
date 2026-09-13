package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.platform.InputConstants

@SkyHanniModule
object UpdateKeybinds {
    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        for (keybindPath in SkyHanniConfigSearchResetCommand.allKeybinds) {
            event.transform(147, keybindPath) { element ->
                val oldCode = element.asInt
                val type =
                    if (oldCode in 0 until MouseCompat.NUMBER_OF_MOUSE_BUTTONS) InputConstants.Type.MOUSE else InputConstants.Type.KEYSYM
                val newStringName = type.getOrCreate(oldCode).name
                JsonPrimitive(newStringName)
            }
        }
    }
}
