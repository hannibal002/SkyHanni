package at.hannibal2.skyhanni.config.features;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;

public class Slayer {

    @Expose
    @ConfigOption(name = "Enderman", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean enderman = false;

    @Expose
    @ConfigOption(name = "Enderman Beacon", desc = "Highlight the enderman slayer Yang Glyph (Beacon) in red color. Supports beacon in hand and beacon flying.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean slayerEndermanBeacon = false;

    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the enderman slayer.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean endermanPhaseDisplay = false;

    @Expose
    @ConfigOption(name = "Blaze", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean blaze = false;

    @Expose
    @ConfigOption(name = "Hellion Shields", desc = "")
    @ConfigEditorAccordion(id = 2)
    @ConfigAccordionId(id = 1)
    public boolean blazeHellion = false;

    @Expose
    @ConfigOption(name = "Colored Mobs", desc = "Color the blaze slayer boss and the demons in the right hellion shield color.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean blazeColoredMobs = false;

    @Expose
    @ConfigOption(name = "Blaze Daggers", desc = "Faster and permanent display for the Blaze Slayer daggers.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean blazeDaggers = false;

    @Expose
    @ConfigOption(name = "Right Dagger", desc = "Mark the right dagger to use for blaze slayer in the dagger overlay.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean blazeMarkRightHellionShield = false;

    @Expose
    @ConfigOption(name = "First Dagger", desc = "Select the first, left sided dagger for the display.")
    @ConfigEditorDropdown(values = {"Spirit/Crystal", "Ashen/Auric"})
    @ConfigAccordionId(id = 2)
    public int blazeFirstDagger = 0;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Remove the wrong blaze slayer dagger messages from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean blazeHideDaggerWarning = false;

    @Expose
    @ConfigOption(name = "Fire Pits", desc = "Warning when the fire pit phase starts for the Blaze Slayer tier 3 and 4.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean firePitsWarning = false;

    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the blaze slayer.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean blazePhaseDisplay = false;

    @Expose
    @ConfigOption(name = "Clear View", desc = "Hide particles and fireballs near blaze slayer bosses and demons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean blazeClearView = false;

    @Expose
    @ConfigOption(name = "Miniboss Highlight", desc = "Highlight slayer miniboss in blue color.")
    @ConfigEditorBoolean
    public boolean slayerMinibossHighlight = false;

    @Expose
    @ConfigOption(name = "Hide Mob Names", desc = "Hide the name of the mobs you need to kill in order for the Slayer boss to spawn. Exclude mobs that are damaged, corrupted, runic or semi rare.")
    @ConfigEditorBoolean
    public boolean hideMobNames = false;

    @Expose
    @ConfigOption(name = "Quest Warning", desc = "Warning when wrong slayer quest is selected, or killing mobs for the wrong slayer.")
    @ConfigEditorBoolean
    public boolean questWarning = true;
}
