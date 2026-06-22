@file:Suppress("VanillaItemStackImport")

package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ComponentsLoadedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

/**
 * Tracks whether Minecraft's item component data has been fully bound.
 *
 * [componentsLoaded] lets callers avoid direct [net.minecraft.world.item.ItemStack]
 * work until the "Components not bound yet" crash introduced in 26.1 can no longer occur.
 */
@SkyHanniModule
object SafeItemStackUtils {

    /**
     * `true` once [ComponentsLoadedEvent] has fired, meaning it is safe to
     * construct [net.minecraft.world.item.ItemStack] instances from vanilla items.
     *
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
}
