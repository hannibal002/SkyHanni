package at.hannibal2.skyhanni.config.features.mining;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class MineshaftWaypointsConfig {
    @Expose
    @ConfigOption(name = "Corpse Locator", desc = "")
    @Accordion
    public CorpseLocatorConfig corpseLocator = new CorpseLocatorConfig();

    @Expose
    @ConfigOption(name = "Share Waypoint Location", desc = "Shares the location of the nearest waypoint upon key press.\n" +
        "§eYou can share the location even if it has already been shared!")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int shareWaypointLocation = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Entrance Location", desc = "Marks the location of the entrance with a waypoint.")
    @ConfigEditorBoolean
    public boolean entranceLocation = false;

    @Expose
    @ConfigOption(name = "Ladder Location", desc = "Marks the location of the ladders at the bottom of the entrance with a waypoint.")
    @ConfigEditorBoolean
    public boolean ladderLocation = false;
}
