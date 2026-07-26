package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Display

/**
 * Event that is fired when a text display entity's text is updated.
 * Prefer using [EntityTextUpdateEvent] unless you explicitly know that the entity is a text display entity.
 */
@PrimaryFunction("onDisplayTextUpdate")
data class DisplayTextUpdateEvent(
    val entity: Display.TextDisplay,
    val newText: Component?,
) : SkyHanniEvent()
