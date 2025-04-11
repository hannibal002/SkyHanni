package at.hannibal2.skyhanni.config.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CraftingRoomConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show an holographic version of the mob on your side of the craft room.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Show Name", desc = "Show the name of the mob.")
    @ConfigEditorBoolean
    var showName: Boolean = true

    @Expose
    @ConfigOption(name = "Show Health", desc = "Show the health of the mob.")
    @ConfigEditorBoolean
    var showHealth: Boolean = true

    @Expose
    @ConfigOption(name = "Hide Players", desc = "Hide other players in the room.")
    @ConfigEditorBoolean
    var hidePlayers: Boolean = true
}
