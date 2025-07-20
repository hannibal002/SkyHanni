package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WarningsFilterConfig {

    @Expose
    @ConfigOption(name = "Ability Cooldown", desc = "Hides ability cooldown messages")
    @ConfigEditorBoolean
    @FeatureToggle
    var abilityCooldown: Boolean = false

    @Expose
    @ConfigOption(name = "Cannot Modify Equipped Pieces", desc = "Hides 'You can not modify your equipped armor set!' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var modifyWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Combat Warning", desc = "Hides 'You can't use this while in combat!' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var combatWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Confirm Cooldown", desc = "Removes 'Wait a moment before confirming' messages")
    @ConfigEditorBoolean
    @FeatureToggle
    var waitWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Fast Command Warning", desc = "Hides 'You are sending commands too fast!' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var fastCommand: Boolean = false

    @Expose
    @ConfigOption(name = "Not Salvageable Warning", desc = "Hides 'This item is not salvageable!' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var salvageableWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Refresh Wait Warning", desc = "Hides 'Please wait a few seconds between refreshing!' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var refreshWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Salvage Warning", desc = "Hides messages about putting a dungeon weapon or armor in anvil to salvage.")
    @ConfigEditorBoolean
    @FeatureToggle
    var salvageWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Slow Down Warning", desc = "Removes 'Whoa! Slow down there!' messages")
    @ConfigEditorBoolean
    @FeatureToggle
    var slowWarning: Boolean = false

}
