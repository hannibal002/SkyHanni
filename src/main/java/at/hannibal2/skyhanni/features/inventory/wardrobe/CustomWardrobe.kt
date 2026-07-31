package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.customwardrobe.CustomWardrobeConfig
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.CustomRenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.world.inventory.ChestMenu
import java.awt.Color

object CustomWardrobe {
    val config: CustomWardrobeConfig get() = SkyHanniMod.feature.inventory.customWardrobe

    internal var switchingScreens = false
    val inCustomWardrobe get() = MinecraftCompat.screen is CustomWardrobeScreen
    val editMode get() = MinecraftCompat.screen is CustomWardrobeEditScreen

    internal const val GUI_NAME = "Custom Wardrobe"

    // Called by MenuScreensHook
    internal fun shouldReplace(inventoryName: String): Boolean {
        if (!isEnabled()) return false
        return ArmorWardrobeApi.matchesInventoryName(inventoryName)
    }

    fun enterEditMode() {
        val screen = MinecraftCompat.screen as? CustomWardrobeScreen ?: return
        val player = MinecraftCompat.localPlayerOrNull ?: return

        switchingScreens = true
        MinecraftCompat.screen = CustomWardrobeEditScreen(
            screen.menu,
            player.inventory,
            screen.title,
        )
    }

    fun exitEditMode() {
        val screen = MinecraftCompat.screen ?: return
        val player = MinecraftCompat.localPlayerOrNull ?: return
        val handler = player.containerMenu as? ChestMenu ?: return

        switchingScreens = true
        MinecraftCompat.screen = CustomWardrobeScreen(
            handler,
            screen.title,
        )
    }

    // TODO: Make this work without the CustomWardrobeScreen
    fun WardrobeSlot.clickSlot() {
        val previousPageSlot = 45
        val nextPageSlot = 53
        val wardrobePage = ArmorWardrobeApi.currentPage ?: return
        val screen = MinecraftCompat.screen as? CustomWardrobeScreen ?: return
        if (isInCurrentPage()) {
            if (isEmpty() || locked || screen.waitingForInventoryUpdate) return
            ArmorWardrobeApi.currentSlot = if (isCurrentSlot()) null else id
            InventoryUtils.clickSlot(inventorySlot)
        } else {
            if (page < wardrobePage) {
                ArmorWardrobeApi.currentPage = wardrobePage - 1
                InventoryUtils.clickSlot(previousPageSlot)
            } else if (page > wardrobePage) {
                ArmorWardrobeApi.currentPage = wardrobePage + 1
                InventoryUtils.clickSlot(nextPageSlot)
            }
        }
    }

    fun createLabeledButton(
        text: String,
        hoveredColor: Color = Color(130, 130, 130, 200),
        unhoveredColor: Color = hoveredColor.darker(0.57),
        onClick: () -> Unit,
        scale: Int = config.spacing.globalScale.get(),
    ): Renderable =
        CustomRenderUtils.createLabeledButton(
            text = text,
            hoveredColor = hoveredColor,
            unhoveredColor = unhoveredColor,
            onClick = onClick,
            width = config.spacing.buttonWidth.get(),
            height = config.spacing.buttonHeight.get(),
            topBorderColor = config.color.topBorderColor,
            bottomBorderColor = config.color.bottomBorderColor,
            scale = scale,
        )

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
