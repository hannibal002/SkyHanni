package at.hannibal2.skyhanni.config.features.enchantparsing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.utils.LorenzColor;
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
    @ConfigOption(name = "Format", desc = "The way the enchants are formatted in the tooltip.")
    @ConfigEditorDropdown()
    public EnchantFormat format = EnchantFormat.NORMAL;

    public enum EnchantFormat {
        NORMAL("Normal"),
        COMPRESSED("Compressed"),
        STACKED("Stacked");

        public final String str;

        EnchantFormat(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

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
        @ConfigEditorDropdown()
        public LorenzColor perfectEnchantColor = LorenzColor.CHROMA;

        @Expose
        @ConfigOption(name = "Great Enchantment Color", desc = "The color an enchantment will be at a great level.")
        @ConfigEditorDropdown()
        public LorenzColor greatEnchantColor = LorenzColor.GOLD;

        @Expose
        @ConfigOption(name = "Good Enchantment Color", desc = "The color an enchantment will be at a good level.")
        @ConfigEditorDropdown()
        public LorenzColor goodEnchantColor = LorenzColor.BLUE;

        @Expose
        @ConfigOption(name = "Poor Enchantment Color", desc = "The color an enchantment will be at a poor level.")
        @ConfigEditorDropdown()
        public LorenzColor poorEnchantColor = LorenzColor.GRAY;

        @Expose
        @ConfigOption(name = "Comma Format", desc = "Change the format of the comma after each enchant.")
        @ConfigEditorDropdown()
        public CommaFormat commaFormat = CommaFormat.COPY_ENCHANT;

        public enum CommaFormat {
            COPY_ENCHANT("Copy enchant format"),
            DEFAULT("Default (Blue)");

            public final String str;

            CommaFormat(String str) {
                this.str = str;
            }

            @Override
            public String toString() {
                return str;
            }
        }

    }
}
