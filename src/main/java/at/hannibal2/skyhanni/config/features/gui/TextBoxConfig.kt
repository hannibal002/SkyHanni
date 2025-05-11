package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class TextBoxConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable showing the textbox while in SkyBlock.")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Only in GUI", desc = "Only show the textbox while an inventory is open.")
    @ConfigEditorBoolean
    var onlyInGui: Boolean = false

    @Expose
    @ConfigOption(
        name = "Text",
        desc = "Enter text you want to display here.\n" +
            "§eUse '&' as the color code character.\n" +
            "§eUse '\\n' as the line break character."
    )
    @ConfigEditorText
    var text: Property<String> = Property.of("&aYour Text Here\\n&bYour new line here")

    @Expose
    @ConfigLink(owner = TextBoxConfig::class, field = "enabled")
    var position: Position = Position(10, 80)
}
