package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.OnlyModern
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category

/**
 * Attention developers:
 * If your feature can only be used on the foraging islands please mark it with @[OnlyModern]
 */
class ForagingConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "To see all foraging features please launch the game on a modern version of Minecraft with SkyHanni installed."
    )
    @OnlyLegacy
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @Category(name = "HotF", desc = "Settings for Heart of the Forest.")
    var hotf: HotfConfig = HotfConfig()
}
