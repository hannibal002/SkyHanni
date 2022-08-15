package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigAccordionId;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorAccordion;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Inventory {

    @Expose
    @ConfigOption(name = "Not Clickable Items", desc = "Hide items that are not clickable in " + "the current inventory: ah, bz, accessory bag, etc.")
    @ConfigEditorBoolean
    public boolean hideNotClickableItems = false;

    @Expose
    @ConfigOption(name = "Item number as stack size", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean filterTypes = false;

    @Expose
    @ConfigOption(name = "Master Star Number", desc = "Show the Tier of the Master Star.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean displayMasterStarNumber = false;

    @Expose
    @ConfigOption(name = "Master Skull Number", desc = "Show the tier of the Master Skull accessory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean displayMasterSkullNumber = false;

    @Expose
    @ConfigOption(name = "Dungeon Head Floor", desc = "Show the correct floor for golden and diamond heads.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean displayDungeonHeadFloor = false;

    @Expose
    @ConfigOption(name = "New Year Cake", desc = "Show the Number of the Year of New Year Cakes.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean displayNewYearCakeNumber = false;

    @Expose
    @ConfigOption(name = "Pet Level", desc = "Show the level of the pet when not maxed.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean displayPetLevel = false;

    @Expose
    @ConfigOption(name = "Minion Tier", desc = "Show the Minion Tier over Items.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean displayMinionTier = false;

    @Expose
    @ConfigOption(name = "Sack Name", desc = "Show an abbreviation of the Sack name.")
    @ConfigEditorBoolean
    public boolean displaySackName = false;

    @Expose
    @ConfigOption(name = "Anvil Combine Helper", desc = "Suggests the same item in the inventory when trying to combine two items in the anvil.")
    @ConfigEditorBoolean
    public boolean anvilCombineHelper = false;
}
