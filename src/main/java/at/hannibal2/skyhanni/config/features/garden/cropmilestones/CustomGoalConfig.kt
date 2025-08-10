package at.hannibal2.skyhanni.config.features.garden.cropmilestones

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CustomGoalConfig {
    @Expose
    @ConfigOption(name = "Wheat", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val wheat: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Carrot", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val carrot: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Potato", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val potato: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Nether Wart", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val wart: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Pumpkin", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val pumpkin: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Melon", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val melon: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Cocoa Beans", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val cocoa: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Sugar Cane", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val cane: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Cactus", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val cactus: Property<Int> = Property.of(46)

    @Expose
    @ConfigOption(name = "Mushroom", desc = "")
    @ConfigEditorSlider(minValue = 1f, maxValue = 46f, minStep = 1f)
    val mushroom: Property<Int> = Property.of(46)
}
