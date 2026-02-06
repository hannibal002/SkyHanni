package at.hannibal2.skyhanni.config.features.garden.cropmilestones

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CustomGoalConfig {
    @Expose
    @ConfigOption(name = "Wheat", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val wheat: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Carrot", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val carrot: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Potato", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val potato: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Nether Wart", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val wart: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Pumpkin", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val pumpkin: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Melon", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val melon: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Cocoa Beans", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val cocoa: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Sugar Cane", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val cane: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Cactus", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val cactus: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Mushroom", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val mushroom: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Moonflower", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val moonflower: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Sunflower", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val sunflower: Property<Float> = Property.of(46f)

    @Expose
    @ConfigOption(name = "Wild Rose", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val rose: Property<Float> = Property.of(46f)
}
