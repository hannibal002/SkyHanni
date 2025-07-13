package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.utils.system.PlatformUtils
import me.shedaniel.math.impl.PointHelper
import me.shedaniel.rei.api.client.REIRuntime
import me.shedaniel.rei.api.client.gui.widgets.Slot
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack

object ReiCompat {

    private val isReiLoaded by lazy { PlatformUtils.isModInstalled("roughlyenoughitems") }

    @JvmStatic
    fun searchHasFocus(): Boolean {
        if (!isReiLoaded) return false
        return try {
            REIRuntime.getInstance().searchTextField?.isFocused == true
        } catch (e: Throwable) {
            false
        }
    }

    fun getHoveredStackFromRei(): ItemStack? {
        if (!isReiLoaded) return null
        try {
            REIRuntime.getInstance()
        } catch (e: Throwable) {
            return null
        }
        var stack = getItemStackFromItemList()
        if (stack == null) {
            val screen = MinecraftClient.getInstance().currentScreen
            if (screen !is HandledScreen<*>) return null
            stack = getItemStackFromRecipe(screen)
        }
        return stack
    }


    private fun getItemStackFromRecipe(screen: HandledScreen<*>): ItemStack? {
        val entryStack = ScreenRegistry.getInstance().getFocusedStack(screen, PointHelper.ofMouse())
            ?: return null
        return entryStack.value as? ItemStack ?: entryStack.cheatsAs().value
    }

    private fun getItemStackFromItemList(): ItemStack? {
        var baseElement: Element? = REIRuntime.getInstance().overlay.orElse(null)
        val mx = PointHelper.getMouseFloatingX()
        val my = PointHelper.getMouseFloatingY()
        while (true) {
            if (baseElement is Slot) return baseElement.currentEntry.cheatsAs().value
            if (baseElement !is ParentElement) return null
            baseElement = baseElement.hoveredElement(mx, my).orElse(null)
        }
    }
}
