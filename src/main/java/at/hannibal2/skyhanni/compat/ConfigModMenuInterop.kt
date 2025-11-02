package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.config.ConfigGuiManager
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class ConfigModMenuInterop : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory<Screen> {
            MoulConfigScreenComponent(
                Text.empty(), GuiContext(GuiElementComponent(ConfigGuiManager.getEditorInstance())), null,
            )
        }
    }
}
