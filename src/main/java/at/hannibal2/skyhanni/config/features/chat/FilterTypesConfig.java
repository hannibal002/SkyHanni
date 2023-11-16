package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class FilterTypesConfig {

    @Expose
    @ConfigOption(name = "Hypixel Hub", desc = "Block messages outside SkyBlock in the Hypixel lobby: player joins, loot boxes, prototype lobby messages, radiating generosity and Hypixel tournaments.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hypixelHub = true;

    @Expose
    @ConfigOption(name = "Empty", desc = "Hide all the empty messages from the chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean empty = true;

    @Expose
    @ConfigOption(name = "Warping", desc = "Block 'Sending request to join...' and 'Warping...' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean warping = true;

    @Expose
    @ConfigOption(name = "Welcome", desc = "Hide the 'Welcome to SkyBlock' message.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean welcome = true;

    @Expose
    @ConfigOption(name = "Guild Exp", desc = "Hide Guild EXP messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean guildExp = true;

    @Expose
    @ConfigOption(name = "Friend Join Left", desc = "Hide friend join/left messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean friendJoinLeft = false;

    @Expose
    @ConfigOption(name = "Winter Gifts", desc = "Hide useless Winter Gift messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean winterGift = false;

    @Expose
    @ConfigOption(name = "Powder Mining", desc = "Hide messages while opening chests in the Crystal Hollows. " +
        "(Except powder numbers over 1k, essence numbers over 2, Prehistoric Eggs, and Automaton Parts)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean powderMining = true;

    @Expose
    @ConfigOption(name = "Kill Combo", desc = "Hide messages about the current Kill Combo from the Grandma Wolf Pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean killCombo = false;

    @Expose
    @ConfigOption(name = "Watchdog", desc = "Hide the message where Hypixel is flexing how many players they have banned over the last week.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean watchDog = true;

    @Expose
    @ConfigOption(name = "Profile Join", desc = "Hide 'You are playing on profile' and 'Profile ID' chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean profileJoin = true;

    //TODO remove
    @Expose
    @ConfigOption(name = "Others", desc = "Hide other annoying messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean others = false;
}
