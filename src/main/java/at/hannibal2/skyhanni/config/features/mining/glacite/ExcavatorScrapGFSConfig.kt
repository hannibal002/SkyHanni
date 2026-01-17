package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ExcavatorScrapGFSConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a UI while in the Fossil Excavator to fetch Suspicious Scrap from your sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = ExcavatorScrapGFSConfig::class, field = "enabled")
    val position: Position = Position(100, 100)

    @Expose
    @ConfigOption(
        name = "Fetch Amount",
        desc = "How many Suspicious Scrap to fetch from your sacks when clicking the button.\n" +
            "§8Can also be changed using the §a+ §8and §c-§8 buttons in the GUI."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 64f, minStep = 1f)
    val fetchAmount: Property<Int> = Property.of(16)

    @Expose
    @ConfigOption(
        name = "Only if No Scrap",
        desc = "If enabled, the button will only be shown if you do not have any Suspicious Scrap in your inventory."
    )
    @ConfigEditorBoolean
    var onlyIfNoScrap: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show BZ Link",
        desc = "If you do not have any Suspicious Scrap in your sacks, show a link to open the bazaar page for Suspicious Scrap."
    )
    @ConfigEditorBoolean
    var bzIfSacksEmpty: Boolean = true

}
