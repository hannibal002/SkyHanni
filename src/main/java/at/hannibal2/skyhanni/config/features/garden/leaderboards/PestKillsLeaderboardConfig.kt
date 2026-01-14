package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteLeaderboardGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.MultiModeTypeRankGoalConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.TypeRankGoalGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.PestTypeRankGoalsConfig
import at.hannibal2.skyhanni.features.garden.pests.PestType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

class PestKillsLeaderboardConfig : EliteLeaderboardGenericConfig<
    PestRankGoalConfig,
    PestKillsDisplayConfig
    >(
    { PestRankGoalConfig() },
    { PestKillsDisplayConfig() },
)

class PestRankGoalConfig : MultiModeTypeRankGoalConfig<PestTypeWithAll, PestTypeRankGoalsConfig, PestTypeMonthlyRankGoalsConfig>(
    { PestTypeRankGoalsConfig() },
    { PestTypeMonthlyRankGoalsConfig() }
) {

    @Expose
    @ConfigOption(
        name = "Rank Goal",
        desc = "What types to set a custom rank goal for. Applies to all leaderboard modes."
    )
    @ConfigEditorDraggableList
    override val rankGoalTypes: Property<MutableList<PestTypeWithAll>> = Property.of(mutableListOf())
}

class PestKillsDisplayConfig : EliteDisplayGenericConfig() {
    @Expose
    @ConfigOption(
        name = "Hide When Inactive",
        desc = "Hides the display when you haven't recently killed a pest and aren't holding a vacuum."
    )
    @ConfigEditorBoolean
    var hideWhenInactive: Boolean = true

    @Expose
    @ConfigOption(name = "Time Displayed", desc = "Time displayed after killing a pest.")
    @ConfigEditorSlider(minValue = 5f, maxValue = 60f, minStep = 1f)
    var timeDisplayed: Int = 30
}
// Pests only support monthly rank goals for all pests
class PestTypeMonthlyRankGoalsConfig : TypeRankGoalGenericConfig<PestTypeWithAll>() {
    @Expose
    @ConfigOption(name = "All pests", desc = "")
    @ConfigEditorText
    val allPests: Property<String> = Property.of("10000")

    override fun getConfig(type: PestTypeWithAll): KProperty0<Property<String>> = this::allPests
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
    DRAGONFLY(PestType.DRAGONFLY, PestType.DRAGONFLY.displayName),
    FIREFLY(PestType.FIREFLY, PestType.FIREFLY.displayName),
    PRAYING_MANTIS(PestType.PRAYING_MANTIS, PestType.PRAYING_MANTIS.displayName)
    ;

    companion object {
        fun fromPestType(type: PestType?): PestTypeWithAll =
            entries.firstOrNull { it.pestType == type } ?: ALL
    }

    override fun toString(): String = displayName
}

