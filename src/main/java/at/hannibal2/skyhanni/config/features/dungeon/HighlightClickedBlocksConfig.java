package at.hannibal2.skyhanni.config.features.dungeon;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HighlightClickedBlocksConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlight levers, chests, and Wither Essence when clicked in Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Chest Colour", desc = "Colour of clicked chests")
    @ConfigEditorColour
    public String chestColour = "0:178:85:255:85";

    @Expose
    @ConfigOption(name = "Trapped Chest Colour", desc = "Colour of clicked trapped chests")
    @ConfigEditorColour
    public String trappedChestColour = "0:178:0:170:0";

    @Expose
    @ConfigOption(name = "Locked Chest Colour", desc = "Colour of clicked locked chests")
    @ConfigEditorColour
    public String lockedChestColour = "0:178:255:85:85";

    @Expose
    @ConfigOption(name = "Wither Essence Colour", desc = "Colour of clicked wither essence")
    @ConfigEditorColour
    public String witherEssenceColour = "0:178:255:85:255";

    @Expose
    @ConfigOption(name = "Lever Colour", desc = "Colour of clicked levers")
    @ConfigEditorColour
    public String leverColour = "0:178:255:255:85";

    @Expose
    @ConfigOption(name = "Show Text", desc = "If enabled shows a text telling you what you clicked with the highlight.")
    @ConfigEditorBoolean
    public boolean showTextEnabled = true;

    @Expose
    @ConfigOption(name = "Random Colour", desc = "If enabled makes the colours random.")
    @ConfigEditorBoolean
    public boolean randomColourEnabled = false;

    @ConfigOption(name = "Reset Colors", desc = "Resets the colors of the highlights to default ones.")
    @ConfigEditorButton(buttonText = "Reset")
    public Runnable reset = () -> {
        chestColour = "0:178:85:255:85";
        trappedChestColour = "0:178:0:170:0";
        lockedChestColour = "0:178:255:85:85";
        witherEssenceColour = "0:178:255:85:255";
        leverColour = "0:178:255:255:85";
    };
}
