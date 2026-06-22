package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired once when Minecraft's item component data has been fully bound,
 * and it is safe to create [net.minecraft.world.item.ItemStack] instances
 * from vanilla items.
 *
 * This corresponds to [net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED].
 */
@PrimaryFunction("onComponentsLoaded")
object ComponentsLoadedEvent : SkyHanniEvent()
