package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.*;
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

    @Expose
    @ConfigOption(name = "Hide Mob Names", desc = "Hide the name of the mobs you need to kill in order for the Slayer boss to spawn. Exclude mobs that are damaged, corrupted, runic or semi rare.")
    @ConfigEditorBoolean
    public boolean hideMobNames = false;

    @Expose
    @ConfigOption(name = "Blaze", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean damageSplash = false;

    @Expose
    @ConfigOption(name = "Fire Pillars", desc = "Cooldown when the Fire Pillars from the blaze slayer will kill you.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean firePillars = false;

    @Expose
    @ConfigOption(name = "Fire Pillars Position", desc = "")
    @ConfigEditorButton(runnableId = "firePillars", buttonText = "Edit")
    @ConfigAccordionId(id = 0)
    public Position firePillarsPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Blaze Daggers", desc = "Faster and permanent display for the blaze slayer daggers")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean blazeDaggers = false;
}
