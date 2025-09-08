package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteLeaderboardGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.MultipleTypeRankGoalConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.PestTypeRankGoalsConfig
import at.hannibal2.skyhanni.features.garden.pests.PestType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class PestKillsLeaderboardConfig : EliteLeaderboardGenericConfig<
    PestRankGoalConfig,
    EliteDisplayGenericConfig
    >(
    { PestRankGoalConfig() },
    { EliteDisplayGenericConfig() },
)

class PestRankGoalConfig : MultipleTypeRankGoalConfig<PestTypeWithAll, PestTypeRankGoalsConfig>(
    { PestTypeRankGoalsConfig() }
) {
    @Expose
    @ConfigOption(
        name = "Rank Goal",
        desc = "What types to set a custom rank goal for. Applies to all leaderboard modes."
    )
    @ConfigEditorDraggableList
    override val rankGoalTypes: Property<MutableList<PestTypeWithAll>> = Property.of(mutableListOf())
}

enum class PestTypeWithAll(val pestType: PestType?, val displayName: String) {
    ALL(null, "All Pests"),

    BEETLE(PestType.BEETLE, PestType.BEETLE.displayName),
    CRICKET(PestType.CRICKET, PestType.CRICKET.displayName),
    EARTHWORM(PestType.EARTHWORM, PestType.EARTHWORM.displayName),
    FIELD_MOUSE(PestType.FIELD_MOUSE, PestType.FIELD_MOUSE.displayName),
    FLY(PestType.FLY, PestType.FLY.displayName),
    LOCUST(PestType.LOCUST, PestType.LOCUST.displayName),
    MITE(PestType.MITE, PestType.MITE.displayName),
    MOSQUITO(PestType.MOSQUITO, PestType.MOSQUITO.displayName),
    MOTH(PestType.MOTH, PestType.MOTH.displayName),
    RAT(PestType.RAT, PestType.RAT.displayName),
    SLUG(PestType.SLUG, PestType.SLUG.displayName),
    ;

    companion object {
        fun fromPestType(type: PestType?): PestTypeWithAll =
            entries.firstOrNull { it.pestType == type } ?: ALL
    }

    override fun toString(): String = displayName
}

