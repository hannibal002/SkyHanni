package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.afterChange
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CustomTextBox {
    private val config get() = SkyHanniMod.feature.misc.textBox
    private var display = listOf<String>()

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        display = config.text.get().format()

        config.text.afterChange {
            display = format()
        }
    }

    private fun String.format() = replace("&", "§").split("\\n").toList()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GUIOverlayRenderEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.inSkyBlock) return

        config.position.renderStrings(display, posLabel = "Custom Text Box")
    }
}