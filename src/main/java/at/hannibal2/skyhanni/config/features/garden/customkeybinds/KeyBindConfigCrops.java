package at.hannibal2.skyhanni.config.features.garden.customkeybinds;

import at.hannibal2.skyhanni.features.garden.CropType;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.HashMap;
import java.util.Map;

public class KeyBindConfigCrops {

    @ConfigOption(name = "test button", desc = "test button")
    @ConfigEditorButton(buttonText = "test")
    public Runnable testButton = () -> {
    };


    @ConfigOption(name = "Copy Keybinds", desc = "Copy keybinds from another crop.")
    @ConfigEditorButton(buttonText = "Copy Keybinds")
    public Runnable copyKeybinds = this::CopyOverKeybinds;

    @Expose
    @ConfigOption(name = "Copy Keybinds from: ", desc = "Select what crop to copy keybind of.")
    @ConfigEditorDropdown()
    public cropTypeToCopy copyKeybindsFrom = cropTypeToCopy.Wheat;

    @Expose
    @ConfigOption(name = "Copy Keybinds to: ", desc = "Select what crop to copy keybind to.")
    @ConfigEditorDropdown()
    public cropTypeToCopy copyKeybindsTo = cropTypeToCopy.Wheat;

    public enum cropTypeToCopy {
        Wheat("Wheat"),
        Carrot("Carrot"),
        Potato("Potato"),
        NetherWart("Nether Wart"),
        Pumpkin("Pumpkin"),
        Melon("Melon"),
        CocoaBeans("Cocoa Beans"),
        SugarCane("Sugar Cane"),
        Cactus("Cactus"),
        Mushroom("Mushroom");

        private final String str;

        cropTypeToCopy(String str) {
            this.str = str;
        }
    }

    private void CopyOverKeybinds() {
        System.out.println("Copying keybinds from " + copyKeybindsFrom.str + " to " + copyKeybindsTo.str + crops.containsKey(copyKeybindsFrom.str));
        if (copyKeybindsFrom == copyKeybindsTo) return;
        if (copyKeybindsFrom == null || copyKeybindsTo == null) return;


        if (!crops.containsKey(copyKeybindsFrom.str) || !crops.containsKey(copyKeybindsTo.str)) return;

        System.out.print("hereeeee2");
        KeyBindCrop sourceCropType = crops.get(copyKeybindsFrom.str);
        KeyBindCrop destinationCropType = crops.get(copyKeybindsTo.str);
        System.out.print("hereeeee3");
        System.out.print(sourceCropType);
        System.out.print("hereeeee4");

        System.out.println(sourceCropType.getClass().getName() + destinationCropType.getClass().getName());

        System.out.print("hereeeee5");
        if (sourceCropType == null || destinationCropType == null) return;
        System.out.print("hereeeee6");

        System.out.print(sourceCropType.attack + " " + sourceCropType.useItem + " " + sourceCropType.left + " " + sourceCropType.right + " " + sourceCropType.forward + " " + sourceCropType.back + " " + sourceCropType.jump + " " + sourceCropType.sneak);
        System.out.print("hereeeee7");
        System.out.print(destinationCropType.attack + " " + destinationCropType.useItem + " " + destinationCropType.left + " " + destinationCropType.right + " " + destinationCropType.forward + " " + destinationCropType.back + " " + destinationCropType.jump + " " + destinationCropType.sneak);

        destinationCropType.attack = sourceCropType.attack;
        destinationCropType.useItem = sourceCropType.useItem;
        destinationCropType.left = sourceCropType.left;
        destinationCropType.right = sourceCropType.right;
        destinationCropType.forward = sourceCropType.forward;
        destinationCropType.back = sourceCropType.back;
        destinationCropType.jump = sourceCropType.jump;
        destinationCropType.sneak = sourceCropType.sneak;
    }




    @Expose
    @ConfigOption(name = "Wheat", desc = "")
    @Accordion
    public KeyBindCrop wheat = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Carrot", desc = "")
    @Accordion
    public KeyBindCrop carrot = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Potato", desc = "")
    @Accordion
    public KeyBindCrop potato = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Nether Wart", desc = "")
    @Accordion
    public KeyBindCrop netherWart = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Pumpkin", desc = "")
    @Accordion
    public KeyBindCrop pumpkin = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Melon", desc = "")
    @Accordion
    public KeyBindCrop melon = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Cocoa Beans", desc = "")
    @Accordion
    public KeyBindCrop cocoaBeans = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Sugar Cane", desc = "")
    @Accordion
    public KeyBindCrop sugarCane = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Cactus", desc = "")
    @Accordion
    public KeyBindCrop cactus = new KeyBindCrop();

    @Expose
    @ConfigOption(name = "Mushroom", desc = "")
    @Accordion
    public KeyBindCrop mushroom = new KeyBindCrop();

    private final Map<String, KeyBindCrop> crops = new HashMap<String, KeyBindCrop>() {{
        put("Wheat", wheat);
        put("Carrot", carrot);
        put("Potato", potato);
        put("Nether Wart", netherWart);
        put("Pumpkin", pumpkin);
        put("Melon", melon);
        put("Cocoa Beans", cocoaBeans);
        put("Sugar Cane", sugarCane);
        put("Cactus", cactus);
        put("Mushroom", mushroom);
    }};

}
