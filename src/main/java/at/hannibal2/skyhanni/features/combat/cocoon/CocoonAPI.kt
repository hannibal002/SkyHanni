package at.hannibal2.skyhanni.features.combat.cocoon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData.skyblockMobs
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.seconds

@Suppress("MaxLineLength")
@SkyHanniModule
object CocoonAPI {
    private val COCOON_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("RIFT_LARVA") }

    private val recentCocoonMobs: TimeLimitedSet<CocoonMob> = TimeLimitedSet(8.seconds)
    private val recentMobs: TimeLimitedSet<Mob> = TimeLimitedSet(1.seconds)

    val logger: LorenzLogger = LorenzLogger("Combat/Cocoon")

    data class CocoonMob(
        val mob: Mob,
        val coordinates: LorenzVec,
        val spawnTime: SimpleTimeMark,
        val cocoonID: Int,
    )

    @HandleEvent
    fun onCheckRenderEntityEvent(event: CheckRenderEntityEvent<ArmorStand>) {
        val entity = event.entity
        if (recentCocoonMobs.any { (it.coordinates.distanceSqIgnoreY(entity.getLorenzVec()) < 0.5 || it.cocoonID == event.entity.id) }) return
        if (entity.wearingSkullTexture(COCOON_SKULL_TEXTURE)) {
            val position = entity.getLorenzVec()
            val mob = getCocoonMob(position) ?: return
            val id = entity.id
            val cocoon = CocoonMob(mob, position, SimpleTimeMark.now(), id)
            recentCocoonMobs.add(cocoon)
            ChatUtils.debug("${cocoon.mob.name}  Cocoon (${cocoon.cocoonID} Entered List")
            logger.log("${cocoon.mob.name} Cocoon (${cocoon.cocoonID} Entered List")
            CocoonSpawnEvent(cocoon).post()
        }
    }

    @HandleEvent
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<ArmorStand>) {
        val cocoon = recentCocoonMobs.firstOrNull { it.cocoonID == event.entity.id } ?: return
        ChatUtils.debug("${cocoon.mob.name}  Cocoon (${cocoon.cocoonID}) Left World After ${cocoon.spawnTime.passedSince()}")
        logger.log("${cocoon.mob.name} (Type: ${cocoon.mob.mobType}) Cocoon (${cocoon.cocoonID}) Left World after ${cocoon.spawnTime.passedSince()}")
    }

    @HandleEvent
    fun onTick() {
        skyblockMobs.forEach {
            if (!recentMobs.contains(it)) {
                recentMobs.add(it)
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onWorldChange(event: WorldChangeEvent) {
        recentMobs.clear()
        recentCocoonMobs.clear()
    }

    private fun getCocoonMob(cocoonVector: LorenzVec): Mob? {
        val mob = recentMobs.minByOrNull { it.baseEntity.getLorenzVec().distanceIgnoreY(cocoonVector) } ?: return null
        if (mob.baseEntity.getLorenzVec().distanceSqOnlyY(cocoonVector) > 4.0) return null
        return mob
    }

}
