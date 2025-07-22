package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SignUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.client.Minecraft

@SkyHanniModule
object BetterSignEditing {

    @HandleEvent
    fun onTick() {
        if (!SkyBlockUtils.onHypixel) return
        if (!SkyHanniMod.feature.misc.betterSignEditing) return
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
