package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ShTrackConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Allows the use of the shtrack command and it's sub commands.")
    @ConfigEditorBoolean
    var enable: Boolean = true

    @Expose
    @ConfigLink(owner = ShTrackConfig::class, field = "enable")
    var position: Position = Position(20, 20)
}
