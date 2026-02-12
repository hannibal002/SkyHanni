package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteLeaderboardGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.SingleTypeRankGoalConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class WeightLeaderboardConfig : EliteLeaderboardGenericConfig<
    SingleTypeRankGoalConfig,
    WeightDisplayConfig,
    >(
    { SingleTypeRankGoalConfig() },
    { WeightDisplayConfig() },
)

class WeightDisplayConfig : EliteDisplayGenericConfig() {

    @Expose
    @ConfigOption(
        name = "Overtake ETA",
        desc = "Show a timer estimating when you'll move up a spot in the leaderboard! " +
            "Does not factor in pest drops. Garden Milestones Display must be enabled."
    )
    @ConfigEditorBoolean
    val overtakeETA: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Always ETA", desc = "Show the Overtake ETA always, even when not farming at the moment.")
    @ConfigEditorBoolean
    val overtakeETAAlways: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Show below 200", desc = "Show the farming weight data even if you are below 200 weight.")
    @ConfigEditorBoolean
    var ignoreLow: Boolean = false
}
