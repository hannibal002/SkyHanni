package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CreationCooldownConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the time until another dungeon instance can be created.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Show Outside of Dungeons", desc = "Show on other skyblock islands.")
    @ConfigEditorBoolean
    var showOutside: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only Show In Entrance Room",
        desc = "Only show the display when in the entrance room of a dungeon.",
    )
    @ConfigEditorBoolean
    var entranceOnly: Boolean = true

    @Expose
    @ConfigOption(name = "Send Chat Message", desc = "Send a chat message when creation cooldown is over.")
    @ConfigEditorBoolean
    var sendChatMessage: Boolean = false

    @Expose
    @ConfigOption(
        name = "Block Command Send",
        desc = "Block sending the /joininstance command during creation cooldown.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var blockInstanceCreation: Boolean = false

    @Expose
    @ConfigLink(owner = CreationCooldownConfig::class, field = "enabled")
    val position: Position = Position(383, 93)
}
