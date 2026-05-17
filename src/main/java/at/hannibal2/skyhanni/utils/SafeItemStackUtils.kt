package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ComponentsLoadedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

/**
 * Tracks whether Minecraft's item component data has been fully bound.
 *
 * [componentsLoaded] is read by [SafeItemStack] before constructing any
 * [net.minecraft.world.item.ItemStack] instance, ensuring the "Components not
 * bound yet" crash introduced in 26.1 cannot occur through [SafeItemStack].
 */
@SkyHanniModule
object SafeItemStackUtils {

    /**
     * `true` once [ComponentsLoadedEvent] has fired, meaning it is safe to
     * construct [net.minecraft.world.item.ItemStack] instances from vanilla items.
     *
     * Can be reset to `false` by [markComponentsNotLoaded] if a construction attempt
     * reveals that the signal fired prematurely (i.e. components were not actually
     * bound yet despite [ComponentsLoadedEvent] having fired).
     */
    var componentsLoaded: Boolean = false
        private set

    init {
        //? if < 26.1
        //componentsLoaded = true
    }

    @HandleEvent
    fun onComponentsLoaded() {
        componentsLoaded = true
    }

    /**
     * Called by [SafeItemStack] when construction throws "Components not bound yet"
     * even though [componentsLoaded] was `true`. Resets the flag so future calls
     * return [net.minecraft.world.item.ItemStack.EMPTY] until [ComponentsLoadedEvent]
     * fires again.
     */
    fun markComponentsNotLoaded() {
        componentsLoaded = false
    }
}
