package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.utils.AbstractCustomMenuScreen
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu

class CustomLoadoutScreen(
    menu: ChestMenu,
    title: Component,
) : AbstractCustomMenuScreen(menu, title) {
    private var updateScheduled = false

    override fun isSwitchingScreens(): Boolean = CustomLoadout.switchingScreens

    override fun onInitGui() {
        CustomLoadout.switchingScreens = false
        CustomLoadout.updateScreenSize(width to height)
    }

    override fun removed() {
        super.removed()
        if (!isSwitchingScreens()) CustomLoadout.reset()
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        CustomLoadout.renderLoadoutOverlay(this.width, this.height)
    }

    override fun slotChanged(container: AbstractContainerMenu, slotId: Int, stack: SafeItemStack) {
        if (updateScheduled) return
        updateScheduled = true

        DelayedRun.runNextTick {
            updateScheduled = false
            CustomLoadout.onInventoryUpdate()
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        CustomLoadoutKeybinds.handlePress()
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        CustomLoadoutKeybinds.handlePress()
    }
}
