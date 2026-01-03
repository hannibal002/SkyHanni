package at.hannibal2.skyhanni.config.features.garden.contest

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.garden.NextJacobContestConfig
import at.hannibal2.skyhanni.config.features.garden.PersonalBestsConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class JacobContestConfig {
    @Expose
    @ConfigOption(name = "Next Jacob's Contest", desc = "")
    @Accordion
    val nextContest: NextJacobContestConfig = NextJacobContestConfig()

    @Expose
    @ConfigOption(name = "Personal Bests", desc = "")
    @Accordion
    val personalBests: PersonalBestsConfig = PersonalBestsConfig()

    @Expose
    @ConfigOption(name = "Contest Time Needed", desc = "")
    @Accordion
    val timesNeeded: ContestTimesConfig = ContestTimesConfig()

    @Expose
    @ConfigOption(name = "Contest Summary", desc = "")
    @Accordion
    val contestSummary: ContestSummaryConfig = ContestSummaryConfig()

    @Expose
    @ConfigOption(
        name = "FF for Contest",
        desc = "Show the minimum needed Farming Fortune for reaching each medal in Jacob's Farming Contest inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var ffForContest: Boolean = true

    @Expose
    @ConfigLink(owner = JacobContestConfig::class, field = "ffForContest")
    val ffForContestPosition: Position = Position(180, 156)

    @Expose
    @ConfigOption(
        name = "Zorro's Cape Protection",
        desc = "Block Jacob's Farming Contest claims when not wearing a §6Zorro's Cape§f. Do not waste the 20% chance of duplicate medals!",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var zorroCapeProtection: Boolean = false
}
