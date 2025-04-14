package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PestSpawnConfig {
    @Expose
    @ConfigOption(name = "Chat Message Format", desc = "Change how the pest spawn chat message should be formatted.")
    @ConfigEditorDropdown
    var chatMessageFormat: ChatMessageFormatEntry = ChatMessageFormatEntry.HYPIXEL

    enum class ChatMessageFormatEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        HYPIXEL("Hypixel Style", 0),
        COMPACT("Compact", 1),
        DISABLED("Disabled", 2),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Show Title", desc = "Show a Title when a pest spawns.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showTitle: Boolean = true
}
