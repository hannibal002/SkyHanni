package at.hannibal2.skyhanni.config.features.garden.visitor;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    @ConfigOption(name = "Bypass Key", desc = "Hold this key to bypass the Prevent Refusing feature.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LCONTROL)
    public int bypassKey = Keyboard.KEY_LCONTROL;


    @Expose
    @ConfigOption(
        name = "Items",
        desc = "Warn for these reward item visitor drops."
    )
    @ConfigEditorDraggableList
    public List<VisitorReward> drops = new ArrayList<>(Arrays.asList(
        VisitorReward.OVERGROWN_GRASS,
        VisitorReward.GREEN_BANDANA,
        VisitorReward.DEDICATION,
        VisitorReward.MUSIC_RUNE,
        VisitorReward.SPACE_HELMET,
        VisitorReward.CULTIVATING,
        VisitorReward.REPLENISH
    ));

    @Expose
    @ConfigOption(name = "Coins Per Copper", desc = "The price to use for the below options.\nRequires one of the below options to be on.")
    @ConfigEditorSlider(minValue = 1, maxValue = 50_000, minStep = 250)
    public int coinsPerCopperPrice = 1;

    @Expose
    @ConfigOption(name = "Prevent Refusing", desc = "Prevent refusing a visitor with a coins per copper lower than the set value.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean preventRefusingCopper = false;

    @Expose
    @ConfigOption(name = "Prevent Accepting", desc = "Prevent accepting a visitor with a coins per copper higher than the set value.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean preventAcceptingCopper = false;
}
