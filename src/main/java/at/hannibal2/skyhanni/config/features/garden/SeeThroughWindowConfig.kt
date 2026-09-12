package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.utils.KeyboardManager
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class SeeThroughWindowConfig {

    @Expose
    @ConfigOption(
        name = "See Through Farming",
        desc = "Makes the window transparent (specified %) with a keybind so you can watch YouTube behind the game\n" +
            "§eDoes not work in full screen"
    )
    @ConfigEditorSlider(minValue = 5f, maxValue = 100f, minStep = 1f)
    val seeThroughFarming: Property<Float> = Property.of(100f)

    @Expose
    @ConfigOption(name = "Keybind", desc = "Press this key to toggle See Through Farming")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var keybind: Int = KeyboardManager.KEY_UNKNOWN
}
