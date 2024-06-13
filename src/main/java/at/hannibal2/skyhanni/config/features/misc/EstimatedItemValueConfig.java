package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

public class EstimatedItemValueConfig {
    @Expose
    @ConfigOption(name = "Enable Estimated Price", desc = "Display an Estimated Item Value for the item you hover over.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Hotkey", desc = "Press this key to show the Estimated Item Value.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int hotkey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Show Always", desc = "Ignore the hotkey and always display the item value.")
    @ConfigEditorBoolean
    public boolean alwaysEnabled = true;

    @Expose
    @ConfigOption(name = "Enchantments Cap", desc = "Only show the top # most expensive enchantments.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 30,
        minStep = 1
    )
    public Property<Integer> enchantmentsCap = Property.of(7);

    @Expose
    @ConfigOption(name = "Show Exact Price", desc = "Show the exact total price instead of the compact number.")
    @ConfigEditorBoolean
    public boolean exactPrice = false;

    @Expose
    @ConfigOption(name = "Show Armor Value", desc = "Show the value of the full armor set in the Wardrobe inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean armor = true;

    @Expose
    @ConfigOption(name = "Ignore Helmet Skins", desc = "Ignore helmet Skins from the total value.")
    @ConfigEditorBoolean
    public boolean ignoreHelmetSkins = false;

    @Expose
    @ConfigOption(name = "Ignore Armor Dyes", desc = "Ignore Armor Dyes from the total value.")
    @ConfigEditorBoolean
    public boolean ignoreArmorDyes = false;

    @Expose
    @ConfigOption(name = "Ignore Runes", desc = "Ignore Runes from the total value.")
    @ConfigEditorBoolean
    public boolean ignoreRunes = false;

    @Expose
    @ConfigLink(owner = EstimatedItemValueConfig.class, field = "enabled")
    public Position itemPriceDataPos = new Position(140, 90, false, true);
}
