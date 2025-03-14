package at.hannibal2.skyhanni.config.features.inventory.accessories

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
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
    var maxHeight: Property<Int> = Property.of(250)

    @Expose
    @ConfigLink(owner = AccessoryOverviewDisplayConfig::class, field = "enabled")
    var position: Position = Position(250, 250)

    enum class AccessoryDisplayTab(private val displayName: String) {
        SUMMARY("§fSummary"),
        STATS("§bStats"),
        MISSING("§cMissing"),
        DUPLICATES("§6Duplicates"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Enabled Clicks", desc = "What options will be enabled from within the accessory overlay hovers.")
    @ConfigEditorDraggableList
    var enabledClickActions: MutableList<ClickActionType> = mutableListOf(
        ClickActionType.SEARCH_AH,
        ClickActionType.OPEN_IN_NEU,
        ClickActionType.OPEN_IN_WIKI,
    )

    enum class ClickActionType(private val displayName: String) {
        SEARCH_AH("§6Search Auction House"),
        OPEN_IN_NEU("§bOpen in NEU"),
        OPEN_IN_WIKI("§aOpen in Wiki"),
        ;

        override fun toString() = displayName
    }

    // Not exposed to the user, configured within the UI
    //
    @Expose
    var selectedTab: Property<AccessoryDisplayTab> = Property.of(AccessoryDisplayTab.SUMMARY)

    @Expose
    var missingTabSortType: Property<MissingSortType> = Property.of(MissingSortType.BEST_RATIO)

    enum class MissingSortType(private val displayName: String) {
        CHEAPEST("Cheapest overall"),
        RAW_MP("Most Raw MP"),
        BEST_RATIO("Next best first"),
        ;

        override fun toString() = displayName
    }

    @Expose
    var missingTabShowType: Property<MissingShowType> = Property.of(MissingShowType.SHOW_ALL_MISSING)

    enum class MissingShowType(private val displayName: String) {
        SHOW_ALL_MISSING("Show all missing"),
        MAX_EACH_FAMILY("Max of each family"),
        ;

        override fun toString() = displayName
    }
}
