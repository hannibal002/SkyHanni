package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteLeaderboardGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.MultiTypeRankGoalConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.CropTypeRankGoalsConfig
import at.hannibal2.skyhanni.features.garden.CropType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CropCollectionLeaderboardConfig : EliteLeaderboardGenericConfig<
    CropRankGoalConfig,
    CropCollectionDisplayConfig
    >(
    { CropRankGoalConfig() },
    { CropCollectionDisplayConfig() }
)

class CropCollectionDisplayConfig : EliteDisplayGenericConfig() {

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
    @ConfigOption(name = "Hide When Not Farming", desc = "Hides the display unless actively farming.")
    @ConfigEditorBoolean
    var hideWhenNotFarming: Boolean = true
}

class CropRankGoalConfig : MultiTypeRankGoalConfig<CropType, CropTypeRankGoalsConfig>(
    { CropTypeRankGoalsConfig() }
) {
    @Expose
    @ConfigOption(
        name = "Rank Goal",
        desc = "What types to set a custom rank goal for. Applies to all leaderboard modes."
    )
    @ConfigEditorDraggableList
    override val rankGoalTypes: Property<MutableList<CropType>> = Property.of(mutableListOf())
}
