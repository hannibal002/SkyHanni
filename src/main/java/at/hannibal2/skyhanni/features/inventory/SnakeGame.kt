package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object SnakeGame {

    /**
     * REGEX-TEST: Snake
     */
    private val pattern by RepoPattern.pattern(
        "abiphone.snake.name",
        "Snake|Eat 8 apples to win!"
    )
    private val config get() = SkyHanniMod.feature.inventory
    private var lastClick = SimpleTimeMark.farPast()

    private var inInventory = false

    private val keys
        get() = with(Minecraft.getInstance().options) {
            mapOf(
                keyLeft.key.value to 24,
                keyUp.key.value to 16,
                keyRight.key.value to 26,
                keyDown.key.value to 34,
            )
        }

    @HandleEvent
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        val chest = event.guiContainer as? ContainerScreen ?: return

        if (lastClick.passedSince() < 100.milliseconds) return

        for ((key, slot) in keys) {
            if (!key.isKeyHeld()) continue
            event.cancel()

            chest.clickSlot(slot, MIDDLE_CLICK)

            lastClick = SimpleTimeMark.now()
            break
        }
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inInventory = pattern.matches(event.inventoryName)
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        inInventory = false
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.snakeGameKeybinds
}
