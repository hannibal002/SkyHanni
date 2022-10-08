package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.config.core.GuiScreenElementWrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.client.IModGuiFactory.RuntimeOptionCategoryElement
import net.minecraftforge.fml.client.IModGuiFactory.RuntimeOptionGuiHandler
import org.lwjgl.input.Keyboard
import java.io.IOException

class ConfigGuiForgeInterop : IModGuiFactory {
    override fun initialize(minecraft: Minecraft) {}
    override fun mainConfigGuiClass(): Class<out GuiScreen> {
        return WrappedSkyHanniConfig::class.java
    }

    override fun runtimeGuiCategories(): Set<RuntimeOptionCategoryElement>? = null

    override fun getHandlerFor(runtimeOptionCategoryElement: RuntimeOptionCategoryElement): RuntimeOptionGuiHandler? =
        null

    class WrappedSkyHanniConfig(private val parent: GuiScreen) : GuiScreenElementWrapper(ConfigEditor.editor) {
        @Throws(IOException::class)
        override fun handleKeyboardInput() {
            if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
                Minecraft.getMinecraft().displayGuiScreen(parent)
                return
            }
            super.handleKeyboardInput()
        }
    }
}