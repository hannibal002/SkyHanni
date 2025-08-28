package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.features.garden.CropType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

class CropRankGoalsConfig(
    private val mode: EliteLeaderboardMode
) {
    @Expose
    @ConfigOption(name = "Wheat", desc = "")
    @ConfigEditorText
    val wheat: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Carrot", desc = "")
    @ConfigEditorText
    val carrot: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Potato", desc = "")
    @ConfigEditorText
    val potato: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Nether Wart", desc = "")
    @ConfigEditorText
    val wart: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Pumpkin", desc = "")
    @ConfigEditorText
    val pumpkin: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Melon", desc = "")
    @ConfigEditorText
    val melon: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Cocoa Beans", desc = "")
    @ConfigEditorText
    val cocoa: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Sugar Cane", desc = "")
    @ConfigEditorText
    val cane: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Cactus", desc = "")
    @ConfigEditorText
    val cactus: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Mushroom", desc = "")
    @ConfigEditorText
    val mushroom: Property<String> = Property.of("10000")

    val goalMap: Map<CropType, KProperty0<Property<String>>> = mapOf(
        CropType.WHEAT to this::wheat,
        CropType.CARROT to this::carrot,
        CropType.POTATO to this::potato,
        CropType.NETHER_WART to this::wart,
        CropType.PUMPKIN to this::pumpkin,
        CropType.MELON to this::melon,
        CropType.COCOA_BEANS to this::cocoa,
        CropType.SUGAR_CANE to this::cane,
        CropType.CACTUS to this::cactus,
        CropType.MUSHROOM to this::mushroom
    )
}
