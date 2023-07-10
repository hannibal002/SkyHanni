package at.hannibal2.skyhanni.config.features;

import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CommandsFeatures {

    @ConfigOption(name = "Fandom Wiki", desc = "Use Fandom wiki (§ehypixel-skyblock.fandom.com§7) instead of the Hypixel wiki (§ewiki.hypixel.net§7).")
    @ConfigEditorBoolean
    public boolean useFandomWiki = false;

    @ConfigOption(name = "Party transfer", desc = "Allows §e/pt <player> §7as alias for §e/party transfer§7.\n" +
            "§7SkyBlock command §e/pt §7to check the play time still works.")
    @ConfigEditorBoolean
    public boolean usePartyTransferAlias = true;
}
