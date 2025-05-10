package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.LineEntry.NOTHING;
import static at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry.AFK;
import static at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry.CROP_MILESTONES;
import static at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry.DUNGEONS;
import static at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry.SLAYER;
import static at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry.STACKING_ENCHANT;

public class DiscordRPCConfig {

    @Expose
    @ConfigOption(name = "Enable Discord RPC", desc = "Details about your SkyBlock session displayed through Discord.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "First Line", desc = "Decide what to show in the first line.")
    @ConfigEditorDropdown
    public Property<LineEntry> firstLine = Property.of(NOTHING);

    @Expose
    @ConfigOption(name = "Second Line", desc = "Decide what to show in the second line.")
    @ConfigEditorDropdown
    public Property<LineEntry> secondLine = Property.of(NOTHING);

    @Expose
    @ConfigOption(name = "Custom", desc = "What should be displayed if you select \"Custom\" above.")
    @ConfigEditorText
    public Property<String> customText = Property.of("");

    @Expose
    @ConfigOption(name = "Dynamic Priority", desc = "Disable certain dynamic statuses, or change the priority in case two are triggered at the same time (higher up means higher priority).")
    @ConfigEditorDraggableList
    public List<PriorityEntry> autoPriority = new ArrayList<>(Arrays.asList(
        CROP_MILESTONES,
        SLAYER,
        STACKING_ENCHANT,
        DUNGEONS,
        AFK
    ));

    public enum PriorityEntry {
        CROP_MILESTONES("Crop Milestones"),
        SLAYER("Slayer"),
        STACKING_ENCHANT("Stacking Enchantment"),
        DUNGEONS("Dungeon"),
        AFK("AFK Indicator"),
        ;

        private final String displayName;

        PriorityEntry(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Expose
    @ConfigOption(name = "Dynamic Fallback", desc = "What to show when none of your \"Dynamic Priority\" statuses are active.")
    @ConfigEditorDropdown
    public Property<LineEntry> auto = Property.of(NOTHING);

    @Expose
    @ConfigOption(name = "Show Button for SkyCrypt", desc = "Add a button to the RPC that opens your SkyCrypt profile.")
    @ConfigEditorBoolean
    public Property<Boolean> showSkyCryptButton = Property.of(true);

    @Expose
    @ConfigOption(name = "Show Button for EliteBot", desc = "Add a button to the RPC that opens your EliteBot profile.")
    @ConfigEditorBoolean
    public Property<Boolean> showEliteBotButton = Property.of(true);

    public enum LineEntry {
        NOTHING("Nothing"),
        LOCATION("Location"),
        PURSE("Purse"),
        BITS("Bits"),
        STATS("Stats"),
        HELD_ITEM("Held Item"),
        SKYBLOCK_DATE("SkyBlock Date"),
        PROFILE("Profile"),
        SLAYER("Slayer"),
        CUSTOM("Custom"),
        DYNAMIC("Dynamic"),
        CROP_MILESTONE("Crop Milestone"),
        CURRENT_PET("Current Pet"),
        ;

        private final String displayName;

        LineEntry(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
