package at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.mining.CorpseFoundEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftWaypoints
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getStandHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand

// TODO: Maybe implement automatic warp-in for chosen players if the user is not in a party.
@SkyHanniModule
object CorpseFinder {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.corpseLocator

    // TODO: use entity events
    @OptIn(AllEntitiesGetter::class)
    private fun findCorpse() {
        EntityUtils.getAllEntities().filterIsInstance<ArmorStand>()
            .filterNot { corpse -> MineshaftWaypoints.waypoints.any { it.location.distance(corpse.getLorenzVec()) <= 3 } }
            .filter { entity ->
                entity.showArms() && entity.showBasePlate().not() && !entity.isInvisible
            }
            .forEach { entity ->
                val helmetName = entity.getStandHelmet()?.getInternalName() ?: return
                val corpseType = CorpseType.getByHelmetOrNull(helmetName) ?: return

                val canSee = entity.getLorenzVec().canBeSeen(-1..3)
                if (canSee) {
                    val location = entity.getLorenzVec().up()
                    CorpseFoundEvent(corpseType, location).post()
                }
            }
    }

    @HandleEvent
    fun onSecondPassed() {
        if (!isEnabled()) return

        findCorpse()
    }

    fun isEnabled() = IslandType.MINESHAFT.isInIsland() && config.enabled
}
