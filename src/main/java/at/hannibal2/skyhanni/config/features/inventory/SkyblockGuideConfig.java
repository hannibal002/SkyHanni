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
    @ConfigOption(name = "Fast Travel Highlight",
        desc = "Highlights missing fast travel scrolls")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean travelGuide = true;

    @Expose
    @ConfigOption(name = "Story Highlight",
        desc = "Highlights not completed story lines")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean storyGuide = true;

    @Expose
    @ConfigOption(name = "Essence Shop Highlight",
        desc = "Highlights missing essence shop upgrades")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean essenceGuide = false;

    @Expose
    @ConfigOption(name = "Harp Highlight",
        desc = "Highlight not 100% completed harp songs")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean harpGuide = true;

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
    @ConfigOption(name = "Dragon Highlight",
        desc = "Highlights which dragons weren't defeated")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean dragonGuide = true;

    @Expose
    @ConfigOption(name = "Slayer Defeat Highlight",
        desc = "Highlights slayer which haven't been defeated")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean slayerDefeatGuide = true;
    @Expose
    @ConfigOption(name = "Kuudra Defeats Highlight",
        desc = "Highlights missing kuudra defeats")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean kuudraGuide = true;

    @Expose
    @ConfigOption(name = "Spooky Highlight",
        desc = "Highlights missing spooky ranks")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean spookyGuide = true;

    @Expose
    @ConfigOption(name = "Bank Highlight",
        desc = "Highlights missing bank upgrades")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean bankGuide = true;

    @Expose
    @ConfigOption(name = "Dojo Belt Highlight",
        desc = "Highlights not obtained dojo Belts")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean beltGuide = true;

    @Expose
    @ConfigOption(name = "Rock Pet Highlight",
        desc = "Highlights missing rock rarities")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean rockPetGuide = true;

    @Expose
    @ConfigOption(name = "Dolphin Pet Highlight",
        desc = "Highlights missing dolphin rarities")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean dolphinGuide = true;

}
