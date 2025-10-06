package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SkyblockGuideConfig {
    @Expose
    @ConfigOption(
        name = "Menu Highlight",
        desc = "Highlight the top level of non-completed task in the SkyBlock guide."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var menuGuide: Boolean = true

    @Expose
    @ConfigOption(
        name = "Missing Tasks",
        desc = "Highlight missing tasks in the SkyBlock Level Guide inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var missingTasks: Boolean = true

    @Expose
    @ConfigOption(
        name = "Power Stone Guide",
        desc = "Highlight missing power stones, show their total bazaar price, and allows " +
            " opening the bazaar when clicking on the items in the Power Stone Guide."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var powerStone: Boolean = true

    @Expose
    @ConfigOption(name = "Collection Highlight", desc = "Highlight missing collections.")
    @ConfigEditorBoolean
    @FeatureToggle
    var collectionGuide: Boolean = false

    @Expose
    @ConfigOption(name = "Abiphone Highlight", desc = "Highlight missing Abiphone contacts.")
    @ConfigEditorBoolean
    @FeatureToggle
    var abiphoneGuide: Boolean = true

    @Expose
    @ConfigOption(name = "Minion Highlight", desc = "Highlight non-maxed minions.")
    @ConfigEditorBoolean
    @FeatureToggle
    var minionGuide: Boolean = true

    @Expose
    @ConfigOption(name = "Essence Shop Highlight", desc = "Highlight missing essence shop upgrades.")
    @ConfigEditorBoolean
    @FeatureToggle
    var essenceGuide: Boolean = false

    @Expose
    @ConfigOption(name = "Consumable Highlight", desc = "Highlight non-fully consumed consumables.")
    @ConfigEditorBoolean
    @FeatureToggle
    var consumableGuide: Boolean = true

    @Expose
    @ConfigOption(name = "Jacob Contest Highlight", desc = "Highlight crop where no gold medal was earned.")
    @ConfigEditorBoolean
    @FeatureToggle
    var jacobGuide: Boolean = true

    @Expose
    @ConfigOption(
        name = "Story Highlight",
        desc = "Highlight uncompleted storylines, missing fast travel scrolls and non-100% completed harp songs."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var storyGuide: Boolean = true

    @Expose
    @ConfigOption(
        name = "One Time Completion Highlights",
        desc = "Highlight missing Kuudra defeats, Dungeon floor completions, spooky ranks, " +
            "bank upgrades, rock/dolphin rarities, undefeated dragons, unobtained dojo belts."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var oneTimeCompletion: Boolean = true
}
