package at.hannibal2.skyhanni.config.features.commands;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CommandsConfig {

    @ConfigOption(name = "Tab Complete", desc = "")
    @Accordion
    @Expose
    public TabCompleteConfig tabComplete = new TabCompleteConfig();

    @ConfigOption(name = "Better §e/wiki", desc = "")
    @Accordion
    @Expose
    public BetterWikiCommandConfig betterWiki = new BetterWikiCommandConfig();

    @ConfigOption(name = "Party Commands", desc = "Shortens party commands and allows tab-completing for them. " +
        "\n§eCommands: /pt /pp /pko /pk /pd §7SkyBlock command §e/pt §7to check the play time still works.")
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean shortCommands = true;

    @ConfigOption(name = "Party Kick Reason", desc = "Kick people while sending a reason when using §e/pk lrg89 Dupe Archer§7 or §e/party kick nea89o Low Cata Level§7.")
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean partyKickReason = true;

    @Expose
    @ConfigOption(name = "Add Warp Is", desc = "Adds §e/warp is §7alongside §e/is§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean replaceWarpIs = false;

    @Expose
    @ConfigOption(name = "/viewrecipe Lower Case", desc = "Adds support for lower case item IDs to the Hypixel command §e/viewrecipe§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean viewRecipeLowerCase = true;
}
