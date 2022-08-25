package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.annotations.*;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class Inventory {

    @ConfigOption(name = "Not Clickable Items", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean hideNotClickable = false;

    @Expose
    @ConfigOption(name = "Not Clickable Items Enabled", desc = "Hide items that are not clickable in the current inventory: ah, bz, accessory bag, etc.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hideNotClickableItems = false;

    @Expose
    @ConfigOption(
            name = "Opacity",
            desc = "How strong should the items be grayed out?"
    )
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 255,
            minStep = 5
    )
    @ConfigAccordionId(id = 0)
    public int hideNotClickableOpacity = 180;

    @Expose
    @ConfigOption(
            name = "Item number",
            desc = "Showing the item number as a stack size for these items"
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

    @ConfigOption(name = "RNG Meter", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean rngMeter = false;

    @Expose
    @ConfigOption(name = "Floor Names", desc = "Show the floor names in the catacombs rng meter inventory")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean rngMeterFloorName = false;

    @Expose
    @ConfigOption(name = "No Drop", desc = "Highlight floors without a drop selected in the catacombs rng meter inventory")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean rngMeterNoDrop = false;

    @Expose
    @ConfigOption(name = "Selected Drop", desc = "Highlight the selected drop in the catacombs oder slayer rng meter inventory")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean rngMeterSelectedDrop = false;
}
