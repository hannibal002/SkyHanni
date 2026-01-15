package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.ConfigUtils.asProperty
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SeaCreatureHealthDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows a GUI with the health of the sea creatures shown.")
    @ConfigEditorBoolean
    var enabled = false

    @Expose
    @ConfigOption(name = "Health Display Mobs", desc = "The name of the sea creatures to show the health display for, separated by commas.")
    @ConfigEditorText
    val names = "Lord Jawbus, Thunder".asProperty()

    @Expose
    @ConfigOption(name = "Limit", desc = "The maximum amount of mobs to show.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var limit = 5

    @Expose
    @ConfigOption(name = "Red Percentage", desc = "Percentage of health at which a mob's health should be shown in red.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 25f, minStep = 1f)
    var redPercentage: Float = 5f

    @Expose
    @ConfigLink(owner = SeaCreatureHealthDisplayConfig::class, field = "enabled")
    val pos = Position(200, 200, centerX = true)
}
