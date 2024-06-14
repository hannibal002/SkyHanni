package at.hannibal2.skyhanni.config.features.inventory.chocolatefactory;

import at.hannibal2.skyhanni.utils.OSUtils;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class ChocolateFactoryRabbitWarningConfig {

    @Expose
    @ConfigOption(name = "Rabbit Warning", desc = "Warn when the rabbit that needs to be clicked appears.")
    @ConfigEditorBoolean
    public boolean rabbitWarning = true;

    @Expose
    @ConfigOption(name = "Special Rabbit Warning", desc = "Warn when a special rabbit that needs to be clicked appears. (Rabbit The Fish and Golden Rabbits.")
    @ConfigEditorBoolean
    public boolean specialRabbitWarning = true;

    @Expose
    @ConfigOption(name = "Warning Sound", desc = "The sound that play for a special rabbit.\n" +
        "§eYou can use custom sounds, put it in the skyhanni/sounds folder in your resource pack.\n" +
        "§eThen write skyhanni:yourfilename\n" +
        "§cMust be a .ogg file")
    @ConfigEditorText
    public Property<String> specialRabbitSound = Property.of("note.pling");

    @Expose
    @ConfigOption(name = "Repeat Sound", desc = "How many times the sound should be repeated.")
    @ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1)
    public int repeatSound = 20;

    @Expose
    @ConfigOption(name = "Flash Screen", desc = "Flash the screen in the given color when a special rabbit appear.")
    @ConfigEditorBoolean
    public boolean flashScreen = false;

    @Expose
    @ConfigOption(name = "Flash Screen", desc = "Choose the stray rabbit type to flash the screen for.")
    @ConfigEditorDropdown
    public FlashScreenTypeEntry flashScreenType = FlashScreenTypeEntry.SPECIAL;

    public enum FlashScreenTypeEntry {
        REGULAR("Regular"),
        SPECIAL("Special"),
        BOTH("All"),
        ;
        private final String str;

        FlashScreenTypeEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Flash Color", desc = "Color of the screen when flashing")
    @ConfigEditorColour
    public String flashColor = "0:127:0:238:255";

    @ConfigOption(name = "Sounds", desc = "Click to open the list of available sounds.")
    @ConfigEditorButton(buttonText = "OPEN")
    public Runnable sounds = () -> OSUtils.openBrowser("https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/mapping-and-modding-tutorials/2213619-1-8-all-playsound-sound-arguments");

}
