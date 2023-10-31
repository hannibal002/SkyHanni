package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.EntityDisplayNPCDeSpawnEvent
import at.hannibal2.skyhanni.events.EntityDisplayNPCSpawnEvent
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityRealPlayerDeSpawnEvent
import at.hannibal2.skyhanni.events.EntityRealPlayerSpawnEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.SkyblockMobDeSpawnEvent
import at.hannibal2.skyhanni.events.SkyblockMobDeathEvent
import at.hannibal2.skyhanni.events.SkyblockMobLeavingRenderEvent
import at.hannibal2.skyhanni.events.SkyblockMobSpawnEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isDisplayNPC
import at.hannibal2.skyhanni.utils.EntityUtils.isRealPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.isSkyBlockMob
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EntityData {

    private val maxHealthMap = mutableMapOf<EntityLivingBase, Int>()

    @SubscribeEvent
    fun onTickForHealth(event: LorenzTickEvent) {
        for (entity in EntityUtils.getEntities<EntityLivingBase>()) {
            val maxHealth = entity.baseMaxHealth
            val oldMaxHealth = maxHealthMap.getOrDefault(entity, -1)
            if (oldMaxHealth != maxHealth) {
                maxHealthMap[entity] = maxHealth
                EntityMaxHealthUpdateEvent(entity, maxHealth.derpy()).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        maxHealthMap.clear()
    }

    @SubscribeEvent
    fun onHealthUpdatePacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        if (packet !is S1CPacketEntityMetadata) return

        val watchableObjects = packet.func_149376_c() ?: return
        val entityId = packet.entityId

        val entity = EntityUtils.getEntityByID(entityId) ?: return
        if (entity is EntityArmorStand) return
        if (entity is EntityXPOrb) return
        if (entity is EntityItem) return
        if (entity is EntityItemFrame) return

        if (entity is EntityOtherPlayerMP) return
        if (entity is EntityPlayerSP) return

        for (watchableObject in watchableObjects) {

            val dataValueId = watchableObject.dataValueId
            val any = watchableObject.`object`
            if (dataValueId != 6) continue

            val health = (any as Float).toInt()

            if (entity is EntityWither && health == 300 && entityId < 0) {
                return
            }

            if (entity is EntityLivingBase) {
                EntityHealthUpdateEvent(entity, health.derpy()).postAndCatch()
            }
        }
    }

    companion object {
        val currentSkyblockMobs = mutableSetOf<EntityLivingBase>()
        private val currentEntityLiving = mutableSetOf<EntityLivingBase>()
        private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

        const val ENTITY_RENDER_RANGE_IN_BLOCKS = 80.0 //Entity Derender after ~5 Chunks
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        previousEntityLiving.clear()
        previousEntityLiving.addAll(currentEntityLiving)
        currentEntityLiving.clear()
        currentEntityLiving.addAll(EntityUtils.getEntities<EntityLivingBase>().filter { it !is EntityArmorStand })

        (currentEntityLiving - previousEntityLiving).forEach { handelEntityInstancing(it, EntityActionState.Spawn) }
        (previousEntityLiving - currentEntityLiving).forEach { handelEntityInstancing(it, EntityActionState.DeSpawn) }
    }

    private enum class EntityActionState {
        Spawn, DeSpawn
    }

    private fun handelEntityInstancing(entity: EntityLivingBase, state: EntityActionState) {
        when {
            entity is EntityPlayer && entity.isRealPlayer() -> {
                when (state) {
                    EntityActionState.Spawn -> EntityRealPlayerSpawnEvent(entity).postAndCatch()
                    EntityActionState.DeSpawn -> EntityRealPlayerDeSpawnEvent(entity).postAndCatch()
                }
            }

            entity.isDisplayNPC() -> {
                when (state) {
                    EntityActionState.Spawn -> EntityDisplayNPCSpawnEvent(entity).postAndCatch()
                    EntityActionState.DeSpawn -> EntityDisplayNPCDeSpawnEvent(entity).postAndCatch()
                }
            }

            entity.isSkyBlockMob() -> {
                when (state) {
                    EntityActionState.Spawn -> SkyblockMobSpawnEvent(entity).postAndCatch()
                    EntityActionState.DeSpawn -> {
                        if (entity.distanceToPlayer() < ENTITY_RENDER_RANGE_IN_BLOCKS) {
                            SkyblockMobDeathEvent(entity).postAndCatch()
                        } else {
                            SkyblockMobLeavingRenderEvent(entity).postAndCatch()
                        }
                        SkyblockMobDeSpawnEvent(entity).postAndCatch()
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityLivingSpawn(event: SkyblockMobSpawnEvent) {
        currentSkyblockMobs.add(event.entity)
    }

    @SubscribeEvent
    fun onEntityLivingDeSpawn(event: SkyblockMobDeSpawnEvent) {
        currentSkyblockMobs.remove(event.entity)
    }
}
