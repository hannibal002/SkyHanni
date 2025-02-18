package at.hannibal2.skyhanni.config.features.event.diana

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class DianaConfig {
    @Expose
    @ConfigOption(
        name = "Highlight Inquisitors",
        desc = "Highlight Inquisitors found from the Mythological Event perk.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightInquisitors: Boolean = true

    @Expose
    @ConfigOption(name = "Inquisitor Highlight", desc = "Color in which Inquisitors will be highlighted.")
    @ConfigEditorColour
    var color: String = "0:127:85:255:255"

    @Expose
    @ConfigOption(
        name = "Guess Next Burrow",
        desc = "Guess the next burrow location when using the Ancestral Spade. §eDoes not show the type of burrow.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var guess: Boolean = false

    enum class GuessLogic(private val display: String) {
        SOOPY_GUESS("Soopy"),
        PRECISE_GUESS("Precise"),
        ;

        override fun toString(): String = display
    }

    @Expose
    @ConfigOption(name = "Guessing Logic", desc = "Change which guess strategy to use.")
    @ConfigEditorDropdown
    var guessLogic: GuessLogic = GuessLogic.PRECISE_GUESS

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
        name = "Nearest Warp",
        desc = "Warp to the nearest warp point on the hub, if closer to the next burrow.",
    )
    @ConfigEditorBoolean
    var burrowNearestWarp: Boolean = false

    @Expose
    @ConfigOption(name = "Warp Key", desc = "Press this key to warp to nearest burrow waypoint.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var keyBindWarp: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(name = "Ignored Warps", desc = "")
    @Accordion
    var ignoredWarps: IgnoredWarpsConfig = IgnoredWarpsConfig()

    @Expose
    @ConfigOption(name = "Inquisitor Waypoint Sharing", desc = "")
    @Accordion
    var inquisitorSharing: InquisitorSharingConfig = InquisitorSharingConfig()

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
    var dianaProfitTracker: DianaProfitTrackerConfig = DianaProfitTrackerConfig()

    // TODO rename mythologicalMobTracker
    @Expose
    @ConfigOption(name = "Mythological Creature Tracker", desc = "")
    @Accordion
    var mythologicalMobtracker: MythologicalMobTrackerConfig = MythologicalMobTrackerConfig()

    @Expose
    @ConfigOption(name = "All Burrows List", desc = "")
    @Accordion
    var allBurrowsList: AllBurrowsListConfig = AllBurrowsListConfig()
}
