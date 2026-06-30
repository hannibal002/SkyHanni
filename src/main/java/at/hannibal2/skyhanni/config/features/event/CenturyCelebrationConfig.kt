package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CenturyCelebrationConfig {

    // TODO: Rename field to reflect that tasks are no longer daily
    @ConfigOption(
        name = "Raffle Task Highlighter",
        desc = "Highlights incomplete raffle tasks.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightDailyTasks: Boolean = true

    @ConfigOption(
        name = "Team Finder",
        desc = "Highlight players in the right team when holding a Slice of Cake item.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var teamFinder: Boolean = true

    @ConfigOption(name = "Team Finder Color", desc = "Change all the colors!")
    @Accordion
    @Expose
    val colors: AnniversaryTeamFinderColorConfig = AnniversaryTeamFinderColorConfig()
}
