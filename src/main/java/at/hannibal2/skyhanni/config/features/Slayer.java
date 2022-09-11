package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Slayer {

    @Expose
    @ConfigOption(name = "Slayer Miniboss Highlight", desc = "Highlight slayer miniboss in blue color")
    @ConfigEditorBoolean
    public boolean slayerMinibossHighlight = false;

    @Expose
    @ConfigOption(name = "Slayer Enderman Beacon", desc = "Highlight the enderman slayer Yang Glyph (Beacon) in red color (supports beacon in hand and beacon flying)")
    @ConfigEditorBoolean
    public boolean slayerEndermanBeacon = false;
}
