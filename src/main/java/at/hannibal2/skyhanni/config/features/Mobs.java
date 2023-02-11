package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.ConfigAccordionId;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorAccordion;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Mobs {

    @Expose
    @ConfigOption(
            name = "Area Boss",
            desc = "Golden Ghoul, Old Wolf, Voidling Extremist and Millenia-Aged Blaze"
    )
    @ConfigEditorAccordion(id = 0)
    public boolean areaBosses = false;

    @Expose
    @ConfigOption(name = "Highlight Area Boss", desc = "Highlight area boss.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean areaBossHighlight = true;

    @Expose
    @ConfigOption(name = "Area Bosses Respawn Timer", desc = "Show a timer when the area boss spawns.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean areaBossRespawnTimer = false;

    @Expose
    @ConfigOption(name = "Corrupted Mob Highlight", desc = "Highlight corrupted mobs in purple color.")
    @ConfigEditorBoolean
    public boolean corruptedMobHighlight = false;

    @Expose
    @ConfigOption(name = "Arachne Keeper Highlight", desc = "Highlight corrupted mobs in purple color.")
    @ConfigEditorBoolean
    public boolean arachneKeeperHighlight = true;

    @Expose
    @ConfigOption(name = "Corleone Highlighter", desc = "Highlight Boss Corleone in the Crystal Hollows.")
    @ConfigEditorBoolean
    public boolean corleoneHighlighter = true;

    @Expose
    @ConfigOption(name = "Zealots Highlighter", desc = "Highlight Zealots and Bruisers in The End.")
    @ConfigEditorBoolean
    public boolean zealotBruiserHighlighter = false;

    @Expose
    @ConfigOption(name = "Special Zealots Highlighter", desc = "Highlight Special Zealots in The End. (The one that drops Summoning Eyes)")
    @ConfigEditorBoolean
    public boolean specialZealotHighlighter = true;
}
