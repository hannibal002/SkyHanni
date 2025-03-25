package at.hannibal2.skyhanni.config.features.event.hoppity

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HoppityEggsConfig {
    @Expose
    @ConfigOption(name = "Hoppity Abiphone Calls", desc = "")
    @Accordion
    var hoppityCallWarning: HoppityCallWarningConfig = HoppityCallWarningConfig()

    @Expose
    @ConfigOption(name = "Hoppity Hunt Stats Summary", desc = "")
    @Accordion
    var eventSummary: HoppityEventSummaryConfig = HoppityEventSummaryConfig()

    @Expose
    @ConfigOption(name = "Warp Menu", desc = "")
    @Accordion
    var warpMenu: HoppityWarpMenuConfig = HoppityWarpMenuConfig()

    @Expose
    @ConfigOption(name = "Stray Timer", desc = "")
    @Accordion
    var strayTimer: HoppityStrayTimerConfig = HoppityStrayTimerConfig()

    @Expose
    @ConfigOption(name = "Hoppity Waypoints", desc = "Toggle guess waypoints for Hoppity's Hunt.")
    @ConfigEditorBoolean
    @FeatureToggle
    var waypoints: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color of the waypoint.")
    @ConfigEditorColour
    var waypointColor: String = "0:53:46:224:73"

    @Expose
    @ConfigOption(name = "Show Line", desc = "Show a line to the waypoint.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showLine: Boolean = false

    @Expose
    @ConfigOption(name = "Show Path Finder", desc = "Show a pathfind to the next hoppity egg.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showPathFinder: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show All Waypoints",
        desc = "Show all possible egg waypoints for the current lobby.\n" +
            "§eOnly works when you don't have an Egglocator in your inventory.",
    )
    @ConfigEditorBoolean
    var showAllWaypoints: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Duplicate Waypoints",
        desc = "Hide egg waypoints you have already found.\n" +
            "§eOnly works when you don't have an Egglocator in your inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideDuplicateWaypoints: Boolean = false

    @Expose
    @ConfigOption(
        name = "Mark Duplicate Locations",
        desc = "Marks egg location waypoints which you have already found in red.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightDuplicateEggLocations: Boolean = false

    @Expose
    @ConfigOption(name = "Mark Nearby Duplicates", desc = "Always show duplicate egg locations when nearby.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showNearbyDuplicateEggLocations: Boolean = false

    @Expose
    @ConfigOption(
        name = "Load from NEU PV",
        desc = "Load Hoppity Egg Location data from API when opening the NEU Profile Viewer.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var loadFromNeuPv: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Unclaimed Eggs",
        desc = "Display which eggs haven't been found in the last SkyBlock day.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showClaimedEggs: Boolean = false

    @Expose
    @ConfigOption(name = "Unclaimed Eggs Order", desc = "Order in which to display unclaimed eggs.")
    @ConfigEditorDropdown
    var unclaimedEggsOrder: UnclaimedEggsOrder = UnclaimedEggsOrder.SOONEST_FIRST

    enum class UnclaimedEggsOrder(private val displayName: String) {
        SOONEST_FIRST("Soonest First"),
        MEAL_ORDER("Meal Order"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Show Collected Locations",
        desc = "Show the number of found egg locations on this island.\n" +
            "§eThis is not retroactive and may not be fully synced with Hypixel's count.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showCollectedLocationCount: Boolean = false

    @Expose
    @ConfigOption(name = "Warn When Unclaimed", desc = "Warn when all six eggs are ready to be found.")
    @ConfigEditorBoolean
    @FeatureToggle
    var warnUnclaimedEggs: Boolean = false

    @Expose
    @ConfigOption(
        name = "Click to Warp",
        desc = "Make the eggs ready chat message & unclaimed timer display clickable to warp you to an island.",
    )
    @ConfigEditorBoolean
    var warpUnclaimedEggs: Boolean = false

    @Expose
    @ConfigOption(name = "Warp Destination", desc = "A custom island to warp to in the above option.")
    @ConfigEditorText
    var warpDestination: String = "nucleus"

    @Expose
    @ConfigOption(
        name = "Show While Busy",
        desc = "Show while \"busy\" (in a farming contest, doing Kuudra, in the rift, etc).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showWhileBusy: Boolean = false

    @Expose
    @ConfigOption(
        name = "Warn While Busy",
        desc = "Warn while \"busy\" (in a farming contest, doing Kuudra, in the rift, etc).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var warnWhileBusy: Boolean = false

    @Expose
    @ConfigOption(name = "Show Outside SkyBlock", desc = "Show on Hypixel even when not playing SkyBlock.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showOutsideSkyblock: Boolean = false

    @Expose
    @ConfigOption(
        name = "Shared Hoppity Waypoints",
        desc = "Enable being able to share and receive egg waypoints in your lobby.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var sharedWaypoints: Boolean = true

    @Expose
    @ConfigOption(
        name = "Adjust player opacity",
        desc = "Adjust the opacity of players near shared & guessed egg waypoints. (in %)",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var playerOpacity: Int = 40

    @Expose
    @ConfigLink(owner = HoppityEggsConfig::class, field = "showClaimedEggs")
    var position: Position = Position(200, 120, false, true)

    @Expose
    @ConfigOption(
        name = "Highlight Hoppity Shop",
        desc = "Highlight items that haven't been bought from the Hoppity shop yet.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightHoppityShop: Boolean = true

    @Expose
    @ConfigOption(name = "Hoppity Shop Reminder", desc = "Remind you to open the Hoppity Shop each year.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hoppityShopReminder: Boolean = true

    @Expose
    @ConfigOption(
        name = "Time in Chat",
        desc = "When the Egglocator can't find an egg, show the time until the next Hoppity event or egg spawn.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var timeInChat: Boolean = true

    @Expose
    @ConfigOption(name = "Compact Chat", desc = "Compact chat events when finding a Hoppity Egg.")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactChat: Boolean = false

    @Expose
    @ConfigOption(name = "Compacted Rarity", desc = "Show rarity of found rabbit in Compacted chat messages.")
    @ConfigEditorDropdown
    var rarityInCompact: CompactRarityTypes = CompactRarityTypes.NEW

    enum class CompactRarityTypes(private val displayName: String) {
        NONE("Neither"),
        NEW("New Rabbits"),
        DUPE("Duplicate Rabbits"),
        BOTH("New & Duplicate Rabbits"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Show Duplicate Count",
        desc = "Show the number of previous finds of a duplicate Hoppity rabbit in chat messages.",
    )
    @ConfigEditorBoolean
    var showDuplicateNumber: Boolean = false

    @Expose
    @ConfigOption(
        name = "Recolor Time-Towered Chocolate",
        desc = "Recolor raw chocolate gain from duplicate rabbits while Time Tower is active.",
    )
    @ConfigEditorBoolean
    var recolorTTChocolate: Boolean = false

    @Expose
    @ConfigOption(
        name = "Rabbit Pet Warning",
        desc = "Warn when using the Egglocator without a §d§lMythic Rabbit Pet §7equipped. " +
            "§eOnly enable this setting when you own a mythic Rabbit pet.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var petWarning: Boolean = false

    @Expose
    @ConfigOption(
        name = "Prevent Missing Rabbit the Fish",
        desc = "Prevent closing a Meal Egg's inventory if Rabbit the Fish is present.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var preventMissingRabbitTheFish: Boolean = true
}
