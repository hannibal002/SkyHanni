package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class AnniversaryCelebration400Config {

    @ConfigOption(
        name = "Daily Highlight",
        desc = "Highlights incomplete daily tasks.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightDailyTasks: Boolean = true

    @ConfigOption(
        name = "Daily HUD",
        desc = "Shows incomplete daily tasks on the hud.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var dailyTasksHud: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = AnniversaryCelebration400Config::class, field = "dailyTasksHud")
    var dailyTaskPosition = Position(20, 20).apply { scale = 0.75f }

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
    var colors: AnniversaryTeamFinderColorConfig = AnniversaryTeamFinderColorConfig()

}
