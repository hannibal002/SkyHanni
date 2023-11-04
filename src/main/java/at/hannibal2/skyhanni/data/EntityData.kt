package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.skyblockentities.DisplayNPC
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockMob
import at.hannibal2.skyhanni.data.skyblockentities.SummoningMob
import at.hannibal2.skyhanni.events.EntityDisplayNPCDeSpawnEvent
import at.hannibal2.skyhanni.events.EntityDisplayNPCSpawnEvent
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityRealPlayerDeSpawnEvent
import at.hannibal2.skyhanni.events.EntityRealPlayerSpawnEvent
import at.hannibal2.skyhanni.events.EntitySummoningDeSpawnEvent
import at.hannibal2.skyhanni.events.EntitySummoningSpawnEvent
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
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
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

        // Only Backup normally this should do nothing
        _currentSkyblockMobs.clear()
        _currentDisplayNPCs.clear()
        _currentRealPlayers.clear()
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
        val currentSkyblockMobs get() = _currentSkyblockMobs as Set<SkyblockMob>
        val currentDisplayNPCs get() = _currentDisplayNPCs as Set<DisplayNPC>
        val currentRealPlayers get() = _currentRealPlayers as Set<EntityPlayer>

        private val _currentSkyblockMobs = mutableSetOf<SkyblockMob>()
        private val _currentDisplayNPCs = mutableSetOf<DisplayNPC>()
        private val _currentRealPlayers = mutableSetOf<EntityPlayer>()
        private val currentEntityLiving = mutableSetOf<EntityLivingBase>()
        private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

        const val ENTITY_RENDER_RANGE_IN_BLOCKS = 80.0 // Entity DeRender after ~5 Chunks
    }

    @SubscribeEvent
    fun onTickForEntityDetection(event: LorenzTickEvent) {
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
                val e = DisplayNPC(entity)
                when (state) {
                    EntityActionState.Spawn -> EntityDisplayNPCSpawnEvent(e).postAndCatch()
                    EntityActionState.DeSpawn -> EntityDisplayNPCDeSpawnEvent(e).postAndCatch()
                }
            }

            entity.isSkyBlockMob() -> {
                val e = SkyblockMobUtils.createSkyblockEntity(entity) ?: return
                if (e is SkyblockMob) {
                    when (state) {
                        EntityActionState.Spawn -> SkyblockMobSpawnEvent(e).postAndCatch()
                        EntityActionState.DeSpawn -> {
                            if (e.isInRender()) {
                                SkyblockMobDeathEvent(e).postAndCatch()
                            } else {
                                SkyblockMobLeavingRenderEvent(e).postAndCatch()
                            }
                            SkyblockMobDeSpawnEvent(e).postAndCatch()
                        }
                    }
                } else if (e is SummoningMob) {
                    when (state) {
                        EntityActionState.Spawn -> EntitySummoningSpawnEvent(e)
                        EntityActionState.DeSpawn -> EntitySummoningDeSpawnEvent(e)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onSkyblockMobSpawnEvent(event: SkyblockMobSpawnEvent) {
        _currentSkyblockMobs.add(event.entity)
    }

    @SubscribeEvent
    fun onSkyblockMobDeSpawnEvent(event: SkyblockMobDeSpawnEvent) {
        _currentSkyblockMobs.remove(event.entity)
    }

    @SubscribeEvent
    fun onEntityDisplayNPCSpawnEvent(event: EntityDisplayNPCSpawnEvent) {
        _currentDisplayNPCs.add(event.entity)
    }

    @SubscribeEvent
    fun onEntityDisplayNPCSpawnDeEvent(event: EntityDisplayNPCDeSpawnEvent) {
        _currentDisplayNPCs.remove(event.entity)
    }

    @SubscribeEvent
    fun onEntityRealPlayerSpawnEvent(event: EntityRealPlayerSpawnEvent) {
        _currentRealPlayers.add(event.entity)
    }

    @SubscribeEvent
    fun onEntityRealPlayerDeSpawnEvent(event: EntityRealPlayerDeSpawnEvent) {
        _currentRealPlayers.remove(event.entity)
    }
}
