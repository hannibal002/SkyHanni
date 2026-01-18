package at.hannibal2.skyhanni.features.combat.cocoon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.fishing.LivingSeaCreatureData
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi.seaCreature
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.seconds

@Suppress("MaxLineLength")
@SkyHanniModule
object CocoonAPI {
    private val COCOON_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("RIFT_LARVA") }
    private val mobRecentDeaths = mutableMapOf<Mob, SimpleTimeMark>()
    private val recentCocoonMobs = mutableListOf<CocoonMob>()
    private val recentSeaCreatures = mutableMapOf<Mob, LivingSeaCreatureData?>()

    data class CocoonMob(
        val mob: Mob,
        val seaCreature: LivingSeaCreatureData?,
        val coordinates: LorenzVec,
        val spawnTime: SimpleTimeMark,
        val cocoonID: Int,
    )

    @HandleEvent
    fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        val entity = event.entity as? ArmorStand ?: return
        if (recentCocoonMobs.any { (it.coordinates.distanceSqIgnoreY(entity.getLorenzVec()) < 0.5 || it.cocoonID == event.entity.id) }) return
        if (entity.wearingSkullTexture(COCOON_SKULL_TEXTURE)) {
            val position = entity.getLorenzVec()
            val mob = getCocoonMobName(position) ?: return
            val id = entity.id
            ChatUtils.debug("Cocoon mob detected ${mob.name}, ${position.toCleanString()}")
            val cocoon = CocoonMob(mob, recentSeaCreatures[mob], position, SimpleTimeMark.now(), id)
            recentCocoonMobs.add(cocoon)
            CocoonSpawnEvent(cocoon).post()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSkyblockMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        mobRecentDeaths[event.mob] = SimpleTimeMark.now()
        recentSeaCreatures[event.mob] = event.mob.seaCreature
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        mobRecentDeaths.removeIf { it.value.passedSince() > 3.seconds }
        recentCocoonMobs.removeIf { it.spawnTime.passedSince() > 10.seconds }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onWorldChange(event: WorldChangeEvent) {
        mobRecentDeaths.clear()
        recentCocoonMobs.clear()
    }

    private fun getCocoonMobName(cocoonVector: LorenzVec): Mob? {
        val mob = mobRecentDeaths.minByOrNull { it.key.baseEntity.getLorenzVec().distanceSqIgnoreY(cocoonVector) } ?: return null
        if (mob.key.baseEntity.getLorenzVec().distanceSqOnlyY(cocoonVector) > 4.0) return null
        return mob.key
    }

}
