package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class RecordAnimationsConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the /shskull recording command.")
    @ConfigEditorBoolean
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Debug Overlay", desc = "Show per-animation debug overlays while recording.")
    @ConfigEditorBoolean
    val debugOverlay: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = RecordAnimationsConfig::class, field = "debugOverlay")
    val debugPosition: Position = Position(20, 20)
}
