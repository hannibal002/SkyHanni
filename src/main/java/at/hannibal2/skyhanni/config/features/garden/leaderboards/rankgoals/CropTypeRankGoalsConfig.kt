package at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals

import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.TypeRankGoalGenericConfig
import at.hannibal2.skyhanni.features.garden.CropType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

class CropTypeRankGoalsConfig : TypeRankGoalGenericConfig<CropType>() {

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

    @Expose
    @ConfigOption(name = "Sunflower", desc = "")
    @ConfigEditorText
    val sunflower: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Moonflower", desc = "")
    @ConfigEditorText
    val moonflower: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Wild Rose", desc = "")
    @ConfigEditorText
    val rose: Property<String> = Property.of("10000")

    override fun getConfig(type: CropType): KProperty0<Property<String>> = when (type) {
        CropType.WHEAT -> this::wheat
        CropType.CARROT -> this::carrot
        CropType.POTATO -> this::potato
        CropType.NETHER_WART -> this::wart
        CropType.PUMPKIN -> this::pumpkin
        CropType.MELON -> this::melon
        CropType.COCOA_BEANS -> this::cocoa
        CropType.SUGAR_CANE -> this::cane
        CropType.CACTUS -> this::cactus
        CropType.MUSHROOM -> this::mushroom
        CropType.SUNFLOWER -> this::sunflower
        CropType.MOONFLOWER -> this::moonflower
        CropType.WILD_ROSE -> this::rose
    }
}
