package at.hannibal2.skyhanni.config.features.garden.customkeybinds;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.HashMap;
import java.util.Map;

public class KeyBindConfigCrops {

    @ConfigOption(name = "test button", desc = "test button")
    @ConfigEditorButton(buttonText = "test")
    public Runnable testButton = () -> {
    };

    private final Map<String, KeyBindCrop> crops = new HashMap<String, KeyBindCrop>() {{
        put("Wheat", wheat);
//         put("Carrot", 1);
//         put("Potato", 2);
//         put("Nether Wart", 3);
//         put("Pumpkin", 4);
//         put("Melon", 5);
//         put("Cocoa Beans", 6);
//         put("Sugar Cane", 7);
//         put("Cactus", 8);
//         put("Mushroom", 9);
    }};

    @Expose
    @ConfigOption(name = "Wheat", desc = "")
    @Accordion
    public KeyBindCrop wheat = new KeyBindCrop(this);


//     @Expose
//     @ConfigOption(name = "Carrot", desc = "")
//     @Accordion
//     public KeyBindCrop carrot = new KeyBindCrop("Carrot", this);
//
//     @Expose
//     @ConfigOption(name = "Potato", desc = "")
//     @Accordion
//     public KeyBindCrop potato = new KeyBindCrop("Potato", this);
//
//     @Expose
//     @ConfigOption(name = "Nether Wart", desc = "")
//     @Accordion
//     public KeyBindCrop netherWart = new KeyBindCrop("NetherWart", this);
//
//     @Expose
//     @ConfigOption(name = "Pumpkin", desc = "")
//     @Accordion
//     public KeyBindCrop pumpkin = new KeyBindCrop("Pumpkin", this);
//
//     @Expose
//     @ConfigOption(name = "Melon", desc = "")
//     @Accordion
//     public KeyBindCrop melon = new KeyBindCrop("Melon", this);
//
//     @Expose
//     @ConfigOption(name = "Cocoa Beans", desc = "")
//     @Accordion
//     public KeyBindCrop cocoaBeans = new KeyBindCrop("CocoaBeans", this);
//
//     @Expose
//     @ConfigOption(name = "Sugar Cane", desc = "")
//     @Accordion
//     public KeyBindCrop sugarCane = new KeyBindCrop("SugarCane", this);
//
//     @Expose
//     @ConfigOption(name = "Cactus", desc = "")
//     @Accordion
//     public KeyBindCrop cactus = new KeyBindCrop("Cactus", this);
//
//     @Expose
//     @ConfigOption(name = "Mushroom", desc = "")
//     @Accordion
//     public KeyBindCrop mushroom = new KeyBindCrop("Mushroom", this);
}
