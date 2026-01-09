package at.hannibal2.skyhanni.config.features.inventory

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class AdvancedEnchantmentColors {

    @Expose
    @ConfigOption(
        name = "Use Advanced Ultimate Color?",
        desc = "Enable this to override the color selected for Ultimate enchantments from above."
    )
    @ConfigEditorBoolean
    val useAdvancedUltimateColor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Advanced Ultimate Color", desc = "Select a custom color to use for Ultimate enchantments.")
    @ConfigEditorColour
    val advancedUltimateColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 85, 1))

    @Expose
    @ConfigOption(
        name = "Use Advanced Perfect Color?",
        desc = "Enable this to override the color selected for perfect level enchantments from above."
    )
    @ConfigEditorBoolean
    val useAdvancedPerfectColor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Advanced Perfect Color", desc = "Select a custom color to use for perfect enchantments.")
    @ConfigEditorColour
    val advancedPerfectColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 85, 1))

    @Expose
    @ConfigOption(
        name = "Use Advanced Great Color?",
        desc = "Enable this to override the color selected for great level enchantments from above."
    )
    @ConfigEditorBoolean
    val useAdvancedGreatColor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Advanced Great Color", desc = "Select a custom color to use for great enchantments.")
    @ConfigEditorColour
    val advancedGreatColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 85, 1))

    @Expose
    @ConfigOption(
        name = "Use Advanced Good Color?",
        desc = "Enable this to override the color selected for good level enchantments from above."
    )
    @ConfigEditorBoolean
    val useAdvancedGoodColor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Advanced Good Color", desc = "Select a custom color to use for good enchantments.")
    @ConfigEditorColour
    val advancedGoodColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 85, 1))

    @Expose
    @ConfigOption(
        name = "Use Advanced Poor Color?",
        desc = "Enable this to override the color selected for poor level enchantments from above."
    )
    @ConfigEditorBoolean
    val useAdvancedPoorColor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Advanced Poor Color", desc = "Select a custom color to use for poor enchantments.")
    @ConfigEditorColour
    val advancedPoorColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 85, 1))

}
