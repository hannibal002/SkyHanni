package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerListener

// Reference: https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/skyblock/dungeon/LeapOverlay.java
abstract class AbstractCustomMenuScreen(
    initialHandler: ChestMenu,
    title: Component,
) : SkyHanniBaseScreen(title), ContainerListener {

    var handler: ChestMenu = initialHandler
        private set

    init {
        handler.addSlotListener(this)
        ToolTipData.lastSlot = null
    }

    fun changeHandler(newHandler: ChestMenu) {
        handler.removeSlotListener(this)
        handler = newHandler
        handler.addSlotListener(this)
        ToolTipData.lastSlot = null
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (super.keyPressed(input)) {
            return true
        }
        if (Minecraft.getInstance().options.keyInventory.matches(input)) {
            this.onClose()
            return true
        }
        return false
    }

    override fun tick() {
        super.tick()
        val player = MinecraftCompat.localPlayerOrNull ?: return
        if (!player.isAlive) {
            player.closeContainer()
        }
    }

    override fun guiClosed() {
        MinecraftCompat.localPlayerOrNull?.closeContainer()
    }

    override fun removed() {
        val player = MinecraftCompat.localPlayerOrNull ?: return
        handler.removed(player)
        handler.removeSlotListener(this)
    }

    override fun dataChanged(container: AbstractContainerMenu, property: Int, value: Int) = Unit
}
