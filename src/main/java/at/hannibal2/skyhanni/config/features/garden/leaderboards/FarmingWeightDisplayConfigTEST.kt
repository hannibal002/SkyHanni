package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class FarmingWeightDisplayConfigTEST: GenericDisplayConfig() {
    @Expose
    @ConfigOption(
        name = "Farming Weight Text",
        desc = "Drag text to change the appearance of the overlay.\n"
    )
    @ConfigEditorDraggableList
    val text: Property<MutableList<FarmingWeightTextEntry>> = Property.of(
        mutableListOf(
            FarmingWeightTextEntry.WEIGHT_POSITION,
            FarmingWeightTextEntry.OVERTAKE
        )
    )

    enum class FarmingWeightTextEntry(private val displayName: String) {
        WEIGHT_POSITION("§6Farming Weight: §e104,481.49 §7[§b#5§7]"),
        OVERTAKE("§e170.21 §7(§b12h 32m 15s§7) §7behind §bChissl"),
        LAST_PLAYER("§e170.21 §7 §7ahead of §bChissl")
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "All-Time Rank Goal",
        desc = "Set a rank goal for the All-Time Farming Weight Leaderboard."
    )
    @ConfigEditorText
    val weightRankGoal: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(
        name = "Monthly Rank Goal",
        desc = "Set a rank goal for the Monthly Farming Weight Leaderboard."
    )
    @ConfigEditorText
    val monthlyWeightRankGoal: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Show below 200", desc = "Show the farming weight data even if you are below 200 weight.")
    @ConfigEditorBoolean
    var ignoreLow: Boolean = false

}
