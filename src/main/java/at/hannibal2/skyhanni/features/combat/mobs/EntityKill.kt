package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityLivingDeathEvent
import at.hannibal2.skyhanni.events.EntityLivingSpawnEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.SkyblockMobKillEvent
import at.hannibal2.skyhanni.events.hitTrigger
import at.hannibal2.skyhanni.events.onMobHitEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.rayTraceForSkyblockMobs
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.testIfSkyBlockMob
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EntityKill {

    var playerName = " " //Just Debug

    private var mobHitList = mutableSetOf<SkyblockMobUtils.SkyblockMob>()

    val currentEntityLiving = mutableSetOf<EntityLivingBase>()
    private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

    val config get() = SkyHanniMod.feature.dev

    // TODO(fix mob exiting Renderrange to count as Death/Kill)
    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        previousEntityLiving.clear()
        previousEntityLiving.addAll(currentEntityLiving)
        currentEntityLiving.clear()
        currentEntityLiving.addAll(EntityUtils.getEntities<EntityLivingBase>().filter { testIfSkyBlockMob(it) })

        //Spawned EntityLiving
        (currentEntityLiving - previousEntityLiving).forEach { EntityLivingSpawnEvent(it).postAndCatch() }
        //Despawned EntityLiving
        (previousEntityLiving - currentEntityLiving).forEach { EntityLivingDeathEvent(it).postAndCatch() }

        if (config.mobKilldetetctionLogMobHitList) {
            if (config.mobKilldetetctionLogMobHitListId) {
                val id = mobHitList.map { it.baseEntity.entityId }
                LorenzDebug.log("Hit List: $id")
            } else {
                LorenzDebug.log("Hit List: $mobHitList")
            }
        }
    }

    @SubscribeEvent
    fun onEntityLivingSpawn(event: EntityLivingSpawnEvent) {
        //Only Debug
        val entity = event.entity
        if (entity.name == playerName) {
            val properties = entity.javaClass.declaredFields

            for (property in properties) {
                property.isAccessible = true
                val propertyName = property.name
                val propertyValue = property.get(entity)

                // Log the property name and value
                LorenzDebug.log("$propertyName: $propertyValue")
            }
        }
    }

    @SubscribeEvent
    fun onEntityLivingDeath(event: EntityLivingDeathEvent) {
        //LorenzDebug.log("Entity Death Id=${event.entity.entityId}")
        mobHitList.firstOrNull { it.baseEntity == event.entity }
            ?.let {
                LorenzDebug.log("Hi i'm not living anymore")
                SkyblockMobKillEvent(it, false).postAndCatch()
            }
    }

    @SubscribeEvent
    fun onSkyblockMobKill(event: SkyblockMobKillEvent) {
        LorenzDebug.chatAndLog("Mob Name: ${event.mob.name}")
        mobHitList.remove(event.mob)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        mobHitList.clear() //TODO Clear before the Kill trigger is activated
    }

    fun addToMobHitList(entity: Entity, trigger: hitTrigger) {
        mobHitList.firstOrNull { it.baseEntity == entity }?.let {
            if(checkIfBlacklisted(it.name,trigger)) return
            onMobHitEvent(it, trigger, false).postAndCatch()
            return
        }
        val mob = SkyblockMobUtils.createSkyblockMob(entity)
        if(checkIfBlacklisted(mob.name,trigger)) return
        mobHitList.add(mob)
        onMobHitEvent(mob, trigger, true).postAndCatch()
    }

    val magicBlacklist = setOf<String>() //Get it from repo

    private fun checkIfBlacklisted(name: String, trigger: hitTrigger) : Boolean{
        if(trigger.isMagic()){
            return magicBlacklist.contains(name)
        }
        if(trigger == hitTrigger.Cleave){
            return name.contains("Star Sentry") // Do not know if more exist
        }
        return false
    }


    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (config.skyblockMobHighlight) {
            currentEntityLiving.forEach {
                event.drawFilledBoundingBox_nea(it.entityBoundingBox.expandBlock(), LorenzColor.GREEN.toColor(), 0.5f)
            }
        }
        if (config.skyblockMobHighlightRayHit) {
            rayTraceForSkyblockMobs(Minecraft.getMinecraft().thePlayer, event.partialTicks)?.forEach {
                event.drawFilledBoundingBox_nea(it.entityBoundingBox.expandBlock(), LorenzColor.YELLOW.toColor(), 0.5f)
            }
        }
    }

}