package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.utils.ConfigUtils
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen

class ConfigModMenuInterop : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory<Screen> { previousScreen ->
            ConfigUtils.createConfigScreen(ConfigGuiManager.getEditorInstance(), previousScreen)
        }
    }
}
