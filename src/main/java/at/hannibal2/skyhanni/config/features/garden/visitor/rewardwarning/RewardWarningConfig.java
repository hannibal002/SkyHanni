package at.hannibal2.skyhanni.config.features.garden.visitor.rewardwarning;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.garden.visitor.rewardwarning.ItemWarnEntry.CULTIVATING_I;
import static at.hannibal2.skyhanni.config.features.garden.visitor.rewardwarning.ItemWarnEntry.DEDICATION_IV;
import static at.hannibal2.skyhanni.config.features.garden.visitor.rewardwarning.ItemWarnEntry.GREEN_BANDANA;
import static at.hannibal2.skyhanni.config.features.garden.visitor.rewardwarning.ItemWarnEntry.MUSIC_RUNE;
import static at.hannibal2.skyhanni.config.features.garden.visitor.rewardwarning.ItemWarnEntry.OVERGROWN_GRASS;
import static at.hannibal2.skyhanni.config.features.garden.visitor.rewardwarning.ItemWarnEntry.SPACE_HELMET;

public class RewardWarningConfig {

    @Expose
    @ConfigOption(name = "Notify in Chat", desc = "Send a chat message once you talk to a visitor with reward.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean notifyInChat = true;

    @Expose
    @ConfigOption(name = "Show over Name", desc = "Show the reward name above the visitor name.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showOverName = true;

    @Expose
    @ConfigOption(name = "Prevent Refusing", desc = "Prevent the refusal of a visitor with reward.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean preventRefusing = true;

    @Expose
    @ConfigOption(name = "Bypass Key", desc = "Hold that key to bypass the Prevent Refusing feature.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int bypassKey = Keyboard.KEY_NONE;


    /**
     * Sync up with {at.hannibal2.skyhanni.features.garden.visitor.VisitorReward}
     */
    @Expose
    @ConfigOption(
        name = "Items",
        desc = "Warn for these reward items."
    )
    @ConfigEditorDraggableList(
        exampleText = {
            "§9Flowering Bouquet",
            "§9Overgrown Grass",
            "§9Green Bandana",
            "§9Dedication IV",
            "§9Music Rune",
            "§cSpace Helmet",
            "§9Cultivating I",
            "§9Replenish I",
        }
    )
    public List<ItemWarnEntry> drops = new ArrayList<>(Arrays.asList(
        OVERGROWN_GRASS,
        GREEN_BANDANA,
        DEDICATION_IV,
        MUSIC_RUNE,
        SPACE_HELMET,
        CULTIVATING_I
    ));

}
