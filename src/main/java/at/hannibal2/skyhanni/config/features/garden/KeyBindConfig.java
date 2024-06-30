package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class KeyBindConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Use custom keybinds while holding a farming tool in the hand.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Exclude Barn", desc = "Disable this feature while on the barn plot.")
    @ConfigEditorBoolean
    public boolean excludeBarn = false;

    @ConfigOption(name = "Disable All", desc = "Disable all keys.")
    @ConfigEditorButton(buttonText = "Disable")
    public Runnable presetDisable = () -> {
        wheat_attack = Keyboard.KEY_NONE;
        wheat_useItem = Keyboard.KEY_NONE;
        wheat_left = Keyboard.KEY_NONE;
        wheat_right = Keyboard.KEY_NONE;
        wheat_forward = Keyboard.KEY_NONE;
        wheat_back = Keyboard.KEY_NONE;
        wheat_jump = Keyboard.KEY_NONE;
        wheat_sneak = Keyboard.KEY_NONE;

        carrot_attack = Keyboard.KEY_NONE;
        carrot_useItem = Keyboard.KEY_NONE;
        carrot_left = Keyboard.KEY_NONE;
        carrot_right = Keyboard.KEY_NONE;
        carrot_forward = Keyboard.KEY_NONE;
        carrot_back = Keyboard.KEY_NONE;
        carrot_jump = Keyboard.KEY_NONE;
        carrot_sneak = Keyboard.KEY_NONE;

        potato_attack = Keyboard.KEY_NONE;
        potato_useItem = Keyboard.KEY_NONE;
        potato_left = Keyboard.KEY_NONE;
        potato_right = Keyboard.KEY_NONE;
        potato_forward = Keyboard.KEY_NONE;
        potato_back = Keyboard.KEY_NONE;
        potato_jump = Keyboard.KEY_NONE;
        potato_sneak = Keyboard.KEY_NONE;

        netherWart_attack = Keyboard.KEY_NONE;
        netherWart_useItem = Keyboard.KEY_NONE;
        netherWart_left = Keyboard.KEY_NONE;
        netherWart_right = Keyboard.KEY_NONE;
        netherWart_forward = Keyboard.KEY_NONE;
        netherWart_back = Keyboard.KEY_NONE;
        netherWart_jump = Keyboard.KEY_NONE;
        netherWart_sneak = Keyboard.KEY_NONE;

        pumpkin_attack = Keyboard.KEY_NONE;
        pumpkin_useItem = Keyboard.KEY_NONE;
        pumpkin_left = Keyboard.KEY_NONE;
        pumpkin_right = Keyboard.KEY_NONE;
        pumpkin_forward = Keyboard.KEY_NONE;
        pumpkin_back = Keyboard.KEY_NONE;
        pumpkin_jump = Keyboard.KEY_NONE;
        pumpkin_sneak = Keyboard.KEY_NONE;

        melon_attack = Keyboard.KEY_NONE;
        melon_useItem = Keyboard.KEY_NONE;
        melon_left = Keyboard.KEY_NONE;
        melon_right = Keyboard.KEY_NONE;
        melon_forward = Keyboard.KEY_NONE;
        melon_back = Keyboard.KEY_NONE;
        melon_jump = Keyboard.KEY_NONE;
        melon_sneak = Keyboard.KEY_NONE;

        cocoaBeans_attack = Keyboard.KEY_NONE;
        cocoaBeans_useItem = Keyboard.KEY_NONE;
        cocoaBeans_left = Keyboard.KEY_NONE;
        cocoaBeans_right = Keyboard.KEY_NONE;
        cocoaBeans_forward = Keyboard.KEY_NONE;
        cocoaBeans_back = Keyboard.KEY_NONE;
        cocoaBeans_jump = Keyboard.KEY_NONE;
        cocoaBeans_sneak = Keyboard.KEY_NONE;

        sugarCane_attack = Keyboard.KEY_NONE;
        sugarCane_useItem = Keyboard.KEY_NONE;
        sugarCane_left = Keyboard.KEY_NONE;
        sugarCane_right = Keyboard.KEY_NONE;
        sugarCane_forward = Keyboard.KEY_NONE;
        sugarCane_back = Keyboard.KEY_NONE;
        sugarCane_jump = Keyboard.KEY_NONE;
        sugarCane_sneak = Keyboard.KEY_NONE;

        cactus_attack = Keyboard.KEY_NONE;
        cactus_useItem = Keyboard.KEY_NONE;
        cactus_left = Keyboard.KEY_NONE;
        cactus_right = Keyboard.KEY_NONE;
        cactus_forward = Keyboard.KEY_NONE;
        cactus_back = Keyboard.KEY_NONE;
        cactus_jump = Keyboard.KEY_NONE;
        cactus_sneak = Keyboard.KEY_NONE;

        mushroom_attack = Keyboard.KEY_NONE;
        mushroom_useItem = Keyboard.KEY_NONE;
        mushroom_left = Keyboard.KEY_NONE;
        mushroom_right = Keyboard.KEY_NONE;
        mushroom_forward = Keyboard.KEY_NONE;
        mushroom_back = Keyboard.KEY_NONE;
        mushroom_jump = Keyboard.KEY_NONE;
        mushroom_sneak = Keyboard.KEY_NONE;

        Minecraft.getMinecraft().thePlayer.closeScreen();
    };

    @ConfigOption(name = "Set Default", desc = "Reset all keys to default.")
    @ConfigEditorButton(buttonText = "Default")
    public Runnable presetDefault = () -> {
        wheat_attack = -100;
        wheat_useItem = -99;
        wheat_left = Keyboard.KEY_A;
        wheat_right = Keyboard.KEY_D;
        wheat_forward = Keyboard.KEY_W;
        wheat_back = Keyboard.KEY_S;
        wheat_jump = Keyboard.KEY_SPACE;
        wheat_sneak = Keyboard.KEY_LSHIFT;

        carrot_attack = -100;
        carrot_useItem = -99;
        carrot_left = Keyboard.KEY_A;
        carrot_right = Keyboard.KEY_D;
        carrot_forward = Keyboard.KEY_W;
        carrot_back = Keyboard.KEY_S;
        carrot_jump = Keyboard.KEY_SPACE;
        carrot_sneak = Keyboard.KEY_LSHIFT;

        potato_attack = -100;
        potato_useItem = -99;
        potato_left = Keyboard.KEY_A;
        potato_right = Keyboard.KEY_D;
        potato_forward = Keyboard.KEY_W;
        potato_back = Keyboard.KEY_S;
        potato_jump = Keyboard.KEY_SPACE;
        potato_sneak = Keyboard.KEY_LSHIFT;

        netherWart_attack = -100;
        netherWart_useItem = -99;
        netherWart_left = Keyboard.KEY_A;
        netherWart_right = Keyboard.KEY_D;
        netherWart_forward = Keyboard.KEY_W;
        netherWart_back = Keyboard.KEY_S;
        netherWart_jump = Keyboard.KEY_SPACE;
        netherWart_sneak = Keyboard.KEY_LSHIFT;

        pumpkin_attack = -100;
        pumpkin_useItem = -99;
        pumpkin_left = Keyboard.KEY_A;
        pumpkin_right = Keyboard.KEY_D;
        pumpkin_forward = Keyboard.KEY_W;
        pumpkin_back = Keyboard.KEY_S;
        pumpkin_jump = Keyboard.KEY_SPACE;
        pumpkin_sneak = Keyboard.KEY_LSHIFT;

        melon_attack = -100;
        melon_useItem = -99;
        melon_left = Keyboard.KEY_A;
        melon_right = Keyboard.KEY_D;
        melon_forward = Keyboard.KEY_W;
        melon_back = Keyboard.KEY_S;
        melon_jump = Keyboard.KEY_SPACE;
        melon_sneak = Keyboard.KEY_LSHIFT;

        cocoaBeans_attack = -100;
        cocoaBeans_useItem = -99;
        cocoaBeans_left = Keyboard.KEY_A;
        cocoaBeans_right = Keyboard.KEY_D;
        cocoaBeans_forward = Keyboard.KEY_W;
        cocoaBeans_back = Keyboard.KEY_S;
        cocoaBeans_jump = Keyboard.KEY_SPACE;
        cocoaBeans_sneak = Keyboard.KEY_LSHIFT;

        sugarCane_attack = -100;
        sugarCane_useItem = -99;
        sugarCane_left = Keyboard.KEY_A;
        sugarCane_right = Keyboard.KEY_D;
        sugarCane_forward = Keyboard.KEY_W;
        sugarCane_back = Keyboard.KEY_S;
        sugarCane_jump = Keyboard.KEY_SPACE;
        sugarCane_sneak = Keyboard.KEY_LSHIFT;

        cactus_attack = -100;
        cactus_useItem = -99;
        cactus_left = Keyboard.KEY_A;
        cactus_right = Keyboard.KEY_D;
        cactus_forward = Keyboard.KEY_W;
        cactus_back = Keyboard.KEY_S;
        cactus_jump = Keyboard.KEY_SPACE;
        cactus_sneak = Keyboard.KEY_LSHIFT;

        mushroom_attack = -100;
        mushroom_useItem = -99;
        mushroom_left = Keyboard.KEY_A;
        mushroom_right = Keyboard.KEY_D;
        mushroom_forward = Keyboard.KEY_W;
        mushroom_back = Keyboard.KEY_S;
        mushroom_jump = Keyboard.KEY_SPACE;
        mushroom_sneak = Keyboard.KEY_LSHIFT;

        Minecraft.getMinecraft().thePlayer.closeScreen();
    };

    // Add wheat keybinds
    @Expose
    @ConfigOption(name = "Wheat: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int wheat_attack = -100;

    @Expose
    @ConfigOption(name = "Wheat: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int wheat_useItem = -99;

    @Expose
    @ConfigOption(name = "Wheat: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int wheat_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Wheat: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int wheat_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Wheat: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int wheat_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Wheat: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int wheat_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Wheat: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int wheat_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Wheat: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int wheat_sneak = Keyboard.KEY_LSHIFT;

    // Add carrot keybinds
    @Expose
    @ConfigOption(name = "Carrot: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int carrot_attack = -100;

    @Expose
    @ConfigOption(name = "Carrot: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int carrot_useItem = -99;

    @Expose
    @ConfigOption(name = "Carrot: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int carrot_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Carrot: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int carrot_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Carrot: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int carrot_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Carrot: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int carrot_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Carrot: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int carrot_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Carrot: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int carrot_sneak = Keyboard.KEY_LSHIFT;

    // Add potato keybinds
    @Expose
    @ConfigOption(name = "Potato: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int potato_attack = -100;

    @Expose
    @ConfigOption(name = "Potato: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int potato_useItem = -99;

    @Expose
    @ConfigOption(name = "Potato: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int potato_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Potato: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int potato_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Potato: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int potato_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Potato: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int potato_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Potato: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int potato_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Potato: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int potato_sneak = Keyboard.KEY_LSHIFT;

    // Add nether wart keybinds
    @Expose
    @ConfigOption(name = "Nether Wart: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int netherWart_attack = -100;

    @Expose
    @ConfigOption(name = "Nether Wart: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int netherWart_useItem = -99;

    @Expose
    @ConfigOption(name = "Nether Wart: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int netherWart_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Nether Wart: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int netherWart_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Nether Wart: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int netherWart_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Nether Wart: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int netherWart_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Nether Wart: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int netherWart_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Nether Wart: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int netherWart_sneak = Keyboard.KEY_LSHIFT;

    // Add pumpkin keybinds
    @Expose
    @ConfigOption(name = "Pumpkin: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int pumpkin_attack = -100;

    @Expose
    @ConfigOption(name = "Pumpkin: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int pumpkin_useItem = -99;

    @Expose
    @ConfigOption(name = "Pumpkin: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int pumpkin_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Pumpkin: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int pumpkin_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Pumpkin: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int pumpkin_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Pumpkin: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int pumpkin_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Pumpkin: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int pumpkin_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Pumpkin: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int pumpkin_sneak = Keyboard.KEY_LSHIFT;

    // Add melon keybinds
    @Expose
    @ConfigOption(name = "Melon: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int melon_attack = -100;

    @Expose
    @ConfigOption(name = "Melon: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int melon_useItem = -99;

    @Expose
    @ConfigOption(name = "Melon: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int melon_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Melon: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int melon_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Melon: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int melon_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Melon: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int melon_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Melon: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int melon_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Melon: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int melon_sneak = Keyboard.KEY_LSHIFT;

    // Add cocoa beans keybinds
    @Expose
    @ConfigOption(name = "Cocoa Beans: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int cocoaBeans_attack = -100;

    @Expose
    @ConfigOption(name = "Cocoa Beans: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int cocoaBeans_useItem = -99;

    @Expose
    @ConfigOption(name = "Cocoa Beans: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int cocoaBeans_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Cocoa Beans: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int cocoaBeans_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Cocoa Beans: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int cocoaBeans_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Cocoa Beans: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int cocoaBeans_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Cocoa Beans: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int cocoaBeans_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Cocoa Beans: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int cocoaBeans_sneak = Keyboard.KEY_LSHIFT;

    // Add sugar cane keybinds
    @Expose
    @ConfigOption(name = "Sugar Cane: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int sugarCane_attack = -100;

    @Expose
    @ConfigOption(name = "Sugar Cane: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int sugarCane_useItem = -99;

    @Expose
    @ConfigOption(name = "Sugar Cane: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int sugarCane_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Sugar Cane: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int sugarCane_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Sugar Cane: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int sugarCane_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Sugar Cane: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int sugarCane_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Sugar Cane: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int sugarCane_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Sugar Cane: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int sugarCane_sneak = Keyboard.KEY_LSHIFT;

    // Add cactus keybinds
    @Expose
    @ConfigOption(name = "Cactus: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int cactus_attack = -100;

    @Expose
    @ConfigOption(name = "Cactus: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int cactus_useItem = -99;

    @Expose
    @ConfigOption(name = "Cactus: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int cactus_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Cactus: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int cactus_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Cactus: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int cactus_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Cactus: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int cactus_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Cactus: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int cactus_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Cactus: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int cactus_sneak = Keyboard.KEY_LSHIFT;

    // Add mushroom keybinds
    @Expose
    @ConfigOption(name = "Mushroom: Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int mushroom_attack = -100;

    @Expose
    @ConfigOption(name = "Mushroom: Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int mushroom_useItem = -99;

    @Expose
    @ConfigOption(name = "Mushroom: Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int mushroom_left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Mushroom: Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int mushroom_right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Mushroom: Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int mushroom_forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Mushroom: Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int mushroom_back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Mushroom: Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int mushroom_jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Mushroom: Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int mushroom_sneak = Keyboard.KEY_LSHIFT;
}
