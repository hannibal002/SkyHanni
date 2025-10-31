package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConditionalUtils.afterChange
import at.hannibal2.hanni.utils.RenderUtils.renderStrings
import at.hannibal2.hanni.utils.SkyBlockUtils

@HanniModule
object CustomTextBox {

    private val config get() = HanniMod.feature.gui.customTextBox
    private var display = listOf<String>()

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        display = config.text.get().format()

        config.text.afterChange {
            display = format()
        }
    }

    private fun String.format() = replace("&", "§").split("\\n").toList()

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.onlyInGui) return
        if (!isEnabled()) return

        config.position.renderStrings(display, posLabel = "Custom Text Box")
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (config.onlyInGui) return
        if (!isEnabled()) return

        config.position.renderStrings(display, posLabel = "Custom Text Box")
    }

    private fun isEnabled() =
        (SkyBlockUtils.inSkyBlock || OutsideSBFeature.CUSTOM_TEXT_BOX.isSelected()) && config.enabled

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.textBox", "gui.customTextBox")
        event.move(81, "gui.customTextBox.onlyInGUI", "gui.customTextBox.onlyInGui")
    }
}
