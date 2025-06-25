package at.hannibal2.skyhanni.config.features.garden.optimalspeed

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CustomSpeedConfig {
    @Expose
    @ConfigOption(
        name = "Wheat",
        desc = "Suggested farm speed:\n" +
            "§e5 Blocks§7: §f✦ 93 speed\n" +
            "§e4 Blocks§7: §f✦ 116 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val wheat: Property<Float> = Property.of(93f)

    @Expose
    @ConfigOption(
        name = "Carrot",
        desc = "Suggested farm speed:\n" +
            "§e5 Blocks§7: §f✦ 93 speed\n" +
            "§e4 Blocks§7: §f✦ 116 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val carrot: Property<Float> = Property.of(93f)

    @Expose
    @ConfigOption(
        name = "Potato",
        desc = "Suggested farm speed:\n" +
            "§e5 Blocks§7: §f✦ 93 speed\n" +
            "§e4 Blocks§7: §f✦ 116 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val potato: Property<Float> = Property.of(93f)

    @Expose
    @ConfigOption(
        name = "Nether Wart",
        desc = "Suggested farm speed:\n" +
            "§e5 Blocks§7: §f✦ 93 speed\n" +
            "§e4 Blocks§7: §f✦ 116 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val netherWart: Property<Float> = Property.of(93f)

    @Expose
    @ConfigOption(
        name = "Pumpkin",
        desc = "Suggested farm speed:\n" +
            "§e3 Blocks§7: §f✦ 155 speed\n" +
            "§e2 Blocks§7: §f✦ 265 §7or §f400 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val pumpkin: Property<Float> = Property.of(155f)

    @Expose
    @ConfigOption(
        name = "Melon",
        desc = "Suggested farm speed:\n" +
            "§e3 Blocks§7: §f✦ 155 speed\n" +
            "§e2 Blocks§7: §f✦ 265 or 400 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val melon: Property<Float> = Property.of(155f)

    @Expose
    @ConfigOption(
        name = "Cocoa Beans",
        desc = "Suggested farm speed:\n" +
            "§e3 Blocks§7: §f✦ 155 speed\n" +
            "§e4 Blocks§7: §f✦ 116 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val cocoaBeans: Property<Float> = Property.of(155f)

    // TODO does other speed settings exist?
    @Expose
    @ConfigOption(
        name = "Sugar Cane",
        desc = "Suggested farm speed:\n" +
            "§eYaw 45§7: §f✦ 328 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val sugarCane: Property<Float> = Property.of(328f)

    @Expose
    @ConfigOption(
        name = "Cactus",
        desc = "Suggested farm speed:\n" +
            "§eYaw 90§7: §f✦ 464 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 500f, minStep = 1f)
    val cactus: Property<Float> = Property.of(464f)

    // TODO does other speed settings exist?
    @Expose
    @ConfigOption(
        name = "Mushroom",
        desc = "Suggested farm speed:\n" +
            "§eYaw 60§7: §f✦ 233 speed",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 400f, minStep = 1f)
    val mushroom: Property<Float> = Property.of(233f)
}
