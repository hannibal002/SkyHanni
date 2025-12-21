package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PestSpawnConfig {
    @Expose
    @ConfigOption(name = "Chat Message Format", desc = "Change how the pest spawn chat message should be formatted.")
    @ConfigEditorDropdown
    var chatMessageFormat: ChatMessageFormatEntry = ChatMessageFormatEntry.HYPIXEL

    enum class ChatMessageFormatEntry(private val displayName: String) {
        HYPIXEL("Hypixel Style"),
        COMPACT("Compact"),
        DISABLED("Disabled"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Show Title", desc = "Show a Title when a pest spawns.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showTitle: Boolean = true

    enum class PestSpawnSoundMode(private val displayName: String) {
        DEFAULT("Default"),
        MUTED("Muted"),
        CUSTOM("Custom"),
        PLUMBER("Plumber"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Spawn Sound", desc = "Mute or replace the default spawn sound with a custom one.")
    @ConfigEditorDropdown
    var soundMode: PestSpawnSoundMode = PestSpawnSoundMode.DEFAULT

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    val sound: PestSpawnSoundSettings = PestSpawnSoundSettings()
}
