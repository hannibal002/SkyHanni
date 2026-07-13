package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGHEST
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object InventoryCloseWithNonMousePreventer {
    private val patternGroup = RepoPattern.group("inventory")
    private val closeButtonPatterns by patternGroup.list(
        "closebuttons",
        "Close"
    )
    private val config get() = SkyHanniMod.feature.inventory

    @HandleEvent(priority = HIGHEST)
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (!config.doNotAllowClosingInventoriesUsingNonMouseKeys) return
        if (Minecraft.getInstance().options.keyInventory.isDown || GLFW.GLFW_KEY_ESCAPE.isKeyHeld()) return
        if (!event.isMouseBasedEvent) {
            val underMouseItemStack = stackUnderCursor() ?: return
            ChatUtils.debug(underMouseItemStack.hoverName.string.removeColor())
            if (closeButtonPatterns.matches(underMouseItemStack.hoverName.string.removeColor())) {
                event.cancel()
            }
        }
    }
}
