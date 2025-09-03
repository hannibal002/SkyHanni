package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.garden.CropType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CropCollectionDisplayConfigTEST: GenericDisplayConfig() {
    @Expose
    @ConfigOption(name = "Hide When Not Farming", desc = "Hides the display unless actively farming.")
    @ConfigEditorBoolean
    val hideWhenNotFarming: Boolean = true

    @Expose
    @ConfigOption(
        name = "Crop Collection Text",
        desc = "Drag text to change the appearance of the overlay.\n"
    )
    @ConfigEditorDraggableList
    val text: Property<MutableList<CropCollectionTextEntry>> = Property.of(
        mutableListOf(
            CropCollectionTextEntry.WEIGHT_POSITION,
            CropCollectionTextEntry.OVERTAKE
        )
    )

    // TODO fix when no longer updating display
    enum class CropCollectionTextEntry(private val displayName: String) {
        WEIGHT_POSITION("§6Farming Weight: §e104,481.49 §7[§b#5§7]"),
        OVERTAKE("§e170.21 §7(§b12h 32m 15s§7) §7behind §bChissl"),
        LAST_PLAYER("§e170.21 §7 §7ahead of §bChissl")
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Rank Goal",
        desc = "What crops to set a custom rank goal for. Applies to all leaderboard modes."
    )
    @ConfigEditorDraggableList
    val rankGoalCrops: Property<MutableList<CropType>> = Property.of(mutableListOf())

    @Expose
    @ConfigOption(
        name = "All-Time Crop Rank Goals",
        desc = ""
    )
    @Accordion
    val cropRankGoalsConfig: CropRankGoalsConfig = CropRankGoalsConfig()

    @Expose
    @ConfigOption(
        name = "Monthly Crop Rank Goals",
        desc = ""
    )
    @Accordion
    val monthlyCropRankGoalsConfig: CropRankGoalsConfig = CropRankGoalsConfig()
}
