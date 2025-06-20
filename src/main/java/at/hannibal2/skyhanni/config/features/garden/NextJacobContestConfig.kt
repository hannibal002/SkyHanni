package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.garden.CropType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NextJacobContestConfig {
    @Expose
    @ConfigOption(
        name = "Show Jacob's Contest",
        desc = "Show the current or next Jacob's farming contest time and crops.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = true

    @Expose
    @ConfigOption(name = "Outside Garden", desc = "Show the timer not only in the Garden but everywhere in SkyBlock.")
    @ConfigEditorBoolean
    var showOutsideGarden: Boolean = false

    @Expose
    @ConfigOption(
        name = "In Other Guis",
        desc = "Mark the current or next Farming Contest crops in other farming GUIs as underlined.",
    )
    @ConfigEditorBoolean
    var otherGuis: Boolean = false

    @Expose
    @ConfigOption(
        name = "Fetch Contests",
        desc = "Automatically fetch Contests from elitebot.dev for the current year if they're uploaded already.",
    )
    @ConfigEditorBoolean
    var fetchAutomatically: Boolean = true

    @Expose
    @ConfigOption(
        name = "Additional Boosted Highlight",
        desc = "Highlight the current boosted crop with an outline in addition to the enchant glint.",
    )
    @ConfigEditorBoolean
    var additionalBoostedHighlight: Boolean = false

    @Expose
    @ConfigOption(
        name = "Additional Boosted Highlight Color",
        desc = "Set the color of the highlight for the current boosted crop.",
    )
    @ConfigEditorColour
    var additionalBoostedHighlightColor: String = "0:80:0:255:0"

    @Expose
    @ConfigOption(
        name = "Share Contests",
        desc = "Share the list of upcoming Contests to elitebot.dev for everyone else to then fetch automatically.",
    )
    @ConfigEditorDropdown
    var shareAutomatically: ShareContestsEntry = ShareContestsEntry.ASK

    enum class ShareContestsEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        ASK("Ask When Needed", 0),
        AUTO("Share Automatically", 1),
        DISABLED("Disabled", 2),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Warning", desc = "Show a warning shortly before a new Jacob's Contest starts.")
    @ConfigEditorBoolean
    var warn: Boolean = false

    @Expose
    @ConfigOption(name = "Warning Time", desc = "Set the warning time in seconds before a Jacob's Contest begins.")
    @ConfigEditorSlider(minValue = 10f, maxValue = 300f, minStep = 1f)
    var warnTime: Int = 120

    @Expose
    @ConfigOption(
        name = "Popup Warning",
        desc = "Create a popup when the warning time is reached and Minecraft is not in focus.",
    )
    @ConfigEditorBoolean
    var warnPopup: Boolean = false

    @Expose
    @ConfigOption(name = "Warn For", desc = "Only warn for these crops.")
    @ConfigEditorDraggableList
    var warnFor: MutableList<CropType> = CropType.entries.toMutableList()

    @Expose
    @ConfigLink(owner = NextJacobContestConfig::class, field = "display")
    var pos: Position = Position(-200, 10)
}
