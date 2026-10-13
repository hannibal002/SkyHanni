package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.world.inventory.ChestMenu
import java.awt.Color

/**
 * A custom gui that replaces a skyblock menu. It is shown by [CustomMenuScreen],
 * while the edit mode ([CustomMenuEditScreen]) shows the original skyblock menu instead.
 */
abstract class AbstractCustomMenu(internal val guiName: String) {

    internal var switchingScreens = false
    val inCustomMenu get() = (MinecraftCompat.screen as? CustomMenuScreen)?.gui === this
    val editMode get() = (MinecraftCompat.screen as? CustomMenuEditScreen)?.gui === this

    /** The area of the screen that is covered by the gui, or null to use the whole screen. */
    open val rectangle: ScreenRectangle? get() = null

    // Called by MenuScreensHook
    internal abstract fun shouldReplace(inventoryName: String): Boolean

    abstract fun renderOverlay(screenWidth: Int, screenHeight: Int)

    internal abstract fun updateScreenSize(gui: Pair<Int, Int>): Boolean

    internal abstract fun onInventoryUpdate()

    internal abstract fun handleKeybinds()

    abstract fun createLabeledButton(
        text: String,
        hoveredColor: Color = Color(130, 130, 130, 200),
        unhoveredColor: Color = hoveredColor.darker(0.57),
        onClick: () -> Unit,
    ): Renderable

    open fun shouldShowItemList(): Boolean = false

    internal open fun onScreenClosed() = Unit

    fun enterEditMode() {
        val screen = MinecraftCompat.screen as? CustomMenuScreen ?: return
        val player = MinecraftCompat.localPlayerOrNull ?: return

        switchingScreens = true
        MinecraftCompat.screen = CustomMenuEditScreen(
            screen.menu,
            player.inventory,
            screen.title,
            this,
        )
    }

    fun exitEditMode() {
        val screen = MinecraftCompat.screen as? CustomMenuEditScreen ?: return
        val player = MinecraftCompat.localPlayerOrNull ?: return
        val handler = player.containerMenu as? ChestMenu ?: return

        switchingScreens = true
        MinecraftCompat.screen = CustomMenuScreen(
            handler,
            screen.title,
            this,
        )
    }
}
