package at.hannibal2.skyhanni.features.summonings

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.getNameTagWith
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SummoningSoulsName {

    // TODO repo
    private val texture =
        "ewogICJ0aW1lc3RhbXAiIDogMTYwMTQ3OTI2NjczMywKICAicHJvZmlsZUlkIiA6ICJmMzA1ZjA5NDI0NTg0ZjU" +
            "4YmEyYjY0ZjAyZDcyNDYyYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJqcm9ja2EzMyIsCiAgInNpZ25hdH" +
            "VyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgI" +
            "nVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81YWY0MDM1ZWMwZGMx" +
            "NjkxNzc4ZDVlOTU4NDAxNzAyMjdlYjllM2UyOTQzYmVhODUzOTI5Y2U5MjNjNTk4OWFkIgogICAgfQogIH0KfQ"

    private val souls = mutableMapOf<EntityArmorStand, String>()
    private val mobsLastLocation = mutableMapOf<EntityLiving, LorenzVec>()
    private val mobsName = mutableMapOf<EntityLiving, String>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        //TODO use packets instead of this
        check()
    }

    private fun check() {
        for (entity in EntityUtils.getEntities<EntityArmorStand>()) {
            if (souls.contains(entity)) continue

            if (entity.hasSkullTexture(texture)) {
                val soulLocation = entity.getLorenzVec()

                val map = mutableMapOf<EntityLiving, Double>()
                for ((mob, loc) in mobsLastLocation) {
                    val distance = loc.distance(soulLocation)
                    map[mob] = distance
                }

                val nearestMob = map.sorted().firstNotNullOfOrNull { it.key }
                if (nearestMob != null) {
                    souls[entity] = mobsName[nearestMob]!!
                }
            }
        }

        for (entity in EntityUtils.getEntities<EntityLiving>()) {
            val consumer = entity.getNameTagWith(2, "§c❤")
            if (consumer != null && !consumer.name.contains("§e0")) {
                mobsLastLocation[entity] = entity.getLorenzVec()
                mobsName[entity] = consumer.name
            }
        }

        val entityList = EntityUtils.getEntities<EntityArmorStand>()
        souls.keys.removeIf { it !in entityList }
        //TODO fix overhead!
//        mobs.keys.removeIf { it !in world.loadedEntityList }
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        for ((entity, name) in souls) {
            val vec = entity.getLorenzVec()
            event.drawString(vec.add(y = 2.5), name)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        souls.clear()
        mobsLastLocation.clear()
        mobsName.clear()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.combat.summonings.summoningSoulDisplay
}
