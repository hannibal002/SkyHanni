package at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.mining.CorpseFoundEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftWaypoint
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftWaypoint.Type
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftWaypointManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand

// TODO: Maybe implement automatic warp-in for chosen players if the user is not in a party.
@SkyHanniModule
object CorpseFinder {

    private const val MARK_AS_FOUND_TICKS_THRESHOLD = 10

    /**
     * WRAPPED-REGEX-TEST: " Lapis: NOT LOOTED"
     * WRAPPED-REGEX-TEST: " Tungsten: NOT LOOTED"
     * WRAPPED-REGEX-TEST: " Umber: NOT LOOTED"
     * WRAPPED-REGEX-TEST: " Vanguard: NOT LOOTED"
     * WRAPPED-REGEX-TEST: " Lapis: LOOTED"
     * WRAPPED-REGEX-TEST: " Tungsten: LOOTED"
     * WRAPPED-REGEX-TEST: " Umber: LOOTED"
     * WRAPPED-REGEX-TEST: " Vanguard: LOOTED"
     */
    private val tabWidgetCorpsePattern by RepoPattern.pattern(
        "mining.glacitemineshaft.tabwidgetcorpse",
        "\\s*(?<corpse>\\w+): (?:NOT )?LOOTED\\s*",
    )

    // Map with the corpse entity as the key and the consecutive ticks count of passing canBeSeen checks as value
    private val corpseEntities = mutableMapOf<ArmorStand, Int>()
    private val potentialWaypoints = mutableMapOf<MineshaftWaypoint, Int>()
    private var totalCorpseCount = 0

    private fun areAllCorpsesFound(): Boolean {
        return totalCorpseCount > 0 &&
            totalCorpseCount == corpseEntities.size &&
            corpseEntities.all { it.value >= MARK_AS_FOUND_TICKS_THRESHOLD }
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    private fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (!event.isHead || event.newItemStack == null) return
        if (!CorpseType.isValidHelmet(event.newItemStack.getInternalName())) return
        if (corpseEntities.any { it.key.uuid == event.entity.uuid }) return

        corpseEntities[event.entity] = 0
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    private fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        for ((entity, canBeSeenTicks) in corpseEntities) {
            if (canBeSeenTicks >= MARK_AS_FOUND_TICKS_THRESHOLD) continue

            if (!entity.getLorenzVec().canBeSeen(-1..3)) {
                corpseEntities[entity] = 0
                continue
            }

            val corpseType = CorpseType.fromEntityOrNull(entity) ?: ErrorManager.skyHanniError(
                "Got CorpseType of null for entity in corpseEntities",
                "event" to "EntityMoveEvent<LocalPlayer>",
                "helmet" to entity.equipment.get(EquipmentSlot.HEAD).getInternalName(),
                "location" to entity.getLorenzVec(),
            )

            if (corpseEntities.addOrPut(entity, 1) >= MARK_AS_FOUND_TICKS_THRESHOLD) {
                CorpseFoundEvent(corpseType, entity.getLorenzVec().up(), areAllCorpsesFound()).post()
            }
        }

        MineshaftWaypointManager.waypoints.removeIf { waypoint ->
            if (waypoint.type != Type.POTENTIAL_CORPSE) return@removeIf false
            if (areAllCorpsesFound()) return@removeIf true

            if (!waypoint.location.canBeSeen(-1..3)) {
                potentialWaypoints[waypoint] = 0
                return@removeIf false
            }

            if (potentialWaypoints.addOrPut(waypoint, 1) < MARK_AS_FOUND_TICKS_THRESHOLD) return@removeIf false

            corpseEntities.none { it.key.getLorenzVec().distance(waypoint.location) <= 3 }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    private fun onEntityClick(event: EntityClickEvent) {
        val clickedEntityUuid = event.clickedEntity.uuid
        val (entity, canBeSeenTicks) = corpseEntities.entries.firstOrNull { it.key.uuid == clickedEntityUuid } ?: return

        if (canBeSeenTicks >= MARK_AS_FOUND_TICKS_THRESHOLD) return

        val corpseType = CorpseType.fromEntityOrNull(entity) ?: ErrorManager.skyHanniError(
            "Got CorpseType of null for entity in corpseEntities",
            "event" to "EntityClickEvent",
            "helmet" to entity.equipment.get(EquipmentSlot.HEAD).getInternalName(),
            "location" to entity.getLorenzVec(),
        )

        corpseEntities[entity] = MARK_AS_FOUND_TICKS_THRESHOLD
        CorpseFoundEvent(corpseType, entity.getLorenzVec().up(), areAllCorpsesFound()).post()
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (event.widget != TabWidget.FROZEN_CORPSES) return
        totalCorpseCount = event.lines.count { tabWidgetCorpsePattern.matches(it) }
    }

    @HandleEvent
    private fun onWorldChange() {
        corpseEntities.clear()
        totalCorpseCount = 0
    }
}
