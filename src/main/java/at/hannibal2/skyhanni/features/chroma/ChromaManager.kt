package at.hannibal2.hanni.features.chroma

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.features.chroma.ChromaConfig
import at.hannibal2.hanni.hannimodule.HanniModule

@HanniModule
object ChromaManager {

    val config get(): ChromaConfig = HanniMod.feature.gui.chroma

    @JvmStatic
    fun resetChromaSettings() {
        with(config) {
            chromaSize = 30f
            chromaSpeed = 6f
            chromaSaturation = 0.75f
            allChroma = false
            ignoreChat = false
            chromaDirection = ChromaConfig.Direction.FORWARD_RIGHT
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(31, "chroma", "gui.chroma")
    }
}
