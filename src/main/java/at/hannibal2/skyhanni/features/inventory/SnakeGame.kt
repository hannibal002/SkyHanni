package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.milliseconds

class SnakeGame {

    private val pattern by RepoPattern.pattern("snake.name", "Snake")
    private val config get() = SkyHanniMod.feature.inventory
    private var lastClick = SimpleTimeMark.farPast()

    private val keys = mapOf(
        Keyboard.KEY_A to 50,
        Keyboard.KEY_W to 51,
        Keyboard.KEY_D to 52,
        Keyboard.KEY_S to 53,
    )

    @SubscribeEvent
    fun onGui(event: GuiKeyPressEvent) {
        if (!isEnabled()) return
        if (!pattern.matches(InventoryUtils.openInventoryName())) return

        val chest = event.guiContainer as? GuiChest ?: return

        if (lastClick.passedSince() < 100.milliseconds) return

        for ((key, slot) in keys) {
            if (key.isKeyHeld()) {
                event.cancel()

                Minecraft.getMinecraft().playerController.windowClick(
                    chest.inventorySlots.windowId,
                    slot,
                    2,
                    3,
                    Minecraft.getMinecraft().thePlayer
                )

                lastClick = SimpleTimeMark.now()
                break
            }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.snakeGameKeybinds
}
