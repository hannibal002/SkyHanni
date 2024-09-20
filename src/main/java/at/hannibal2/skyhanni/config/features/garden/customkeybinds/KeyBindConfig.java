package at.hannibal2.skyhanni.config.features.garden.customkeybinds;

import at.hannibal2.skyhanni.config.FeatureToggle;
// import at.hannibal2.skyhanni.config.features.garden.customkeybinds.KeyBindConfigCrops;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;


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
    public Runnable presetDisable = this::disableKeybinds;

    @ConfigOption(name = "Set Default", desc = "Reset all keys to default.")
    @ConfigEditorButton(buttonText = "Default")
    public Runnable presetDefault = this::resetKeybinds;

    @Expose
    @ConfigOption(name = "Custom Keybinds", desc = "Set custom keybinds for each crop.")
    @Accordion
    public KeyBindConfigCrops keyBind = new KeyBindConfigCrops();

    private void resetKeybinds() {
        // Reset all keybinds to default
        Class<?> clazz = keyBind.getClass();

        // Get all public fields of the class
        Field[] fields = clazz.getFields();

        // Iterate through each field
        for (Field field : fields) {
            if (field.getType() == KeyBindCrop.class) {
                try {
                    KeyBindCrop CropClassInstance = (KeyBindCrop) field.get(keyBind);

                    CropClassInstance.attack = -100;
                    CropClassInstance.useItem = -99;
                    CropClassInstance.left = Keyboard.KEY_A;
                    CropClassInstance.right = Keyboard.KEY_D;
                    CropClassInstance.forward = Keyboard.KEY_W;
                    CropClassInstance.back = Keyboard.KEY_S;
                } catch (IllegalAccessException e) {
                    // pass
                }
            }
        }

    }

    private void disableKeybinds() {
        // Reset all keybinds to default
        Class<?> clazz = keyBind.getClass();

        // Get all public fields of the class
        Field[] fields = clazz.getFields();

        // Iterate through each field
        for (Field field : fields) {
            if (field.getType() == KeyBindCrop.class) {
                try {
                    KeyBindCrop CropClassInstance = (KeyBindCrop) field.get(keyBind);

                    CropClassInstance.attack = Keyboard.KEY_NONE;
                    CropClassInstance.useItem = Keyboard.KEY_NONE;
                    CropClassInstance.left = Keyboard.KEY_NONE;
                    CropClassInstance.right = Keyboard.KEY_NONE;
                    CropClassInstance.forward = Keyboard.KEY_NONE;
                    CropClassInstance.back = Keyboard.KEY_NONE;
                } catch (IllegalAccessException e) {
                    // pass
                }
            }
        }

    }


}

