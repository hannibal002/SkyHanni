package at.hannibal2.skyhanni.features.combat.cocoon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CocoonAPI {
    private const val cocoonTexture = "eyJ0aW1lc3RhbXAiOjE1ODMxMjMyODkwNTMsInByb2ZpbGVJZCI6IjkxZjA0ZmU5MGYzNjQzYjU4ZjIwZTMzNzVmODZkMzllIiwicHJvZmlsZU5hbWUiOiJTdG9ybVN0b3JteSIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNlYjBlZDhmYzIyNzJiM2QzZDgyMDY3NmQ1MmEzOGU3YjJlOGRhOGM2ODdhMjMzZTBkYWJhYTE2YzBlOTZkZiJ9fX0="
    private val mobRecentDeaths = mutableMapOf<Mob, SimpleTimeMark>()
    private val recentCocoonMobs = mutableListOf<CocoonMob>()

    data class CocoonMob(
        val mobName: String,
        val coordinates: LorenzVec,
        val spawnTime: SimpleTimeMark,
        val cocoonID: Int,
    )

    @HandleEvent
    fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        val entity = event.entity as? ArmorStand ?: return
        if (recentCocoonMobs.any { (it.coordinates.distanceSqIgnoreY(entity.getLorenzVec()) < 0.5 || it.cocoonID == event.entity.id) }) return
        if (entity.wearingSkullTexture(cocoonTexture)) {
            val position = entity.getLorenzVec()
            val name = getCocoonMobName(position) ?: return
            val id = entity.id
            ChatUtils.debug("Cocoon mob detected $name, ${position.toCleanString()}")
            val cocoon = CocoonMob(name, position, SimpleTimeMark.now(), id)
            recentCocoonMobs.add(cocoon)
            CocoonSpawnEvent(cocoon).post()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSkyblockMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        mobRecentDeaths[event.mob] = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        mobRecentDeaths.removeIf { it.value.passedSince() > 3.seconds }
        recentCocoonMobs.removeIf { it.spawnTime.passedSince() > 10.seconds }
    }

    private fun getCocoonMobName(cocoonVector: LorenzVec): String? {
        val mob = mobRecentDeaths.minByOrNull { it.key.baseEntity.getLorenzVec().distanceSqIgnoreY(cocoonVector)} ?: return null
        if (mob.key.baseEntity.getLorenzVec().distanceSqOnlyY(cocoonVector) > 4.0) return null
        return mob.key.name
    }

}
