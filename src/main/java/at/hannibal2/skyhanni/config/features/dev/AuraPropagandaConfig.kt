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
    var targetUUID = "91548fba-61b4-449f-bc52-8227d463264d"
}
