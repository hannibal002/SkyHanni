package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu

class CustomWardrobeScreen(
    menu: ChestMenu,
    title: Component,
) : AbstractCustomMenuScreen(menu, title) {
    var waitingForInventoryUpdate = false
        private set
    private var updateScheduled = false

    override fun getRectangle(): ScreenRectangle = ScreenRectangle(
        CustomWardrobe.renderableTopCorner.first,
        CustomWardrobe.renderableTopCorner.second,
        CustomWardrobe.renderableDimensions.first,
        CustomWardrobe.renderableDimensions.second
    )
    override fun shouldShowItemList(): Boolean = CustomWardrobe.config.showReiItems

    override fun onInitGui() {
        CustomWardrobe.switchingScreens = false
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        CustomWardrobe.renderWardrobeOverlay(this.width, this.height)
    }

    override fun slotChanged(container: AbstractContainerMenu, slotId: Int, stack: SafeItemStack) {
        if (updateScheduled) return
        updateScheduled = true

        DelayedRun.runNextTick {
            waitingForInventoryUpdate = false
            updateScheduled = false
            CustomWardrobe.update()
        }
    }

    override fun removed() {
        CustomWardrobe.reset()
        val player = MinecraftCompat.localPlayerOrNull ?: return
        if (!CustomWardrobe.switchingScreens) {
            menu.removed(player)
        }
        menu.removeSlotListener(this)
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        CustomWardrobeKeybinds.handlePress()
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        CustomWardrobeKeybinds.handlePress()
    }

    fun clickContainerSlot(slot: Int) {
        InventoryUtils.clickSlot(slot, windowId = menu.containerId)
        waitingForInventoryUpdate = true
    }
}
