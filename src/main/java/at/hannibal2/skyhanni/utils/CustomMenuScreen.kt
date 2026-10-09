package at.hannibal2.skyhanni.utils

import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu

class CustomMenuScreen(
    menu: ChestMenu,
    title: Component,
    val gui: AbstractCustomMenu,
) : AbstractCustomMenuScreen(menu, title) {
    private var updateScheduled = false

    override fun getRectangle(): ScreenRectangle = gui.rectangle ?: super.getRectangle()

    override fun shouldShowItemList(): Boolean = gui.shouldShowItemList()

    override fun isSwitchingScreens(): Boolean = gui.switchingScreens

    override fun onInitGui() {
        gui.switchingScreens = false
        gui.onInventoryUpdate()
        gui.updateScreenSize(width to height)
    }

    override fun removed() {
        super.removed()
        if (!isSwitchingScreens()) gui.onScreenClosed()
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        gui.renderOverlay(this.width, this.height)
    }

    override fun slotChanged(container: AbstractContainerMenu, slotId: Int, stack: SafeItemStack) {
        if (updateScheduled) return
        updateScheduled = true

        DelayedRun.runNextTick {
            updateScheduled = false
            gui.onInventoryUpdate()
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        gui.handleKeybinds()
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        gui.handleKeybinds()
    }
}
