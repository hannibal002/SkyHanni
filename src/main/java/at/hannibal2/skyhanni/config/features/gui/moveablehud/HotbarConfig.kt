package at.hannibal2.skyhanni.config.features.gui.moveablehud

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HotbarConfig : MoveableHudConfig {
    @Expose
    @ConfigOption(
        name = "Editable",
        desc = "Add the hotbar to the gui editor. Allows for moving and scaling of the hotbar."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = HotbarConfig::class, field = "enabled")
    override val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Show Outside SkyBlock", desc = "Enable the hotbar to be edited even outside of SkyBlock.")
    @ConfigEditorBoolean
    override var showOutsideSkyblock: Boolean = false

    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(129, "gui.hotbar.editable", "gui.hotbar.enabled")
            event.move(129, "gui.hotbar.hotbar", "gui.hotbar.position")
        }
    }
}
