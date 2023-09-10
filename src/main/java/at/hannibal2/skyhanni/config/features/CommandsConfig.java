package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CommandsConfig {

    @Expose
    @ConfigOption(name = "Fandom Wiki", desc = "Use Fandom wiki (§ehypixel-skyblock.fandom.com§7) instead of the Hypixel wiki (§ewiki.hypixel.net§7).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean useFandomWiki = false;

    @Expose
    @ConfigOption(name = "Party transfer", desc = "Allows §e/pt <player> §7as alias for §e/party transfer§7.\n" +
            "§7SkyBlock command §e/pt §7to check the play time still works.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean usePartyTransferAlias = true;

    @Expose
    @ConfigOption(name = "Replace Warp Is", desc = "Adds §e/warp is §7alongside §e/is§7. Idk why. Ask §cKaeso")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean replaceWarpIs = false;
}
