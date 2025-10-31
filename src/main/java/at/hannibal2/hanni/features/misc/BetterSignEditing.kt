package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.SignUtils
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.system.PlatformUtils
import net.minecraft.client.Minecraft

@HanniModule
object BetterSignEditing {

    @HandleEvent
    fun onTick() {
        if (!SkyBlockUtils.onHypixel) return
        if (!HanniMod.feature.misc.betterSignEditing) return
        if (!PlatformUtils.IS_LEGACY) return

        val gui = Minecraft.getMinecraft().currentScreen
        SignUtils.checkPaste()
        SignUtils.checkCopying(gui)
        SignUtils.checkDeleting(gui)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(16, "misc.pasteIntoSigns", "misc.betterSignEditing")
    }
}
