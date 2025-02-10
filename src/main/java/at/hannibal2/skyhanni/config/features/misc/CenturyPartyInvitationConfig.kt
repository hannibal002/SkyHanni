package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CenturyPartyInvitationConfig {

    @ConfigOption(
        name = "Player Highlighter",
        desc = "Find players that can be invited to your Century Party.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var playerHighlighter: Boolean = true

    @Expose
    @ConfigOption(name = "Can Invite", desc = "Color for players you can invite.")
    @ConfigEditorColour
    var canColor: Property<String> = Property.of("0:1:85:255:85")

    @Expose
    @ConfigOption(name = "No invitation", desc = "Color for players you can't invite.")
    @ConfigEditorColour
    var canNotColor: Property<String> = Property.of("0:1:76:76:76")
}
