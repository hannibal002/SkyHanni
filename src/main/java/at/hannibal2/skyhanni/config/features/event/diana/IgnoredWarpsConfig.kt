package at.hannibal2.skyhanni.config.features.event.diana

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class IgnoredWarpsConfig {
    @Expose
    @ConfigOption(name = "Crypt", desc = "Ignore the Crypt warp point (because it takes a long time to leave).")
    @ConfigEditorBoolean
    var crypt: Boolean = false

    @Expose
    @ConfigOption(
        name = "Wizard",
        desc = "Ignore the Wizard Tower warp point (because it is easy to fall into the Rift portal)."
    )
    @ConfigEditorBoolean
    var wizard: Boolean = false

    @Expose
    @ConfigOption(name = "Stonks", desc = "Ignore the Stonks warp point (because it is new).")
    @ConfigEditorBoolean
    var stonks: Boolean = false
}
