package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SkyblockGuideConfig {

    @Expose
    @ConfigOption(name = "Abiphone Highlight",
        desc = "Highlights missing abiphone contacts")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean abiphoneGuide = true;

    @Expose
    @ConfigOption(name = "Minion Highlight",
        desc = "Highlights not maxed minions")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean minionGuide = true;

    @Expose
    @ConfigOption(name = "Essence Shop Highlight",
        desc = "Highlights missing essence shop upgrades")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean essenceGuide = false;

    @Expose
    @ConfigOption(name = "Consumable Highlight",
        desc = "Highlight not fully consumed consumables")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean consumableGuide = true;

    @Expose
    @ConfigOption(name = "Jacob Contest Highlight",
        desc = "Highlight crop where no gold medal was earned")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean jacobGuide = true;

    @Expose
    @ConfigOption(name = "Story Highlight",
        desc = "Highlights not completed story lines, missing fast travel scrolls and not 100% completed harp songs")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean storyGuide = true;

    @Expose
    @ConfigOption(name = "One time Completion Highlights",
        desc = "Highlights these things: " + "\nwhich dragons weren't defeated" + "\nmissing kuudra defeats" + "\nmissing spooky ranks" + "\nmissing bank upgrades" + "\nnot obtained dojo Belts" + "\nmissing rock rarities" + "\nmissing dolphin rarities")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean oneTimeCompletion = true;

}
