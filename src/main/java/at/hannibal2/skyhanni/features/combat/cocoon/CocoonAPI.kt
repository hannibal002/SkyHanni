package at.hannibal2.skyhanni.features.combat.cocoon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData.skyblockMobs
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand

@Suppress("MaxLineLength")
@SkyHanniModule
object CocoonAPI {
    private val COCOON_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("RIFT_LARVA") }

    val existingCocoons: MutableList<CocoonMob> = mutableListOf()
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
    fun onWorldRender(event: SkyHanniRenderWorldEvent) {
        existingCocoons.forEach { cocoon ->
            if (!cocoon.hasBeenSeen) cocoon.hasBeenSeen = cocoon.cocoonEntity.canBeSeen()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityEnterWorldEvent(event: EntityEnterWorldEvent<ArmorStand>) {
        val entity = event.entity
        val id = entity.id
        if (existingCocoons.any { (it.coordinates.distanceSqIgnoreY(entity.getLorenzVec()) < 0.5 || it.cocoonID == id) }) return
        val position = entity.getLorenzVec()
        val mob = getCocoonMob(position) ?: return
        val cocoon = CocoonMob(mob, position, SimpleTimeMark.now(), id, entity.canBeSeen(), entity)
        if (existingCocoons.any { (it.coordinates.distanceSqIgnoreY(entity.getLorenzVec()) < 0.5) }) return
        if (event.entity.wearingSkullTexture(COCOON_SKULL_TEXTURE)) {
            existingCocoons.add(cocoon)
            ChatUtils.debug("${cocoon.mob.name} Cocoon (${cocoon.cocoonID} Entered List")
            logger.log("${cocoon.mob.name} Cocoon (${cocoon.cocoonID} Entered List")
            CocoonSpawnEvent(cocoon).post()
        }
    }


    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<ArmorStand>) {
        val cocoon = existingCocoons.firstOrNull { it.cocoonID == event.entity.id } ?: return
        ChatUtils.debug("${cocoon.mob.name}  Cocoon (${cocoon.cocoonID}) Left World After ${cocoon.spawnTime.passedSince()}")
        logger.log("${cocoon.mob.name} (Type: ${cocoon.mob.mobType}) Cocoon (${cocoon.cocoonID}) Left World after ${cocoon.spawnTime.passedSince()}")
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
