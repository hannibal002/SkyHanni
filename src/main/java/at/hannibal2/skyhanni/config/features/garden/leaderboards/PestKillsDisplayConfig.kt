package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.features.garden.pests.PestType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.*
import io.github.notenoughupdates.moulconfig.observer.Property

class PestKillsDisplayConfig {
    @Expose
    @ConfigOption(
        name = "Display",
        desc = "Display your farming weight on screen.\n" +
            "The calculation and API is provided by The Elite SkyBlock farmers.\n" +
            "See §eelitebot.dev/info §7for more info."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = true

    @Expose
    @ConfigLink(owner = PestKillsDisplayConfig::class, field = "display")
    val pos: Position = Position(180, 10)

    @Expose
    @ConfigOption(
        name = "Farming Weight Text",
        desc = "Drag text to change the appearance of the overlay.\n"
    )
    @ConfigEditorDraggableList
    val text: Property<MutableList<PestKillsTextEntry>> = Property.of(
        mutableListOf(
            PestKillsTextEntry.WEIGHT_POSITION,
            PestKillsTextEntry.OVERTAKE
        )
    )

    enum class PestKillsTextEntry(private val displayName: String) {
        WEIGHT_POSITION("§6Farming Weight: §e104,481.49 §7[§b#5§7]"),
        OVERTAKE("§e170.21 §7(§b12h 32m 15s§7) §7behind §bChissl"),
        LAST_PLAYER("§e170.21 §7 §7ahead of §bChissl")
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Leaderboard Ranking",
        desc = "Show your position in the farming weight leaderboard. " +
            "Only if your farming weight is high enough! Updates periodically."
    )
    @ConfigEditorBoolean
    val leaderboard: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Use Rank Goal",
        desc = "Use the Rank Goal number instead of the next upcoming rank. Useful when your rank is in the " +
                "ten thousands and you don't want to see small ETAs."
    )
    @ConfigEditorBoolean
    val useRankGoal: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Rank Goal",
        desc = "What crops to set a custom rank goal for. Applies to all leaderboard modes."
    )
    @ConfigEditorDraggableList
    val rankGoalPests: Property<MutableList<PestTypeWithAll>> = Property.of(mutableListOf())

    sealed class PestTypeWithAll {
        object AllPests : PestTypeWithAll()
        data class Specific(val type: PestType) : PestTypeWithAll()

        override fun toString(): String = when (this) {
            AllPests -> "All Pests"
            is Specific -> type.displayName
        }
    }

    @Expose
    @ConfigOption(
        name = "All-Time Crop Rank Goals",
        desc = ""
    )
    @Accordion
    val pestRankGoalsConfig: Property<PestRankGoalsConfig> =
        Property.of(PestRankGoalsConfig(EliteLeaderboardMode.ALL_TIME))

    @Expose
    @ConfigOption(
        name = "Monthly Crop Rank Goals",
        desc = ""
    )
    @Accordion
    val monthlyPestRankGoalsConfig: Property<PestRankGoalsConfig> =
        Property.of(PestRankGoalsConfig(EliteLeaderboardMode.MONTHLY))

    @Expose
    @ConfigOption(
        name = "Show LB Change",
        desc = "Show the change of your position on your current pest leaderboard while you were offline."
    )
    @ConfigEditorBoolean
    var showLbChange: Boolean = false

    @Expose
    @ConfigOption(name = "Show Outside Garden", desc = "Show your pest kills outside of the garden.")
    @ConfigEditorBoolean
    var showOutsideGarden: Boolean = false
}
