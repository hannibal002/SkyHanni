package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.visitor.VisitorConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacks
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.SackDataUpdateEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToIgnoreY
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.math.round

/**
 * Manages visitor status updates based on player inventory and entity proximity.
 * The "Brain" that checks if requirements are met and updates VisitorApi state.
 */
@SkyHanniModule
object GardenVisitorStatus {

    private val config get() = VisitorApi.config

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.shoppingList.enabled &&
            config.highlightStatus == VisitorConfig.HighlightMode.DISABLED
        ) return
        if (!event.isMod(10, 2)) return

        if (GardenApi.onBarnPlot) {
            checkVisitorsReady()
        }
    }

    @HandleEvent(OwnInventoryItemUpdateEvent::class)
    fun onOwnInventoryItemUpdate() {
        if (GardenApi.onBarnPlot) {
            update()
        }
    }

    @HandleEvent(SackDataUpdateEvent::class)
    fun onSackUpdate() {
        update()
    }

    @HandleEvent(VisitorRefusedEvent::class)
    fun onVisitorRefused() {
        update()
        GardenApi.storage?.visitorDrops?.let { it.deniedVisitors += 1 }
        GardenVisitorDropStatistics.saveAndUpdate()
    }

    @HandleEvent
    fun onVisitorAccepted(event: VisitorAcceptedEvent) {
        VisitorAcceptEvent(event.visitor).post()
        update()
        GardenApi.storage?.visitorDrops?.let {
            it.coinsSpent += round(GardenVisitorTooltip.lastFullPrice).toLong()
        }
    }

    /**
     * Central update trigger. Called when inventory, sacks, or visitor state changes.
     * Updates visitor status and triggers UI refresh.
     */
    fun update() {
        checkVisitorsReady()
        GardenVisitorShoppingList.updateDisplay()
    }

    /**
     * Main checking loop. Iterates all visitors and:
     * 1. Finds their entity if missing
     * 2. Checks if player has required items
     * 3. Updates status (WAITING vs READY)
     * 4. Delegates entity highlighting to GardenVisitorHighlight
     */
    private fun checkVisitorsReady() {
        for (visitor in VisitorApi.getVisitors()) {
            val visitorName = visitor.visitorName
            val entity = visitor.getEntity()

            if (entity == null) {
                NpcVisitorFix.findNametag(visitorName.removeColor())?.let {
                    findEntity(it, visitor)
                }
            }

            if (visitor.status in setOf(
                    VisitorApi.VisitorStatus.WAITING,
                    VisitorApi.VisitorStatus.READY,
                )
            ) {
                if (hasItems(visitor)) {
                    VisitorApi.changeStatus(visitor, VisitorApi.VisitorStatus.READY, "hasItems")
                } else {
                    VisitorApi.changeStatus(visitor, VisitorApi.VisitorStatus.WAITING, "noLongerHasItems")
                }
            }

            if (entity is LivingEntity) {
                GardenVisitorHighlight.updateEntityColor(visitor, entity)
            }
        }
    }

    /**
     * Checks if player has all required items for a visitor.
     * Combines inventory + sacks.
     */
    fun hasItems(visitor: VisitorApi.Visitor): Boolean {
        for ((internalName, required) in visitor.shoppingList) {
            val having = internalName.getAmountInInventory() + internalName.getAmountInSacks()
            if (having < required) {
                return false
            }
        }
        return true
    }

    /**
     * Links a visitor to their Minecraft entity.
     * Finds the actual NPC entity near the nametag armor stand.
     */
    private fun findEntity(nameTag: ArmorStand, visitor: VisitorApi.Visitor) {
        val nameTagVec = nameTag.getLorenzVec()
        nameTagVec.getEntitiesNearby<LivingEntity>(5.0) { entity ->
            entity !is ArmorStand && entity !is LocalPlayer &&
                entity.distanceToIgnoreY(nameTagVec) < 0.5
        }.forEach {
            visitor.entityId = it.id
            visitor.nameTagEntityId = nameTag.id
        }
    }
}
