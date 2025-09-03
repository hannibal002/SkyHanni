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

class PestKillsDisplayConfigTEST: GenericDisplayConfig() {
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
}
