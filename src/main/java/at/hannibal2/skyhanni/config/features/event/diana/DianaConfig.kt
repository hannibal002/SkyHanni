package at.hannibal2.skyhanni.config.features.event.diana

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import org.lwjgl.glfw.GLFW

class DianaConfig {
    // TODO rename to highlightRareMobs
    @Expose
    @ConfigOption(
        name = "Highlight Rare Diana Mobs",
        desc = "Highlight Rare Diana Mobs (Sphinx+) found from the Mythological Event perk.",
    )
    @SearchTag("inquisitor")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightInquisitors: Boolean = true

    @Expose
    @ConfigOption(name = "Rare Diana Mob Highlight", desc = "Color in which Rare Diana Mobs will be highlighted.")
    @SearchTag("inquisitor")
    @ConfigEditorColour
    var color: ChromaColour = ChromaColour.fromStaticRGB(85, 255, 255, 127)

    @Expose
    @ConfigOption(
        name = "Guess Next Burrow",
        desc = "Guess the next burrow location when using the Ancestral Spade. §eDoes not show the type of burrow.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var guess: Boolean = false

    @Expose
    @ConfigOption(
        name = "Multi Guesses",
        desc = "Remember previous guess locations when guessing to a new location.",
    )
    @ConfigEditorBoolean
    var multiGuesses: Boolean = true

    @Expose
    @ConfigOption(
        name = "Nearby Detection",
        desc = "Show burrow locations near you, include type of burrow.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var burrowsNearbyDetection: Boolean = false

    @Expose
    @ConfigOption(
        name = "Line To Next",
        desc = "Show a line to the closest burrow or guess location.\n" +
            "§eRequires Burrow particle detection.",
    )
    @ConfigEditorBoolean
    var lineToNext: Boolean = true

    @Expose
    @ConfigOption(
        name = "Guess From Arrow",
        desc = "Guess next burrow location in chain instantly from the particle arrow.\n" +
            "It is recommended to use bobby for better results.",
    )
    @ConfigEditorBoolean
    var guessFromArrow: Boolean = true

    @Expose
    @ConfigOption(
        name = "Warn On Failure",
        desc = "Sends \"Use Spade\" title when arrow guess fails.",
    )
    @ConfigEditorBoolean
    var warnOnFail: Boolean = true

    @Expose
    @ConfigOption(
        name = "Warn On Chain Complete",
        desc = "Sends \"Use Spade\" title when you complete a chain and there is not a burrow within 90 blocks.",
    )
    @ConfigEditorBoolean
    var warnOnChainComp: Boolean = true

    @Expose
    @ConfigOption(
        name = "Render SubGuesses",
        desc = "If there are multiple possible blocks will render them all in a greyed out chain.",
    )
    @ConfigEditorBoolean
    var renderSubGuesses: Boolean = false

    @Expose
    @ConfigOption(
        name = "Clear On World Change",
        desc = "Clear all guess data on world change.",
    )
    @ConfigEditorBoolean
    var clearOnWorldChange: Boolean = false

    @Expose
    @ConfigOption(
        name = "Nearest Warp",
        desc = "Warp to the nearest warp point on the hub, if closer to the next burrow.",
    )
    @ConfigEditorBoolean
    var burrowNearestWarp: Boolean = false

    @Expose
    @ConfigOption(name = "Warp Key", desc = "Press this key to warp to nearest burrow waypoint.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var keyBindWarp: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Warp Distance",
        desc = "How much closer a warp needs to be than you to suggest it.",
    )
    @ConfigEditorSlider(minValue = 0.0f, maxValue = 200.0f, minStep = 1.0f)
    var warpDistanceDifference: Int = 10

    @Expose
    @ConfigLink(owner = DianaConfig::class, field = "burrowNearestWarp")
    val warpGuiPosition: Position = Position(327, 125, scale = 2.6f)

    @Expose
    @ConfigOption(name = "Ignored Warps", desc = "")
    @Accordion
    val ignoredWarps: IgnoredWarpsConfig = IgnoredWarpsConfig()

    // TODO rename to rareMobsSharing
    @Expose
    @ConfigOption(name = "Rare Diana Mob Waypoint Sharing", desc = "")
    @SearchTag("inquisitor")
    @Accordion
    val inquisitorSharing: InquisitorSharingConfig = InquisitorSharingConfig()

    @Expose
    @ConfigOption(
        name = "Griffin Pet Warning",
        desc = "Warn when holding an Ancestral Spade if a Griffin Pet is not equipped.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var petWarning: Boolean = true

    @Expose
    @ConfigOption(name = "Diana Profit Tracker", desc = "")
    @Accordion
    val dianaProfitTracker: DianaProfitTrackerConfig = DianaProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Mythological Creature Tracker", desc = "")
    @Accordion
    val mythologicalMobtracker: MythologicalMobTrackerConfig = MythologicalMobTrackerConfig()

    @Expose
    @ConfigOption(name = "All Burrows List", desc = "")
    @Accordion
    val allBurrowsList: AllBurrowsListConfig = AllBurrowsListConfig()
}
