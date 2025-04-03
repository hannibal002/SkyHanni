package at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.danceroomhelper.danceroomformatting

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ColorConfig {
    @Expose
    @ConfigOption(name = "Move", desc = "Color for the Move instruction")
    @ConfigEditorText
    var move: String = "&e"

    @Expose
    @ConfigOption(name = "Stand", desc = "Color for the Stand instruction")
    @ConfigEditorText
    var stand: String = "&e"

    @Expose
    @ConfigOption(name = "Sneak", desc = "Color for the Sneak instruction")
    @ConfigEditorText
    var sneak: String = "&5"

    @Expose
    @ConfigOption(name = "Jump", desc = "Color for the Jump instruction")
    @ConfigEditorText
    var jump: String = "&b"

    @Expose
    @ConfigOption(name = "Punch", desc = "Color for the Punch instruction")
    @ConfigEditorText
    var punch: String = "&d"

    @Expose
    @ConfigOption(name = "Countdown", desc = "Color for the Countdown")
    @ConfigEditorText
    var countdown: String = "&f"

    @Expose
    @ConfigOption(name = "Default", desc = "Fallback color")
    @ConfigEditorText
    var fallback: String = "&f"
}
