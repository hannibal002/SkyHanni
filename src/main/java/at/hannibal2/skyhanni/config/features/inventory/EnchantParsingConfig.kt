package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class EnchantParsingConfig {
    @Expose
    @ConfigOption(
        name = "Enable",
        desc = "Toggle for coloring the enchants. Turn this off if you want to use enchant parsing from other mods."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val colorParsing: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Format", desc = "The way the enchants are formatted in the tooltip.")
    @ConfigEditorDropdown
    val format: Property<EnchantFormat> = Property.of(EnchantFormat.NORMAL)

    enum class EnchantFormat(private val displayName: String) {
        NORMAL("Normal"),
        COMPRESSED("Compressed"),
        STACKED("Stacked"),
        ;

        override fun toString() = displayName
    }

    @ConfigOption(
        name = "§cChroma Warning",
        desc = "Chroma requires a separate setting.\n§eIf SkyHanni chroma is disabled Chroma will default to §6Gold.",
    )
    @ConfigEditorButton(buttonText = "Go")
    val chromaRunnable = Runnable { SkyHanniMod.feature.gui.chroma::enabled.jumpToEditor() }

    @Expose
    @ConfigOption(name = "Ultimate Enchantment Color", desc = "The color the Ultimate enchantment will be. (Will always be bold)")
    @ConfigEditorDropdown
    val ultimateEnchantColor: Property<LorenzColor> = Property.of(LorenzColor.LIGHT_PURPLE)

    @Expose
    @ConfigOption(name = "Perfect Enchantment Color", desc = "The color an enchantment will be at max level.")
    @ConfigEditorDropdown
    val perfectEnchantColor: Property<LorenzColor> = Property.of(LorenzColor.CHROMA)

    @Expose
    @ConfigOption(name = "Perfect Enchantment Bold", desc = "Enchantments at max level will be bold.")
    @ConfigEditorBoolean
    val boldPerfectEnchant: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Great Enchantment Color", desc = "The color an enchantment will be at a great level.")
    @ConfigEditorDropdown
    val greatEnchantColor: Property<LorenzColor> = Property.of(LorenzColor.GOLD)

    @Expose
    @ConfigOption(name = "Good Enchantment Color", desc = "The color an enchantment will be at a good level.")
    @ConfigEditorDropdown
    val goodEnchantColor: Property<LorenzColor> = Property.of(LorenzColor.BLUE)

    @Expose
    @ConfigOption(name = "Poor Enchantment Color", desc = "The color an enchantment will be at a poor level.")
    @ConfigEditorDropdown
    val poorEnchantColor: Property<LorenzColor> = Property.of(LorenzColor.GRAY)

    @Expose
    @ConfigOption(name = "Advanced Enchantment Colors", desc = "")
    @Accordion
    val advancedEnchantColors: AdvancedEnchantmentColors = AdvancedEnchantmentColors()

    @Expose
    @ConfigOption(
        name = "Hide Vanilla Enchants",
        desc = "Hide the regular vanilla enchants usually found in the first 1-2 lines of lore."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val hideVanillaEnchants: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Hide Enchant Description",
        desc = "Hide the enchant description after each enchant if available."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val hideEnchantDescriptions: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Stacking Enchant Progress",
        desc = "Shows the stacking enchant progress at the bottom of the lore. " +
            "§eRequires Enchant Parsing to be enabled."
    )
    @ConfigEditorBoolean
    var stackingEnchantProgress: Boolean = true
}
