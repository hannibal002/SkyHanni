// TODO 26.1 rei compat needed
//? if < 26.1 {
/*package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import me.shedaniel.math.impl.PointHelper
import me.shedaniel.rei.api.client.REIRuntime
import me.shedaniel.rei.api.client.gui.widgets.Slot
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object ReiCompat {

    private val isReiLoaded by lazy { PlatformUtils.isModInstalled("roughlyenoughitems") }

    @JvmStatic
    fun searchHasFocus(): Boolean {
        if (!isReiLoaded) return false
        if (Minecraft.getInstance().screen == null) return false
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
            val screen = Minecraft.getInstance().screen
            if (screen !is AbstractContainerScreen<*>) return null
            stack = getItemStackFromRecipe(screen)
        }
        return stack
    }


    private fun getItemStackFromRecipe(screen: AbstractContainerScreen<*>): SafeItemStack? {
        val entryStack = ScreenRegistry.getInstance().getFocusedStack(screen, PointHelper.ofMouse())
            ?: return null
        return entryStack.value as? SafeItemStack ?: entryStack.cheatsAs().value
    }

    private fun getItemStackFromItemList(): SafeItemStack? {
        var baseElement: GuiEventListener? = REIRuntime.getInstance().overlay.orElse(null)
        val mx = PointHelper.getMouseFloatingX()
        val my = PointHelper.getMouseFloatingY()
        while (true) {
            if (baseElement is Slot) return baseElement.currentEntry.cheatsAs().value
            if (baseElement !is ContainerEventHandler) return null
            baseElement = baseElement.getChildAt(mx, my).orElse(null)
        }
    }
}
*///?}
