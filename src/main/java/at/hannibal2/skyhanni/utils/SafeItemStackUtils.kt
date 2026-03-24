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
     */
    var componentsLoaded: Boolean = false
        private set

    @HandleEvent
    fun onComponentsLoaded(event: ComponentsLoadedEvent) {
        componentsLoaded = true
    }
}
