package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorDraggableList;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class Inventory {

    @Expose
    @ConfigOption(name = "Not Clickable Items", desc = "Hide items that are not clickable in " + "the current inventory: ah, bz, accessory bag, etc.")
    @ConfigEditorBoolean
    public boolean hideNotClickableItems = false;

    @Expose
    @ConfigOption(
            name = "Item number as stack size",
            desc = ""
    )
    @ConfigEditorDraggableList(
            exampleText = {
                    "\u00a7bMaster Star Tier",
                    "\u00a7bMaster Skull Tier",
                    "\u00a7bDungeon Head Floor Number",
                    "\u00a7bNew Year Cake",
                    "\u00a7bPet Level",
                    "\u00a7bMinion Tier",
                    "\u00a7bCrimson Armor",
            }
    )
    public List<Integer> itemNumberAsStackSize = new ArrayList<>();

    @Expose
    @ConfigOption(name = "Sack Name", desc = "Show an abbreviation of the Sack name.")
    @ConfigEditorBoolean
    public boolean displaySackName = false;

    @Expose
    @ConfigOption(name = "Anvil Combine Helper", desc = "Suggests the same item in the inventory when trying to combine two items in the anvil.")
    @ConfigEditorBoolean
    public boolean anvilCombineHelper = false;

    @Expose
    @ConfigOption(name = "Item Stars",
            desc = "Show a compact star count in the item name for all items")
    @ConfigEditorBoolean
    public boolean itemStars = false;
}
