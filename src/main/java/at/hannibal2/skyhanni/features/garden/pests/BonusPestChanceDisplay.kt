package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString

@SkyHanniModule
object BonusPestChanceDisplay {

    private val config get() = PestApi.config

    private var display: String? = null

    @HandleEvent
    fun onWorldChange() {
        display = null
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.STATS)) return
        PestApi.updateBpc(event)

        val compact = config.pestChanceDisplay.get() == DisplayFormat.COMPACT
        val disabled = !PestApi.isBpcEnabled
        val amount = PestApi.latestBpc

        display = buildString {
            if (compact) append("§2ൠ BPC ") else append("§2ൠ Bonus Pest Chance ")
            if (disabled) append("§c§m") else append("§f")
            append("$amount%")
            if (disabled && !compact) append("§r §cDISABLED")
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        config.pestChanceDisplayPosition.renderString(display, posLabel = "Bonus Pest Chance")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(118, "garden.pests.pestChanceDisplay") { entry ->
            ConfigUtils.migrateBooleanToEnum(entry, DisplayFormat.FULL, DisplayFormat.DISABLED)
        }
    }

    private fun isEnabled() = GardenApi.inGarden() && config.pestChanceDisplay.get() != DisplayFormat.DISABLED && !GardenApi.hideExtraGuis()

    enum class DisplayFormat(private val displayName: String) {
        DISABLED("Disabled"),
        COMPACT("Compact"),
        FULL("Full"),
        ;

        override fun toString() = displayName
    }
}
