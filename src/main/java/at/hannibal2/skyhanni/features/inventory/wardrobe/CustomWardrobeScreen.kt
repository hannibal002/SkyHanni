package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.utils.AbstractCustomMenuScreen
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu

class CustomWardrobeScreen(
    menu: ChestMenu,
    title: Component,
    val wardrobe: AbstractCustomWardrobe,
) : AbstractCustomMenuScreen(menu, title) {
    private var updateScheduled = false

    override fun getRectangle(): ScreenRectangle = ScreenRectangle(
        wardrobe.renderableTopCorner.first,
        wardrobe.renderableTopCorner.second,
        wardrobe.renderableDimensions.first,
        wardrobe.renderableDimensions.second,
    )

    override fun shouldShowItemList(): Boolean = wardrobe.config.showReiItems

    override fun isSwitchingScreens(): Boolean = wardrobe.switchingScreens

    override fun onInitGui() {
        wardrobe.switchingScreens = false
        wardrobe.updateScreenSize(width to height)
        // slotChanged is called when a screen is opened, so no need to call wardrobe.onInventoryUpdate() here
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        wardrobe.renderWardrobeOverlay(this.width, this.height)
    }

    override fun slotChanged(container: AbstractContainerMenu, slotId: Int, stack: SafeItemStack) {
        if (updateScheduled) return
        updateScheduled = true

        DelayedRun.runNextTick {
            updateScheduled = false
            wardrobe.onInventoryUpdate()
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        CustomWardrobeKeybinds.handlePress(wardrobe)
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        CustomWardrobeKeybinds.handlePress(wardrobe)
    }
}
