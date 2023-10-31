package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class EnchantParsingConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Global Toggle for entire category")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Hide Vanilla Enchants", desc = "Hide the regular vanilla enchants usually found in the first 1-2 lines of lore.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideVanillaEnchants = true;

    @Expose
    @ConfigOption(name = "Hide Enchant Description", desc = "Hides the enchant description after each enchant if available.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideEnchantDescriptions = false;

    @Expose
    @ConfigOption(name = "Enchant Coloring", desc = "")
    @Accordion
    public ColorEnchants colorEnchants = new ColorEnchants();

    public static class ColorEnchants {

        @Expose
        @ConfigOption(name = "Enable", desc = "Toggle for coloring the enchants. Turn this off if you want to use enchant parsing from other mods.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean colorParsing = true;

        @Expose
        @ConfigOption(name = "Perfect Enchantment Color", desc = "The color an enchantment will be at max level.")
        @ConfigEditorDropdown(values = {"§0Black", "§1Dark Blue", "§2Dark Green", "§3Dark Aqua", "§4Dark Red",
            "§5Dark Purple", "§6Gold", "§7Gray", "§8Dark Gray", "§9Blue", "§aGreen", "§bAqua",
            "§cRed", "§dPink", "§eYellow", "§fWhite", "§ZChroma"})
        public int perfectEnchantColor = 16;

        @Expose
        @ConfigOption(name = "Great Enchantment Color", desc = "The color an enchantment will be at a great level.")
        @ConfigEditorDropdown(values = {"§0Black", "§1Dark Blue", "§2Dark Green", "§3Dark Aqua", "§4Dark Red",
            "§5Dark Purple", "§6Gold", "§7Gray", "§8Dark Gray", "§9Blue", "§aGreen", "§bAqua",
            "§cRed", "§dPink", "§eYellow", "§fWhite", "§ZChroma"})
        public int greatEnchantColor = 6;

        @Expose
        @ConfigOption(name = "Good Enchantment Color", desc = "The color an enchantment will be at a good level.")
        @ConfigEditorDropdown(values = {"§0Black", "§1Dark Blue", "§2Dark Green", "§3Dark Aqua", "§4Dark Red",
            "§5Dark Purple", "§6Gold", "§7Gray", "§8Dark Gray", "§9Blue", "§aGreen", "§bAqua",
            "§cRed", "§dPink", "§eYellow", "§fWhite", "§ZChroma"})
        public int goodEnchantColor = 9;

        @Expose
        @ConfigOption(name = "Poor Enchantment Color", desc = "The color an enchantment will be at a poor level.")
        @ConfigEditorDropdown(values = {"§0Black", "§1Dark Blue", "§2Dark Green", "§3Dark Aqua", "§4Dark Red",
            "§5Dark Purple", "§6Gold", "§7Gray", "§8Dark Gray", "§9Blue", "§aGreen", "§bAqua",
            "§cRed", "§dPink", "§eYellow", "§fWhite", "§ZChroma"})
        public int poorEnchantColor = 7;

        @Expose
        @ConfigOption(name = "Comma Format", desc = "Change the format of the comma after each enchant.")
        @ConfigEditorDropdown(values = {"Copy enchant format", "Default (Blue)"})
        public int commaFormat = 0;

    }
}
