package at.hannibal2.hanni.features.summonings

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.EntityUtils.getNameTagWith
import at.hannibal2.hanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SkullTextureHolder
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.hanni.utils.collection.TimeLimitedCache
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.item.EntityArmorStand
import kotlin.time.Duration.Companion.minutes

@HanniModule
object SummoningSoulsName {

    private val SUMMONING_SOUL_TEXTURE by lazy { SkullTextureHolder.getTexture("SUMMONING_SOUL") }
    private val souls = mutableMapOf<EntityArmorStand, String>()
    private val mobsLastLocation = TimeLimitedCache<Int, LorenzVec>(6.minutes)
    private val mobsName = TimeLimitedCache<Int, String>(6.minutes)

    @HandleEvent(HanniTickEvent::class)
    fun onTick() {
        if (!isEnabled()) return

        // TODO use packets instead of this
        check()
    }

    private fun check() {
        for (entity in EntityUtils.getEntities<EntityArmorStand>()) {
            if (entity in souls) continue

            if (!entity.wearingSkullTexture(SUMMONING_SOUL_TEXTURE)) continue
            val soulLocation = entity.getLorenzVec()

            val map = mutableMapOf<Int, Double>()
            for ((mob, loc) in mobsLastLocation) {
                val distance = loc.distance(soulLocation)
                map[mob] = distance
            }
            val nearestMob = map.sorted().firstNotNullOfOrNull { it.key }
            if (nearestMob != null) {
                souls[entity] = mobsName[nearestMob] ?: continue
            }
        }

        for (entity in EntityUtils.getEntities<EntityLiving>()) {
            val id = entity.entityId
            val consumer = entity.getNameTagWith(2, "§c❤")
            if (consumer != null && !consumer.name.contains("§e0")) {
                mobsLastLocation[id] = entity.getLorenzVec()
                mobsName[id] = consumer.name
            }
        }

        val entityList = EntityUtils.getEntities<EntityArmorStand>()
        souls.keys.removeIf { it !in entityList }
        // TODO fix overhead!
//        mobs.keys.removeIf { it !in world.loadedEntityList }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        for ((entity, name) in souls) {
            val vec = entity.getLorenzVec()
            event.drawString(vec.up(2.5), name)
        }
    }

    @HandleEvent
    fun onWorldChange() {
        souls.clear()
        mobsLastLocation.clear()
        mobsName.clear()
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && HanniMod.feature.combat.summonings.summoningSoulDisplay
}
