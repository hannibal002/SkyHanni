package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import me.shedaniel.math.Point
import me.shedaniel.rei.api.client.REIRuntime
import me.shedaniel.rei.api.client.gui.widgets.Slot
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object ReiCompat {

    private val isReiLoaded by lazy { PlatformUtils.isModInstalled("roughlyenoughitems") }

    @JvmStatic
    fun searchHasFocus(): Boolean {
        if (!isReiLoaded) return false
        if (MinecraftCompat.screen == null) return false
        return try {
            (REIRuntime.getInstance().searchTextField as? GuiEventListener)?.isFocused == true
        } catch (e: Throwable) {
            false
        }
    }

    fun getHoveredStackFromRei(): SafeItemStack? {
        if (!isReiLoaded) return null
        try {
            REIRuntime.getInstance()
        } catch (e: Throwable) {
            return null
        }
        var stack = getItemStackFromItemList()
        if (stack == null) {
            val screen = MinecraftCompat.screen
            if (screen !is AbstractContainerScreen<*>) return null
            stack = getItemStackFromRecipe(screen)
        }
        return stack
    }


    private fun getItemStackFromRecipe(screen: AbstractContainerScreen<*>): SafeItemStack? {
        val entryStack = ScreenRegistry.getInstance().getFocusedStack(screen, currentMousePoint())
            ?: return null
        return entryStack.value as? SafeItemStack ?: entryStack.cheatsAs().value
    }

    private fun getItemStackFromItemList(): SafeItemStack? {
        var baseElement: GuiEventListener? = REIRuntime.getInstance().overlay.orElse(null)
        val mousePoint = currentMousePoint()
        while (true) {
            if (baseElement is Slot) return baseElement.currentEntry.cheatsAs().value
            if (baseElement !is ContainerEventHandler) return null
            baseElement = baseElement.getChildAt(mousePoint.x.toDouble(), mousePoint.y.toDouble()).orElse(null)
        }
    }

    private fun currentMousePoint(): Point = Point(MouseCompat.getX(), MouseCompat.getY())
}
