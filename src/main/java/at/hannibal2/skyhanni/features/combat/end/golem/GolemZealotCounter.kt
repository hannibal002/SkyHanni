package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.EnderMan
import java.util.UUID

/**
 * Counts the zealots the player killed on the current server, for [GolemDisplay]. Zealot kills add
 * up to 100 to the protector weight, so the number shows how much of that is already secured.
 *
 * A kill is counted when a zealot the player hit dies. Counting every zealot that dies nearby would
 * include other players' kills, which are common in the End. The catch: a zealot killed without a
 * single hit landing on it - purely by an ability, say - is not counted.
 */
@SkyHanniModule
object GolemZealotCounter {

    /** How far above a zealot its nametag may sit while the mob detection has not paired them yet. */
    private const val NAMETAG_RADIUS = 3.0

    /** Zealots the player hit that are still alive - only these count once they die. */
    private val hitZealots = mutableSetOf<UUID>()

    /** Zealots killed on this server. */
    var kills = 0
        private set

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onEntityClick(event: EntityClickEvent) {
        if (event.action != EntityClickEvent.ActionType.ATTACK) return
        val entity = event.clickedEntity as? EnderMan ?: return
        if (isZealot(entity)) hitZealots.add(entity.uuid)
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onEntityDeath(event: EntityDeathEvent<*>) {
        if (hitZealots.remove(event.entity.uuid)) kills++
    }

    /**
     * Zealot, Zealot Bruiser and Special Zealot all carry "Zealot" in their name. The mob detection
     * usually knows the name already; the nametag right above the entity covers the moment before
     * the two have been paired.
     */
    private fun isZealot(entity: EnderMan): Boolean {
        MobData.entityToMob[entity]?.let { return it.name.contains("Zealot") }
        return entity.getLorenzVec().getEntitiesNearby<ArmorStand>(NAMETAG_RADIUS)
            .any { it.name.string.contains("Zealot") }
    }

    /** Switching to another server starts the count over. */
    @HandleEvent
    private fun onWorldChange(event: WorldChangeEvent) {
        hitZealots.clear()
        kills = 0
    }
}
