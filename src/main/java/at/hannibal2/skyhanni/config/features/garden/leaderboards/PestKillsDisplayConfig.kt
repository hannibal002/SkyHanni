package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.garden.pests.PestType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
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
        desc = "What pests to set a custom rank goal for. Applies to all leaderboard modes."
    )
    @ConfigEditorDraggableList
    val rankGoalPests: Property<MutableList<PestTypeWithAll>> = Property.of(mutableListOf())

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

            fun toPestType(value: PestTypeWithAll): PestType? =
                value.pestType
        }

        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(
        name = "All-Time Crop Rank Goals",
        desc = ""
    )
    @Accordion
    val pestRankGoalsConfig: PestRankGoalsConfig = PestRankGoalsConfig()

    @Expose
    @ConfigOption(
        name = "Monthly Crop Rank Goals",
        desc = ""
    )
    @Accordion
    val monthlyPestRankGoalsConfig: PestRankGoalsConfig = PestRankGoalsConfig()

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
