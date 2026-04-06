package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object CustomTextBox {

    private val config get() = SkyHanniMod.feature.gui.customTextBox
    private var display = listOf<Renderable>()

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.text.afterChange(init = true) {
            display = config.text.get().format()
        }
    }

    private fun String.format(): List<Renderable> = replace("&", "§").split("\\n").map { Renderable.text(it) }

    @HandleEvent(onlyOnSkyblockOrFeatures = [OutsideSBFeature.CUSTOM_TEXT_BOX])
    fun onChestGuiRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.onlyInGui || !config.enabled) return


        config.position.renderRenderables(display, posLabel = "Custom Text Box")
    }

    @HandleEvent(onlyOnSkyblockOrFeatures = [OutsideSBFeature.CUSTOM_TEXT_BOX])
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (config.onlyInGui || !config.enabled) return

        config.position.renderRenderables(display, posLabel = "Custom Text Box")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.textBox", "gui.customTextBox")
        event.move(81, "gui.customTextBox.onlyInGUI", "gui.customTextBox.onlyInGui")
    }
}
