package at.hannibal2.skyhanni.config.features.inventory

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW


class ShowMaxEnchants {
    @Expose
    @ConfigOption(
        name = "Show max enchant level",
        desc = "Shows the maximum enchant level for an enchant beside the enchantment level. ",
    )
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only show on keybind",
        desc = "Only shows the maximum enchant level when you hold the keybind.\n" +
            "Unbind to show persistently.",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_LEFT_SHIFT)
    var keybind: Int = GLFW.GLFW_KEY_LEFT_SHIFT

    @Expose
    @ConfigOption(name = "Use good enchant level", desc = "Instead of max enchant level, use good enchant level")
    @ConfigEditorBoolean
    var useGoodEnchantLevel: Boolean = false

}
