package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerListener

// Reference: https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/skyblock/dungeon/LeapOverlay.java
/**
 * An abstract class that replaces a skyblock menu screen with a custom one.
 * Look at [at.hannibal2.skyhanni.mixins.hooks.MenuScreensHook] for when to use your custom menu screen.
 */
abstract class AbstractCustomMenuScreen(
    initialMenu: ChestMenu,
    title: Component,
) : SkyHanniBaseScreen(title), ContainerListener, MenuAccess<ChestMenu> {

    private var menu: ChestMenu = initialMenu

    init {
        menu.addSlotListener(this)
        ToolTipData.lastSlot = null
    }

    fun changeHandler(newMenu: ChestMenu) {
        menu.removeSlotListener(this)
        menu = newMenu
        menu.addSlotListener(this)
        ToolTipData.lastSlot = null
    }

    override fun getMenu(): ChestMenu {
        return menu
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (Minecraft.getInstance().options.keyInventory.matches(input)) {
            this.onClose()
            return true
        }
        return super.keyPressed(input)
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
        menu.removed(player)
        menu.removeSlotListener(this)
    }

    override fun dataChanged(container: AbstractContainerMenu, property: Int, value: Int) = Unit
}
