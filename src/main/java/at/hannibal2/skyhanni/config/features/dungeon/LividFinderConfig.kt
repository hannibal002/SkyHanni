package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
//#if TODO
import at.hannibal2.skyhanni.features.dungeon.DungeonLividFinder.LividColorHighlight
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

// todo 1.21 impl needed
class LividFinderConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Help find the correct livid in F5 and in M5.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hide Wrong Livids", desc = "Hide wrong livids entirely.")
    @ConfigEditorBoolean
    var hideWrong: Boolean = false

    //#if TODO
    @Expose
    @ConfigOption(name = "Color Override", desc = "Forces the livid highlight to be a specific color.")
    @ConfigEditorDropdown
    var colorOverride: LividColorHighlight = LividColorHighlight.DEFAULT
    //#endif
}
