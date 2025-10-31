package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiKeyPressEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryOpenEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object SnakeGame {

    private val pattern by RepoPattern.pattern("abiphone.snake.name", "Snake")
    private val config get() = HanniMod.feature.inventory
    private var lastClick = SimpleTimeMark.farPast()

    private var inInventory = false

    private val keys
        get() = with(Minecraft.getMinecraft().gameSettings) {
            mapOf(
                keyBindLeft.keyCode to 50,
                keyBindForward.keyCode to 51,
                keyBindRight.keyCode to 52,
                keyBindBack.keyCode to 53,
            )
        }

    @HandleEvent
    fun onGui(event: GuiKeyPressEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        val chest = event.guiContainer as? GuiChest ?: return

        if (lastClick.passedSince() < 100.milliseconds) return

        for ((key, slot) in keys) {
            if (!key.isKeyHeld()) continue
            event.cancel()

            InventoryUtils.clickSlot(slot, chest.inventorySlots.windowId, mouseButton = 2, mode = 3)

            lastClick = SimpleTimeMark.now()
            break
        }
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inInventory = pattern.matches(event.inventoryName)
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.snakeGameKeybinds
}
