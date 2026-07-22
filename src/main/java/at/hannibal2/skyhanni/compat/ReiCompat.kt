package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import me.shedaniel.math.Point
import me.shedaniel.rei.api.client.REIRuntime
import me.shedaniel.rei.api.client.gui.widgets.Slot
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry
import me.shedaniel.rei.api.common.entry.type.EntryTypeRegistry
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes
import me.shedaniel.rei.api.common.plugins.PluginManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object ReiCompat {

    private val isReiLoaded by lazy { PlatformUtils.isModInstalled("roughlyenoughitems") }

    @JvmStatic
    fun searchHasFocus(): Boolean {
        if (!isReiReady()) return false
        if (Minecraft.getInstance().screen == null) return false
        return try {
            (REIRuntime.getInstance().searchTextField as? GuiEventListener)?.isFocused == true
        } catch (e: Throwable) {
            false
        }
    }

    fun getHoveredStackFromRei(): SafeItemStack? {
        if (!isReiReady()) return null
        return try {
            getItemStackFromItemList() ?: (Minecraft.getInstance().screen as? AbstractContainerScreen<*>)
                ?.let(::getItemStackFromRecipe)
        } catch (e: Throwable) {
            null
        }
    }

    private fun isReiReady(): Boolean {
        if (!isReiLoaded) return false
        return try {
            !PluginManager.areAnyReloading() &&
                EntryTypeRegistry.getInstance().get(VanillaEntryTypes.ITEM.id) != null
        } catch (e: Throwable) {
            false
        }
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
