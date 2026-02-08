package at.hannibal2.skyhanni.features.combat.cocoon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData.skyblockMobs
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CocoonAPI {
    private val COCOON_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("RIFT_LARVA") }

    val expectedLifetime = 6.4.seconds
    /*
     roughly where cocoon times landed for me across a few hundred cocoons
     Might require some sort of ping based tweaking?
     */
    val existingCocoons: TimeLimitedSet<CocoonMob> = TimeLimitedSet(8.seconds)
    val logger: LorenzLogger = LorenzLogger("Combat/Cocoon")

    data class CocoonMob(
        val mob: Mob,
        val coordinates: LorenzVec,
        val spawnTime: SimpleTimeMark,
        val cocoonID: Int,
        var hasBeenSeen: Boolean,
        val cocoonEntity: ArmorStand,
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        existingCocoons.forEach { cocoon ->
            if (!cocoon.hasBeenSeen) cocoon.hasBeenSeen = cocoon.cocoonEntity.canBeSeen()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityEquipmentChangeEvent(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (IslandType.THE_RIFT.isCurrent()) return
        if (!event.entity.wearingSkullTexture(COCOON_SKULL_TEXTURE)) return
        val entity = event.entity
        val id = entity.id
        if (existingCocoons.any { (it.coordinates.distanceSqIgnoreY(entity.getLorenzVec()) < 0.5 || it.cocoonID == id) }) return
        val position = entity.getLorenzVec()
        val mob = getCocoonMob(position) ?: return
        val cocoon = CocoonMob(mob, position, SimpleTimeMark.now(), id, entity.canBeSeen(), entity)
        if (existingCocoons.any { (it.coordinates.distanceSqIgnoreY(entity.getLorenzVec()) < 0.5) }) return
        existingCocoons.add(cocoon)
        logger.log("${mob.name} Cocoon (${cocoon.cocoonID} Entered List")
        CocoonSpawnEvent(cocoon).post()
    }


    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<ArmorStand>) {
        if (IslandType.THE_RIFT.isCurrent()) return
        val cocoon = existingCocoons.firstOrNull { it.cocoonID == event.entity.id } ?: return
        val cocoonMob = cocoon.mob
        val timeSince = cocoon.spawnTime.passedSince()
        logger.log("name: (${cocoonMob.name}), Type: (${cocoonMob.mobType}), Cocoon: (${cocoon.cocoonID}) Left World After $timeSince")
        existingCocoons.removeIf { it.cocoonID == event.entity.id }
    }


    @HandleEvent(onlyOnSkyblock = true)
    fun onWorldChange(event: WorldChangeEvent) {
        existingCocoons.clear()
    }

    private fun getCocoonMob(cocoonVector: LorenzVec): Mob? {
        val mob = skyblockMobs.minByOrNull { it.baseEntity.getLorenzVec().distanceIgnoreY(cocoonVector) } ?: return null
        if (mob.baseEntity.getLorenzVec().distanceSqOnlyY(cocoonVector) > 4.0) return null
        return mob
    }

}
