package at.hannibal2.skyhanni.config

import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.client.IModGuiFactory.RuntimeOptionCategoryElement
import net.minecraftforge.fml.client.IModGuiFactory.RuntimeOptionGuiHandler
import org.lwjgl.input.Keyboard
import java.io.IOException

@Suppress("unused")
class ConfigGuiForgeInterop : IModGuiFactory {
    override fun initialize(minecraft: Minecraft) {}
    override fun mainConfigGuiClass() = WrappedSkyHanniConfig::class.java

    override fun runtimeGuiCategories(): Set<RuntimeOptionCategoryElement>? = null

    override fun getHandlerFor(element: RuntimeOptionCategoryElement): RuntimeOptionGuiHandler? = null

    class WrappedSkyHanniConfig(private val parent: GuiScreen) : GuiScreenElementWrapper(ConfigGuiManager.editor) {
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