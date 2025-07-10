package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items

@SkyHanniModule
object RaffleTicketHighlight {

    val config get() = SkyHanniMod.feature.mining.raffleHighlighter

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onGlow(event: RenderEntityOutlineEvent) {
        if (!config.enabled) return
        // currently xray is never posted on 1.21, but you can never be too sure
        if (event.type != RenderEntityOutlineEvent.Type.NO_XRAY) return
        event.queueEntitiesToOutline { entity -> findTicket(entity) }
    }

    private fun findTicket(entity: Entity): Int? {
        if (entity !is ItemEntity) return null
        if (entity.isGlowing) return null
        val stack = entity.stack
        if (stack.item != Items.NAME_TAG) return null
        if (!stack.name.formattedTextCompat().contains("§aRaffle Ticket")) return null
        return config.ticketColor.getEffectiveColourRGB()
    }
}
