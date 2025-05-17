package at.hannibal2.skyhanni.features.garden.farmingsettings

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SignUtils.isMousematSign
import at.hannibal2.skyhanni.utils.SignUtils.isRancherSign
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.gui.inventory.GuiEditSign

@SkyHanniModule
object FarmingSettings {

    private val config get() = GardenApi.config.farmingSettings

    private var display = listOf<Renderable>()

    @HandleEvent
    fun onGuiOpen(event: GuiScreenOpenEvent) {
        if (!isShortcutGUIEnabled()) return
        val gui = event.gui as? GuiEditSign ?: return
        if (!gui.isRancherSign() && !gui.isMousematSign()) return

        display = FarmingSettingsAPI.createDisplay(gui)
    }

    @HandleEvent
    fun onGuiRender(event: ScreenDrawnEvent) {
        if (!isShortcutGUIEnabled()) return
        val gui = event.gui as? GuiEditSign ?: return
        if (!gui.isRancherSign() && !gui.isMousematSign()) return

        config.signPosition.renderRenderables(
            display,
            posLabel = "Optimal Settings Overlay",
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (GardenApi.hideExtraGuis() || (!config.warning && !config.showOnHUD)) return

        if (config.showOnHUD) config.pos.renderRenderable(FarmingSettingsAPI.createStatus(), posLabel = "Garden Optimal Settings")
        if (config.warning && config.warningTypes.isNotEmpty()) FarmingSettingsAPI.handleWarning()
    }

    private fun isShortcutGUIEnabled() = GardenApi.inGarden() && config.shortcutGUI

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.optimalSpeedEnabled", "garden.optimalSpeeds.enabled")
        event.move(3, "garden.optimalSpeedWarning", "garden.optimalSpeeds.warning")
        event.move(3, "garden.optimalSpeedSignEnabled", "garden.optimalSpeeds.signEnabled")
        event.move(3, "garden.optimalSpeedSignPosition", "garden.optimalSpeeds.signPosition")
        event.move(3, "garden.optimalSpeedPos", "garden.optimalSpeeds.pos")
        event.move(3, "garden.optimalSpeedCustom.wheat", "garden.optimalSpeeds.customSpeed.wheat")
        event.move(3, "garden.optimalSpeedCustom.carrot", "garden.optimalSpeeds.customSpeed.carrot")
        event.move(3, "garden.optimalSpeedCustom.potato", "garden.optimalSpeeds.customSpeed.potato")
        event.move(3, "garden.optimalSpeedCustom.netherWart", "garden.optimalSpeeds.customSpeed.netherWart")
        event.move(3, "garden.optimalSpeedCustom.pumpkin", "garden.optimalSpeeds.customSpeed.pumpkin")
        event.move(3, "garden.optimalSpeedCustom.melon", "garden.optimalSpeeds.customSpeed.melon")
        event.move(3, "garden.optimalSpeedCustom.cocoaBeans", "garden.optimalSpeeds.customSpeed.cocoaBeans")
        event.move(3, "garden.optimalSpeedCustom.sugarCane", "garden.optimalSpeeds.customSpeed.sugarCane")
        event.move(3, "garden.optimalSpeedCustom.cactus", "garden.optimalSpeeds.customSpeed.cactus")
        event.move(3, "garden.optimalSpeedCustom.mushroom", "garden.optimalSpeeds.customSpeed.mushroom")

        event.move(14, "garden.optimalSpeeds.enabled", "garden.optimalSpeeds.showOnHUD")

        event.move(88, "garden.optimalSpeeds.signEnabled", "garden.farmingSettings.shortcutGUI")
        event.move(88, "garden.optimalSpeeds.compactRancherGui", "garden.farmingSettings.compactShortcutGUI")
        event.move(88, "garden.optimalSpeeds.customSpeed", "garden.farmingSettings.customSettings")
        event.move(88, "garden.optimalSpeeds.customSpeed.wheat", "garden.farmingSettings.customSettings.wheat.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.carrot", "garden.farmingSettings.customSettings.carrot.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.potato", "garden.farmingSettings.customSettings.potato.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.netherWart", "garden.farmingSettings.customSettings.netherWart.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.pumpkin", "garden.farmingSettings.customSettings.pumpkin.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.melon", "garden.farmingSettings.customSettings.melon.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.cocoaBeans", "garden.farmingSettings.customSettings.cocoaBeans.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.sugarCane", "garden.farmingSettings.customSettings.sugarCane.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.cactus", "garden.farmingSettings.customSettings.cactus.speed")
        event.move(88, "garden.optimalSpeeds.customSpeed.mushroom", "garden.farmingSettings.customSettings.mushroom.speed")
    }
}
