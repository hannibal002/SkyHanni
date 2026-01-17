package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CustomTodosConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Enables Custom Todos. Use §e/shtodos §rto edit them."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Separate Guis",
        desc = "Turns the Todos into separate Guis, instead of having one big Gui."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var separateGuis: Boolean = true

    @Expose
    @ConfigLink(owner = CustomTodosConfig::class, field = "separateGuis")
    val position: Position = Position(150, 200)
}
