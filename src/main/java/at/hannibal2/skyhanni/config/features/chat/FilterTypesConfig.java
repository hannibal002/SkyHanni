package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FilterTypesConfig {

    @Expose
    @ConfigOption(name = "Hypixel Lobbies", desc = "Hide announcements in Hypixel lobbies " +
        "(player joins, loot boxes, prototype lobby messages, radiating generosity, Hypixel tournaments)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hypixelHub = false;

    @Expose
    @ConfigOption(name = "Empty", desc = "Hide all empty messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean empty = false;

    @Expose
    @ConfigOption(name = "Warping", desc = "Hide 'Sending request to join...' and 'Warping...' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean warping = false;

    @Expose
    @ConfigOption(name = "Welcome", desc = "Hide the 'Welcome to SkyBlock' message.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean welcome = false;

    @Expose
    @ConfigOption(name = "Guild EXP", desc = "Hide Guild EXP messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean guildExp = false;

    @Expose
    @ConfigOption(name = "Friend Join/Left", desc = "Hide friend join/left messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean friendJoinLeft = false;

    @Expose
    @ConfigOption(name = "Winter Gifts", desc = "Hide pointless Winter Gift messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean winterGift = false;

    @Expose
    @ConfigOption(name = "Powder Mining", desc = "Hide messages while opening chests in the Crystal Hollows. " +
        "(except powder numbers over 1k, essence numbers over 2, Prehistoric Eggs, and Automaton Parts)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean powderMining = false;

    @Expose
    @ConfigOption(name = "Kill Combo", desc = "Hide messages about your Kill Combo from the Grandma Wolf pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean killCombo = false;

    @Expose
    @ConfigOption(name = "Watchdog", desc = "Hide the message where Hypixel flexes about how many players they have banned over the last week.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean watchDog = false;

    @Expose
    @ConfigOption(name = "Profile Join", desc = "Hide 'You are playing on profile' and 'Profile ID' chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean profileJoin = false;

    @Expose
    @ConfigOption(name = "Fire Sale", desc = "Hide the repeating fire sale reminder chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean fireSale = false;

    @Expose
    @ConfigOption(name = "Event Level Up", desc = "Hide event level up messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean eventLevelUp = false;

    @Expose
    @ConfigOption(name = "Diana", desc = "Hide chat messages around griffin burrow chains, griffin feather drops, and coin drops.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean diana = false;

    @Expose
    @ConfigOption(name = "Factory Upgrade", desc = "Hide §nHypixel's§r Chocolate Factory upgrade and employee promotion messages.\n" +
        "§eTo turn off SkyHanni's upgrade messages, search §lUpgrade Warning")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean factoryUpgrade = false;

    @Expose
    @ConfigOption(name = "Sacrifice", desc = "Hide other players' sacrifice messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean sacrifice = false;


    //TODO remove
    @Expose
    @ConfigOption(name = "Others", desc = "Hide other annoying messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean others = false;
}
