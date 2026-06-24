@file:Suppress("VanillaItemStackImport")

package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ComponentsLoadedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SafeItemStackUtils.componentsLoaded
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.compat.getStringOrDefault
import net.minecraft.core.component.DataComponents
import kotlin.time.Duration.Companion.seconds

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

    @JvmStatic
    fun getUniqueIdentifier(item: SafeItemStack): Int {
        getUuidDirect(item)?.let { return it.hashCode() }
        return SafeItemStack.hashItemAndComponents(item)
    }

    // Do not use getItemUuid since then it would recursively call this function and cause a stack overflow
    private fun getUuidDirect(item: SafeItemStack): String? {
        val extraAttributes = item.get(DataComponents.CUSTOM_DATA) ?: return null
        return extraAttributes.getStringOrDefault("uuid").takeUnless { it.isBlank() }
    }
}
