package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.core.config.Position
//#if TODO
import at.hannibal2.skyhanni.features.gui.MayorOverlay
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class MayorOverlayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Mayor Overlay.")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    //#if TODO
    @Expose
    @ConfigOption(name = "Appearance", desc = "Change the order of appearance of the Mayor Overlay.")
    @ConfigEditorDraggableList
    var mayorOverlay: MutableList<MayorOverlay> = MayorOverlay.entries.toMutableList()
    //#endif

    @Expose
    @ConfigOption(name = "Show Perks", desc = "Show the perks of the mayor.")
    @ConfigEditorBoolean
    var showPerks: Boolean = true

    @Expose
    @ConfigOption(name = "Spacing between UI Elements", desc = "Change the spacing between the UI element entries.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
    var spacing: Int = 10

    @Expose
    @ConfigOption(name = "Spacing between Candidates", desc = "Change the spacing between the candidates.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
    var candidateSpacing: Int = 3

    @Expose
    @ConfigLink(owner = MayorOverlayConfig::class, field = "enabled")
    var position: Position = Position(10, 10)
}
