package at.hannibal2.skyhanni.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import io.github.notenoughupdates.moulconfig.gui.GuiElementWrapper
import net.minecraft.client.gui.screen.Screen

class ConfigModMenuInterop : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory<Screen> { GuiElementWrapper(ConfigGuiManager.getEditorInstance()) }
    }
}
