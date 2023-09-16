package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MobsConfig {

    @Expose
    @ConfigOption(name = "Highlighters", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean highlighters = false;

    @Expose
    @ConfigOption(name = "Area Boss", desc = "Highlight Golden Ghoul, Old Wolf, Voidling Extremist and Millenia-Aged Blaze.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean areaBossHighlight = true;

    @Expose
    @ConfigOption(name = "Arachne Keeper", desc = "Highlight the Arachne Keeper in the Spider's Den in purple color.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean arachneKeeperHighlight = true;

    @Expose
    @ConfigOption(name = "Corleone", desc = "Highlight Boss Corleone in the Crystal Hollows.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean corleoneHighlighter = true;

    @Expose
    @ConfigOption(name = "Zealot", desc = "Highlight Zealots and Bruisers in The End.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean zealotBruiserHighlighter = false;

    @Expose
    @ConfigOption(
            name = "Special Zealots",
            desc = "Highlight Special Zealots (the ones that drop Summoning Eyes) in the End."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean specialZealotHighlighter = true;

    @Expose
    @ConfigOption(name = "Corrupted Mob", desc = "Highlight corrupted mobs in purple color.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean corruptedMobHighlight = false;

    @Expose
    @ConfigOption(name = "Arachne Boss", desc = "Highlight the Arachne boss in red and mini-bosses in orange.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean arachneBossHighlighter = true;

    @Expose
    @ConfigOption(name = "Respawn Timers", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean timers = false;

    @Expose
    @ConfigOption(
            name = "Area Boss",
            desc = "Show a timer when Golden Ghoul, Old Wolf, Voidling Extremist or Millenia-Aged Blaze respawns. " +
                    "§cSometimes it takes 20-30 seconds to calibrate correctly."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean areaBossRespawnTimer = false;

    @Expose
    @ConfigOption(name = "Enderman TP Hider", desc = "Stops the Enderman Teleportation animation.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean endermanTeleportationHider = true;

    @Expose
    @ConfigOption(name = "Arachne Minis Hider", desc = "Hides the nametag above arachne minis.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideNameTagArachneMinis = true;
}
