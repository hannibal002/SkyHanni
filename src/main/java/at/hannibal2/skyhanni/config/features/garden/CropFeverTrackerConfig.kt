package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CropFeverTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Track your crop fever drops.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Only Show With Enchant", desc = "Only show when holding a farming with the crop fever enchant.")
    @ConfigEditorBoolean
    @FeatureToggle
    var onlyWithTool: Boolean = true

    @Expose
    @ConfigOption(name = "Only Show During Fever", desc = "Only show during a crop fever.")
    @ConfigEditorBoolean
    var onlyDuringFever: Boolean = true

    @Expose
    @ConfigOption(
        name = "Tracker Text",
        desc = "Drag text to change the appearance of the overlay.\n"
    )
    @ConfigEditorDraggableList
    val text: Property<MutableList<CropFeverTrackerTextEntry>> = Property.of(
        mutableListOf(
            CropFeverTrackerTextEntry.TITLE,
            CropFeverTrackerTextEntry.RNG_DROPS,
            CropFeverTrackerTextEntry.SPACER_2,
            CropFeverTrackerTextEntry.ITEM_DROPS,
            CropFeverTrackerTextEntry.FEVER_AMOUNT,
            CropFeverTrackerTextEntry.TOTAL_BLOCKS,
            CropFeverTrackerTextEntry.TOTAL_PROFIT
        )
    )

    enum class CropFeverTrackerTextEntry(private val displayName: String) {
        TITLE("§6Crop Fever Tracker"),
        RNG_DROPS("§7- §e17x §a§lUNCOMMON DROP\n§7- §e6x §9§lRARE DROP\n§7- §e1x §d§lCRAZY RARE DROP"),
        ITEM_DROPS("§7120x §aEnchanted Melon Slice §61.8m\n§73x §9Enchanted Melon Block §6150k"),
        FEVER_AMOUNT("§7Total Crop Fevers: §e5"),
        FEVER_DURATION("§7Crop Fever Duration: §b4m 32s"),
        TOTAL_BLOCKS("§7Total Blocks Broken: §e12,123"),
        TOTAL_PROFIT("§7Total Profit: §618m"),
        SPACER_1(" "),
        SPACER_2(" "),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = CropFeverTrackerConfig::class, field = "enabled")
    val position: Position = Position(80, 20)
}
