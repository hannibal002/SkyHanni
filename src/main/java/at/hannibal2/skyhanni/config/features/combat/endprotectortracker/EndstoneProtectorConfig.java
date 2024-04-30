package at.hannibal2.skyhanni.config.features.combat.endprotectortracker;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.combat.endprotectortracker.EndstoneProtector;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.combat.endprotectortracker.EndstoneProtectorConfig.ProtectorDisplayEntry.*;

public class EndstoneProtectorConfig {

    @Expose
    @ConfigOption(name = "Stage Display", desc = "Enable the Endstone Protector stage display.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabledGUI = true;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Endstone Protector counter.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Countdown", desc = "Enable Countdown Display under Stage Display when Protector about to spawn.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabledCountdown = true;

    @Expose
    @ConfigOption(
            name = "Display Text",
            desc = "Drag text to change the appearance of the overlay."
    )
    @ConfigEditorDraggableList
    public Property<List<ProtectorDisplayEntry>> textFormat = Property.of(new ArrayList<>(Arrays.asList(
            TITLE,
            ENDSTONE_PROTECTORS_KILLED,
            TIER_BOOST,
            PROTECTOR_SINCE_TIER_BOOST,
            LEG_GOLEM,
            PROTECTOR_SINCE_LEG_GOLEM,
            EPIC_GOLEM,
            PROTECTOR_SINCE_EPIC_GOLEM,
            CRYSTAL_FRAGMENTS,
            ENCHANTED_ENDSTONE,
            ROSE
    )));

    public enum ProtectorDisplayEntry {
        TITLE("§6§lEndstone Protector Tracker"),
        ENDSTONE_PROTECTORS_KILLED(" §eTotal Killed§f: §b42"),
        TIER_BOOST(" §6Tier Boost§f: §b2"),
        PROTECTOR_SINCE_TIER_BOOST(" §6Protectors since Tier Boost§f: §b1"),
        LEG_GOLEM(" §6Legendary Golem Pets§f: §b3"),
        PROTECTOR_SINCE_LEG_GOLEM(" §6Protectors since Legendary Golem Pet§f: §b1"),
        EPIC_GOLEM(" §5Epic Golem Pets§f: §b1"),
        PROTECTOR_SINCE_EPIC_GOLEM(" §5Kills Since Epic Golem§f: §b2"),
        CRYSTAL_FRAGMENTS(" §5Crystal Fragments§f: §b0"),
        ENCHANTED_ENDSTONE(" §aEnchanted Endstone§f: §b20"),
        ROSE(" §9Roses§f: §b42"),
        ;

        private final String str;


        // Constructor if new enum elements are added post-migration
        ProtectorDisplayEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @ConfigOption(name = "Reset", desc = "Reset the counter.")
    @ConfigEditorButton(buttonText = "Reset")
    public Runnable resetCounter = EndstoneProtector.INSTANCE::resetTracker;

    @Expose
    @ConfigOption(name = "Show only in Dragon's Nest", desc = "Show the overlay only when you are in The Dragon's Nest.")
    @ConfigEditorBoolean
    public boolean onlyOnNest = true;

    @Expose
    @ConfigOption(name = "Alert when Stage 5", desc = "Shows a title when stage 5.")
    @ConfigEditorBoolean
    public boolean stage5Alert = true;

    @Expose
    @ConfigOption(name = "Alert when Stage 4", desc = "Shows a title when stage 4.")
    @ConfigEditorBoolean
    public boolean stage4Alert = true;

    @Expose
    @ConfigOption(name = "Alert when Rare Drop", desc = "Shows a title when dropped Golem or Tier Boost.")
    @ConfigEditorBoolean
    public boolean dropAlert = true;

    @Expose
    @ConfigOption(name = "Show Location", desc = "Shows the location of Endstone Protector.")
    @ConfigEditorBoolean
    public boolean showLocation = true;

    @Expose
    @ConfigOption(name = "Shows only if Stage 4/5", desc = "Shows Location only if Stage is 4 or 5")
    @ConfigEditorBoolean
    public boolean showOnlyIfStage4or5 = true;

    @Expose
    @ConfigOption(name = "Show Fight Time", desc = "Shows the time it took to kill Endstone Protector in chat.")
    @ConfigEditorBoolean
    public boolean showFightTime = false;

    @Expose
    @ConfigOption(name = "Show DPS", desc = "Shows the damage per second to Endstone Protector in chat.")
    @ConfigEditorBoolean
    public boolean showFightDPS = false;

    @Expose
    @ConfigLink(owner = EndstoneProtectorConfig.class, field = "enabled")
    public Position position = new Position(50, 50, false, true);

    @Expose
    @ConfigLink(owner = EndstoneProtectorConfig.class, field = "enabledGUI")
    public Position guiposition = new Position(50, 200, false, true);
}
