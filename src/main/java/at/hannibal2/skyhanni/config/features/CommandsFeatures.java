package at.hannibal2.skyhanni.config.features;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CommandsFeatures {

    @Expose
    @ConfigOption(name = "Fandom Wiki", desc = "Use Fandom wiki (§ehypixel-skyblock.fandom.com§7) instead of the Hypixel wiki (§ewiki.hypixel.net§7).")
    @ConfigEditorBoolean
    public boolean useFandomWiki = false;
}
