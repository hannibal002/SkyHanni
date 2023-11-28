package at.hannibal2.skyhanni.config.features.misc.discordrpc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.misc.discordrpc.LineEntry.NOTHING;
import static at.hannibal2.skyhanni.config.features.misc.discordrpc.PriorityEntry.AFK;
import static at.hannibal2.skyhanni.config.features.misc.discordrpc.PriorityEntry.CROP_MILESTONES;
import static at.hannibal2.skyhanni.config.features.misc.discordrpc.PriorityEntry.DUNGEONS;
import static at.hannibal2.skyhanni.config.features.misc.discordrpc.PriorityEntry.SLAYER;
import static at.hannibal2.skyhanni.config.features.misc.discordrpc.PriorityEntry.STACKING_ENCHANT;

public class DiscordRPCConfig {

    @Expose
    @ConfigOption(name = "Enable Discord RPC", desc = "Details about your SkyBlock session displayed through Discord.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "First Line", desc = "Decide what to show in the first line.")
    @ConfigEditorDropdown()
    public Property<LineEntry> firstLine = Property.of(NOTHING);

    @Expose
    @ConfigOption(name = "Second Line", desc = "Decide what to show in the second line.")
    @ConfigEditorDropdown()
    public Property<LineEntry> secondLine = Property.of(NOTHING);

    @Expose
    @ConfigOption(name = "Custom", desc = "What should be displayed if you select \"Custom\" above.")
    @ConfigEditorText
    public Property<String> customText = Property.of("");

    @Expose
    @ConfigOption(name = "Dynamic Priority", desc = "Disable certain dynamic statuses, or change the priority in case two are triggered at the same time (higher up means higher priority).")
    @ConfigEditorDraggableList()
    public List<PriorityEntry> autoPriority = new ArrayList<>(Arrays.asList(
        CROP_MILESTONES,
        SLAYER,
        STACKING_ENCHANT,
        DUNGEONS,
        AFK
    ));

    @Expose
    @ConfigOption(name = "Dynamic Fallback", desc = "What to show when none of your \"Dynamic Priority\" statuses are active.")
    @ConfigEditorDropdown()
    public Property<LineEntry> auto = Property.of(NOTHING);

}
