package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.testIfSkyBlockMob
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EntityKill {

    var playerName = " " //Just Debug

    private var mobHitList = mutableSetOf<SkyblockMobUtils.SkyblockMob>()

    val currentEntityLiving = mutableSetOf<EntityLivingBase>()
    private val previousEntityLiving = mutableSetOf<EntityLivingBase>()


    val config get() = SkyHanniMod.feature.dev

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

        if(config.mobKilldetetctionLogMobHitList) {
            if(config.mobKilldetetctionLogMobHitListId) {
                val id = mobHitList.map{it.baseEntity.entityId}
                LorenzDebug.log("Hit List: $id")
            }else{
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
                SkyblockMobKillEvent(it, false).postAndCatch() }
    }

    @SubscribeEvent
    fun onSkyblockMobKill(event: SkyblockMobKillEvent) {
        LorenzDebug.chatAndLog("Mob Name: ${event.mob.name}")
        mobHitList.remove(event.mob)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        //Backup to avoid Memory Leak (if any exists)
        mobHitList.clear()
    }

    private fun addToMobHitList(mobId: Int) {
        EntityUtils.getEntityById(mobId)?.let {
            addToMobHitList(it)
        }
    }

    private fun addToMobHitList(entity: Entity) {
        if (!testIfSkyBlockMob(entity)) return
        mobHitList.add(SkyblockMobUtils.SkyblockMob(entity))
    }

    fun checkAndAddToMobHitList(mobId: Int) {
        EntityUtils.getEntityById(mobId)?.let {
            checkAndAddToMobHitList(it)
        }
    }

    fun checkAndAddToMobHitList(entity: Entity) {
        if (!isInMobHitList(entity)) {
            addToMobHitList(entity)
        }
    }

    private fun isInMobHitList(entity: Entity): Boolean {
        return mobHitList.any { it.baseEntity == entity }
    }

    @SubscribeEvent
    fun onWorldLastEvent(event: RenderWorldLastEvent){
        if(!config.skyblockMobHighlight) return
        currentEntityLiving.forEach{
            event.drawFilledBoundingBox_nea(it.entityBoundingBox.expandBlock(),LorenzColor.GREEN.toColor(),0.5f)
        }
    }

}

