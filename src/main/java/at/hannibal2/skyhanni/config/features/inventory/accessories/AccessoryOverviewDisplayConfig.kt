package at.hannibal2.skyhanni.config.features.inventory.accessories

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class AccessoryOverviewDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a summary of your owned accessories in the Accessory Bag.")
    @FeatureToggle
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Max Height", desc = "Maximum height of the display.")
    @ConfigEditorSlider(minValue = 50f, maxValue = 500f, minStep = 10f)
    var maxHeight: Property<Float> = Property.of(250f)

    @Expose
    @ConfigLink(owner = AccessoryOverviewDisplayConfig::class, field = "enabled")
    var position: Position = Position(250, 250)

    enum class AccessoryDisplayTab(private val displayName: String) {
        OVERVIEW("§fOverview"),
        STATS("§bStats"),
        MISSING("§cMissing"),
        DUPLICATES("§6Duplicates"),
        ;

        override fun toString() = displayName
    }

    @Expose
    var selectedTab: AccessoryDisplayTab = AccessoryDisplayTab.OVERVIEW
}
