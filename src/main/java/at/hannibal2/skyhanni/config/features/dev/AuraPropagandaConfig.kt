package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText

class AuraPropagandaConfig {
    @Expose
    @ConfigEditorBoolean
    @OnlyModern
    @OnlyLegacy
    var enabled = true

    @Expose
    @ConfigEditorText
    @OnlyModern
    @OnlyLegacy
    var targetUUID = "eaa5623c-8413-46b7-a74b-2d74a42b2841"
}
