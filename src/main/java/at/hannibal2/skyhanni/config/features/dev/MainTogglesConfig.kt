package at.hannibal2.skyhanni.config.features.dev

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MainTogglesConfig {

    @ConfigOption(
        name = "§cNote",
        desc = "§cOnly disable when you know what you are doing!",
    )
    @ConfigEditorInfoText
    var note: String = ""

    @Expose
    @ConfigOption(
        name = "Damage Indicator",
        desc = "Enable the backend of the Damage Indicator.",
    )
    @ConfigEditorBoolean
    var damageIndicator: Boolean = true

    @Expose
    @ConfigOption(name = "Mob Detection Enable", desc = "Turn off and on again to reset all mobs.")
    @ConfigEditorBoolean
    var mobDetection: Boolean = true

    @Expose
    @ConfigOption(
        name = "Ping API",
        desc = "Make the client always send ping packets to the server as if the debug HUD was open so that we can calculate your ping.",
    )
    @ConfigEditorBoolean
    var pingApi: Boolean = true
}
