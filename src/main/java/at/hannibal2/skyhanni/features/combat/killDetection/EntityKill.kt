package at.hannibal2.skyhanni.features.combat.killDetection

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityLivingDeathEvent
import at.hannibal2.skyhanni.events.EntityLivingSpawnEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.SkyblockMobKillEvent
import at.hannibal2.skyhanni.events.onMobHitEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.isSkyBlockMob
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.rayTraceForSkyblockMobs
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EntityKill {

    private var mobHitList = mutableSetOf<SkyblockMobUtils.SkyblockMob>()

    val currentEntityLiving = mutableSetOf<EntityLivingBase>()
    private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

    val config get() = SkyHanniMod.feature.dev.mobKillDetection

    const val ENTITY_RENDER_RANGE_IN_BLOCKS = 80.0 //Entity Derender after ~5 Chunks

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        previousEntityLiving.clear()
        previousEntityLiving.addAll(currentEntityLiving)
        currentEntityLiving.clear()
        currentEntityLiving.addAll(EntityUtils.getEntities<EntityLivingBase>().filter { it.isSkyBlockMob() })

        //Spawned EntityLiving
        (currentEntityLiving - previousEntityLiving).forEach { EntityLivingSpawnEvent(it).postAndCatch() }
        //Despawned EntityLiving
        (previousEntityLiving - currentEntityLiving).filter {
            it.distanceToPlayer() < ENTITY_RENDER_RANGE_IN_BLOCKS
        }.forEach { EntityLivingDeathEvent(it).postAndCatch() }

        if (config.LogMobHitList) {
            if (config.LogMobHitListId) {
                val id = mobHitList.map { it.baseEntity.entityId }
                LorenzDebug.log("Hit List: $id")
            } else {
                LorenzDebug.log("Hit List: $mobHitList")
            }
        }
    }

    @SubscribeEvent
    fun onEntityLivingSpawn(event: EntityLivingSpawnEvent) {

    }

    @SubscribeEvent
    fun onEntityLivingDeath(event: EntityLivingDeathEvent) {
        mobHitList.firstOrNull { it == event.entity }
            ?.let {
                SkyblockMobKillEvent(it, false).postAndCatch()
            }
    }

    @SubscribeEvent
    fun onSkyblockMobKill(event: SkyblockMobKillEvent) {
        if(config.ShowNameOfKilledMob) LorenzDebug.chatAndLog("Mob Name: ${event.mob.name}")
        mobHitList.remove(event.mob)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        mobHitList.clear() //TODO Clear before the Kill trigger is activated
    }

    fun addToMobHitList(entity: Entity, trigger: hitTrigger) {
        mobHitList.firstOrNull { it == entity }?.let {
            if (checkIfBlacklisted(it.name, trigger)) return
            onMobHitEvent(it, trigger, false).postAndCatch()
            return
        }
        val mob = SkyblockMobUtils.createSkyblockMob(entity)
        if (checkIfBlacklisted(mob.name, trigger)) return
        mobHitList.add(mob)
        onMobHitEvent(mob, trigger, true).postAndCatch()
    }

    val magicBlacklist = setOf<String>() //Get it from repo

    private fun checkIfBlacklisted(name: String, trigger: hitTrigger): Boolean { //TODO complete the Blacklist
        if (trigger.isMagic()) {
            return magicBlacklist.contains(name)
        }
        if (trigger == hitTrigger.Cleave) {
            return name.contains("Star Sentry") // Do not know if more exist
        }
        return false
    }


    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (config.skyblockMobHighlight) {
            currentEntityLiving.forEach {
                event.drawFilledBoundingBox_nea(it.entityBoundingBox.expandBlock(), LorenzColor.GREEN.toColor(), 0.3f)
            }
        }
        if (config.skyblockMobHitHighlight) {
            mobHitList.forEach {
                event.drawFilledBoundingBox_nea(
                    it.baseEntity.entityBoundingBox.expandBlock(), LorenzColor.LIGHT_PURPLE.toColor
                        (), 0.6f
                )
            }
        }
        if (config.skyblockMobHighlightRayHit) {
            rayTraceForSkyblockMobs(Minecraft.getMinecraft().thePlayer, event.partialTicks)?.forEach {
                event.drawFilledBoundingBox_nea(it.entityBoundingBox.expandBlock(), LorenzColor.YELLOW.toColor(), 0.5f)
            }
        }
    }

}
