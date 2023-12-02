package at.hannibal2.skyhanni.config.features.garden.visitor;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.garden.visitor.RewardWarningConfig.ItemWarnEntry.CULTIVATING_I;
import static at.hannibal2.skyhanni.config.features.garden.visitor.RewardWarningConfig.ItemWarnEntry.DEDICATION_IV;
import static at.hannibal2.skyhanni.config.features.garden.visitor.RewardWarningConfig.ItemWarnEntry.GREEN_BANDANA;
import static at.hannibal2.skyhanni.config.features.garden.visitor.RewardWarningConfig.ItemWarnEntry.MUSIC_RUNE;
import static at.hannibal2.skyhanni.config.features.garden.visitor.RewardWarningConfig.ItemWarnEntry.OVERGROWN_GRASS;
import static at.hannibal2.skyhanni.config.features.garden.visitor.RewardWarningConfig.ItemWarnEntry.SPACE_HELMET;

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

    public enum ItemWarnEntry implements HasLegacyId {
        FLOWERING_BOUQUET("§9Flowering Bouquet", 0),
        OVERGROWN_GRASS("§9Overgrown Grass", 1),
        GREEN_BANDANA("§9Green Bandana", 2),
        DEDICATION_IV("§9Dedication IV", 3),
        MUSIC_RUNE("§9Music Rune", 4),
        SPACE_HELMET("§cSpace Helmet", 5),
        CULTIVATING_I("§9Cultivating I", 6),
        REPLENISH_I("§9Replenish I", 7),
        ;

        private final String str;
        private final int legacyId;

        ItemWarnEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        ItemWarnEntry(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
