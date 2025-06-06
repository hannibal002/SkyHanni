package at.hannibal2.skyhanni.config.features.foraging

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category

class ForagingConfig {
    @Expose
    @Category(name = "HotF", desc = "Settings for Heart of the Forest.")
    var hotf: HotfConfig = HotfConfig()
}
