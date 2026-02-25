package at.hannibal2.skyhanni.config.features.garden

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.glfw.GLFW

class SeeThroughWindowConfig {

    @Expose
    @ConfigOption(
        name = "See Through Farming",
        desc = "Makes the window transparent with a keybind so you can watch YouTube behind the game\n" + "§eDoes not work in full screen"
    )
    @ConfigEditorSlider(minValue = 5f, maxValue = 100f, minStep = 1f)
    val seeThroughFarming: Property<Float> = Property.of(100f)

    @Expose
    @ConfigOption(name = "Keybind", desc = "Press this key to toggle See Through Farming")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var keybind: Int = GLFW.GLFW_KEY_UNKNOWN
}
