package at.hannibal2.skyhanni.config.features.garden.cropmilestones

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// TODO moulconfig runnable support
class NextConfig {
    @Expose
    @ConfigOption(
        name = "Best Crop Time",
        desc = "List all crops and their ETA till next milestone. Sorts for best crop for getting garden or SkyBlock levels.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var bestDisplay: Boolean = false

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Sort Type", desc = "Sort the crops by either garden or SkyBlock EXP.")
    @ConfigEditorDropdown
    var bestType: BestTypeEntry = BestTypeEntry.GARDEN_EXP

    enum class BestTypeEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        GARDEN_EXP("Garden Exp", 0),
        SKYBLOCK_EXP("SkyBlock Exp", 1),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Only Show Top", desc = "Only show the top # crops.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var showOnlyBest: Int = 10

    @Expose
    @ConfigOption(
        name = "Extend Top List",
        desc = "Add current crop to the list if its lower ranked than the set limit by extending the list.",
    )
    @ConfigEditorBoolean
    var showCurrent: Boolean = true

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Always On", desc = "Show the Best Display always while in the garden.")
    @ConfigEditorBoolean
    var bestAlwaysOn: Boolean = false

    @Expose
    @ConfigOption(
        name = "Compact Display",
        desc = "A more compact best crop time: Removing the crop name and exp, hide the # number and using a more compact time format.",
    )
    @ConfigEditorBoolean
    var bestCompact: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Title", desc = "Hide the 'Best Crop Time' line entirely.")
    @ConfigEditorBoolean
    var bestHideTitle: Boolean = false

    @Expose
    @ConfigLink(owner = NextConfig::class, field = "bestDisplay")
    var displayPos: Position = Position(-200, -200, false, true)
}
