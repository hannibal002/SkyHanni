package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGui
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import io.github.notenoughupdates.moulconfig.gui.GuiElementWrapper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.SignEditScreen
import org.lwjgl.glfw.GLFW

object ChatPeek {

    @JvmStatic
    fun peek(): Boolean {
        val key = SkyHanniMod.feature.chat.peekChat

        if (!MinecraftCompat.localPlayerExists) return false
        if (key <= GLFW.GLFW_KEY_UNKNOWN) return false
        if (MinecraftClient.getInstance().currentScreen is SignEditScreen) return false
        if (MinecraftClient.getInstance().currentScreen is GuiElementWrapper) return false

        if (NeuItems.neuHasFocus()) return false
        if (GuiEditManager.isInGui() || FFGuideGui.isInGui() || VisualWordGui.isInGui()) return false

        return key.isKeyHeld()
    }
}
