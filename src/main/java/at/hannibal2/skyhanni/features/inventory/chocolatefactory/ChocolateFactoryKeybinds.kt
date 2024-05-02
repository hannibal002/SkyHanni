package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

object ChocolateFactoryKeybinds {
    private val config get() = ChocolateFactoryAPI.config.chocolateFactoryKeybindsConfig
    private var lastClick = SimpleTimeMark.farPast()

    private object KeyIterable : Iterable<Int> {

        override fun iterator() = object : Iterator<Int> {
            private var currentIndex = 0

            override fun hasNext() = currentIndex < 5

            override fun next() = getKey(currentIndex++) ?: throw NoSuchElementException("currentIndex: $currentIndex")
        }
    }

    @SubscribeEvent
    fun onKeyPress(event: GuiKeyPressEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.chocolateFactoryKeybinds) return
        if (!ChocolateFactoryAPI.inChocolateFactory) return

        val chest = event.guiContainer as? GuiChest ?: return

        for ((index, key) in KeyIterable.withIndex()) {
            if (!key.isKeyClicked()) continue
            if (lastClick.passedSince() < 200.milliseconds) break

            event.cancel()

            Minecraft.getMinecraft().playerController.windowClick(
                chest.inventorySlots.windowId,
                29 + index,
                2,
                3,
                Minecraft.getMinecraft().thePlayer
            )
            lastClick = SimpleTimeMark.now()
            break
        }
    }

    fun getKey(index: Int) = when (index) {
        0 -> config.key1
        1 -> config.key2
        2 -> config.key3
        3 -> config.key4
        4 -> config.key5
        else -> null
    }
}
