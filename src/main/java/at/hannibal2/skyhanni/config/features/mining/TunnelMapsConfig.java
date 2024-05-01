package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

public class TunnelMapsConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enables the tunnel maps, which give you a path to any location you want. Open the Inventory to select a destination.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enable = true;

    @ConfigLink(owner = TunnelMapsConfig.class, field = "enable")
    public Position position = new Position(20, 20);

    @Expose
    @ConfigOption(name = "Campfire Hotkey", desc = "Hotkey to warp to the campfire, if the travel scroll is not unlocked shows a path to the campfire.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int campfireKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Travel Scroll", desc = "Lets the mod know that you have unlocked the travel scroll to basecamp.")
    @ConfigEditorBoolean
    public boolean travelScroll = false;

    @Expose
    @ConfigOption(name = "Next Spot Hotkey", desc = "Hotkey to select the next spot.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int nextSpotHotkey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Dynamic Path Colour", desc = "Instead of the selected color use the color of the target as line colour.")
    @ConfigEditorBoolean
    public boolean dynamicPathColour = true;

    @Expose
    @ConfigOption(name = "Path Colour", desc = "The colour for the paths, if the dynamic colour option is turned off.")
    @ConfigEditorColour
    public String pathColour = "0:255:0:255:0";

    @Expose
    @ConfigOption(name = "Compact Gemstone", desc = "Only shows the icon for gemstones in the selection list.")
    @ConfigEditorBoolean
    public Property<Boolean> compactGemstone = Property.of(false);
}
