package at.hannibal2.skyhanni.config.features.misc.trevortracker;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.ELUSIVE;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.ENDANGERED;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.KILLED;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.PELTS_PER_HOUR;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.QUESTS_STARTED;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.SPACER_1;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.TITLE;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.TOTAL_PELTS;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.TRACKABLE;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.UNDETECTED;
import static at.hannibal2.skyhanni.config.features.misc.trevortracker.TrackerEntry.UNTRACKABLE;

public class TrevorTheTrapperConfig {

    @Expose
    @ConfigOption(
        name = "Enable Data Tracker",
        desc = "Tracks all of your data from doing Trevor Quests.\n" +
            "Shows based on the setting below."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean dataTracker = true;

    @Expose
    @ConfigOption(
        name = "Show Between Quests",
        desc = "Shows the tracker during and between quests otherwise it will only show during them." +
            "Will show in the Trapper's Den regardless. §cToggle 'Enable Data Tracker' above."
    )
    @ConfigEditorBoolean
    public boolean displayType = true;

    @Expose
    @ConfigOption(
        name = "Text Format",
        desc = "Drag text to change the appearance of the overlay."
    )
    @ConfigEditorDraggableList()
    public List<TrackerEntry> textFormat = new ArrayList<>(Arrays.asList(
        TITLE,
        QUESTS_STARTED,
        TOTAL_PELTS,
        PELTS_PER_HOUR,
        SPACER_1,
        KILLED,
        TRACKABLE,
        UNTRACKABLE,
        UNDETECTED,
        ENDANGERED,
        ELUSIVE
    ));

    @Expose
    public Position position = new Position(10, 80, false, true);

    @Expose
    @ConfigOption(name = "Trapper Solver", desc = "Assists you in finding Trevor's mobs. §eNote: May not always work as expected. " +
        "§cWill not help you to find rabbits or sheep in the Oasis!")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean trapperSolver = true;

    @Expose
    @ConfigOption(name = "Mob Dead Warning", desc = "Show a message when Trevor's mob dies.")
    @ConfigEditorBoolean
    public boolean trapperMobDiedMessage = true;

    @Expose
    @ConfigOption(name = "Warp to Trapper", desc = "Warp to Trevor's Den. Works only inside the Farming Islands.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean warpToTrapper = false;

    @Expose
    @ConfigOption(name = "Accept Trapper Quest", desc = "Click this key after the chat prompt to accept Trevor's quest.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean acceptQuest = false;

    @Expose
    @ConfigOption(name = "Trapper Hotkey", desc = "Press this key to warp to Trevor's Den or to accept the quest. " +
        "§eRequires the relevant above settings to be toggled")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int keyBindWarpTrapper = Keyboard.KEY_NONE;


    @Expose
    @ConfigOption(name = "Trapper Cooldown", desc = "Change the color of Trevor and adds a cooldown over his head.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean trapperTalkCooldown = true;

    @Expose
    @ConfigOption(
        name = "Trapper Cooldown GUI",
        desc = "Show the cooldown on screen in an overlay (intended for Abiphone users)."
    )
    @ConfigEditorBoolean
    public boolean trapperCooldownGui = false;

    @Expose
    public Position trapperCooldownPos = new Position(10, 10, false, true);
}
