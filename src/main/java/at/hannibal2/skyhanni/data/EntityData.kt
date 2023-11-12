package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.EntityData.counter.addEntityName
import at.hannibal2.skyhanni.data.skyblockentities.DisplayNPC
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockBossMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockEntity
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockMob
import at.hannibal2.skyhanni.data.skyblockentities.SummoningMob
import at.hannibal2.skyhanni.data.skyblockentities.toHashPair
import at.hannibal2.skyhanni.events.EntityDisplayNPCDeSpawnEvent
import at.hannibal2.skyhanni.events.EntityDisplayNPCSpawnEvent
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityRealPlayerDeSpawnEvent
import at.hannibal2.skyhanni.events.EntityRealPlayerSpawnEvent
import at.hannibal2.skyhanni.events.EntitySummoningDeSpawnEvent
import at.hannibal2.skyhanni.events.EntitySummoningSpawnEvent
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.SkyblockMobDeSpawnEvent
import at.hannibal2.skyhanni.events.SkyblockMobDeathEvent
import at.hannibal2.skyhanni.events.SkyblockMobLeavingRenderEvent
import at.hannibal2.skyhanni.events.SkyblockMobSpawnEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isDisplayNPC
import at.hannibal2.skyhanni.utils.EntityUtils.isRealPlayer
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.LorenzUtils.put
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.isSkyBlockMob
import at.hannibal2.skyhanni.utils.getLorenzVec
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
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

private const val MAX_RETRIES = 100

private const val MAX_DISTANCE_TO_PLAYER = 22.8

class EntityData {

    private val devTrackerPath: String = "config/skyhanni/logs/mob/Tracker.txt"
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

    private val mobConfig get() = SkyHanniMod.feature.dev.mobDetection

    companion object {

        val currentSkyblockMobs get() = _currentSkyblockMobs.values.toList()
        val currentDisplayNPCs get() = _currentDisplayNPCs.values.toList()
        val currentRealPlayers get() = _currentRealPlayers.toList()
        val currentSummoningMobs get() = _currentSummoningMobs.values.toList()

        private val _currentSkyblockMobs = mutableMapOf<Int, SkyblockMob>()
        private val _currentDisplayNPCs = mutableMapOf<Int, DisplayNPC>()
        private val _currentRealPlayers = mutableSetOf<EntityPlayer>()
        private val _currentSummoningMobs = mutableMapOf<Int, SummoningMob>()
        private val currentEntityLiving = mutableSetOf<EntityLivingBase>()
        private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

        private val retries = TreeSet<RetryEntityInstancing>()

        const val ENTITY_RENDER_RANGE_IN_BLOCKS = 80.0 // Entity DeRender after ~5 Chunks

        fun removeRetry(entity: EntityLivingBase) = retries.removeIf { it.entity == entity }
    }


    @SubscribeEvent
    fun onTickForEntityDetection(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.isMod(2)) return

        handleReTries()

        previousEntityLiving.clear()
        previousEntityLiving.addAll(currentEntityLiving)
        currentEntityLiving.clear()
        if (!mobConfig.forceReset) {
            currentEntityLiving.addAll(EntityUtils.getEntities<EntityLivingBase>().filter { it !is EntityArmorStand })
        }

        (currentEntityLiving - previousEntityLiving).forEach { retry(it) }
        (previousEntityLiving - currentEntityLiving).forEach { EntityDeSpawn(it) }
    }

    private object counter {
        var retries = 0
        var outOfRangeRetries = 0
        var spawn = 0
        var deSpawn = 0
        var misses = 0
        var instancedFinish = 0
        var startedRetries = 0
        val retriesAvg get() = if (startedRetries != 0) retries / startedRetries else 0

        val EntityNames = sortedSetOf<String>()
        fun addEntityName(entity: SkyblockEntity) {
            if (EntityNames.contains(entity.name)) return
            val type = when (entity) {
                is DisplayNPC -> "DNPC "
                is SkyblockMob -> "SMOB "
                else -> "NONE "
            }
            EntityNames.add(type + entity.name)
        }

        fun reset() {
            retries = 0
            spawn = 0
            deSpawn = 0
            misses = 0
            instancedFinish = 0
            startedRetries = 0
            EntityNames.clear()
        }
    }

    @SubscribeEvent
    fun onIslandJoin(event: HypixelJoinEvent) {
        var foundNameList = false
        try {
            FileInputStream(devTrackerPath).apply {
                reader().forEachLine {
                    if (!foundNameList) {
                        when {
                            it.contains("Retries") -> counter.retries = trackerLineToCounter(it)
                            it.contains("OutOfRangeRetires") -> counter.outOfRangeRetries = trackerLineToCounter(it)
                            it.contains("Spawn") -> counter.spawn = trackerLineToCounter(it)
                            it.contains("Despawn") -> counter.deSpawn = trackerLineToCounter(it)
                            it.contains("InstancedFinish") -> counter.instancedFinish = trackerLineToCounter(it)
                            it.contains("StartedRetries") -> counter.startedRetries = trackerLineToCounter(it)
                            it.contains("Misses") -> counter.misses = trackerLineToCounter(it)
                            it.contains("Name List:") -> foundNameList = true
                        }
                    } else {
                        counter.EntityNames.add(it)
                    }
                }
            }.apply { close() }
        } catch (e: FileNotFoundException) {
            Files.createDirectories(Paths.get(devTrackerPath.substring(0, devTrackerPath.indexOfLast { it == '/' })))
            return
        } catch (e: SecurityException) {
            return
        }
    }

    private fun trackerLineToCounter(it: String) = it.substring(it.indexOf(":") + 2, it.length).toInt()

    @SubscribeEvent
    fun onExit(event: IslandChangeEvent) {
        FileOutputStream(devTrackerPath).apply {
            write(
                (buildString {
                    append("Retries: ${counter.retries}\n")
                    append("OutOfRangeRetires: ${counter.outOfRangeRetries}\n")
                    append("Spawn: ${counter.spawn}\n")
                    append("Despawn: ${counter.deSpawn}\n")
                    append("InstancedFinish: ${counter.instancedFinish}\n")
                    append("StartedRetries: ${counter.startedRetries}\n")
                    append("RetiresAVG: ${counter.retriesAvg}\n")
                    append("Misses: ${counter.misses}\n")
                    append("\nName List:\n")
                }).toByteArray()
            )
            write(counter.EntityNames.joinToString("\n").toByteArray())
            close()
        }
        // counter.reset()
    }

    private fun EntitySpawn(entity: EntityLivingBase): Boolean {
        counter.spawn++
        when {
            entity is EntityPlayer && entity.isRealPlayer() -> EntityRealPlayerSpawnEvent(entity).postAndCatch()
            entity.isDisplayNPC() -> EntityDisplayNPCSpawnEvent(DisplayNPC(entity)).postAndCatch()
            entity.isSkyBlockMob() -> {
                if (RiftAPI.inRift()) return true
                val it = SkyblockMobUtils.createSkyblockEntity(entity) ?: return false
                if (it is SummoningMob) {
                    EntitySummoningSpawnEvent(it).postAndCatch()
                } else if (it is SkyblockMob) {
                    SkyblockMobSpawnEvent(it).postAndCatch()
                }
            }
        }
        return true
    }

    private fun EntityDeSpawn(entity: EntityLivingBase) {
        counter.deSpawn++
        when {
            entity is EntityPlayer && entity.isRealPlayer() -> EntityRealPlayerDeSpawnEvent(entity).postAndCatch()
            entity.isDisplayNPC() -> EntityDisplayNPCDeSpawnEvent(DisplayNPC(entity)).postAndCatch()
            entity.isSkyBlockMob() -> {
                if (RiftAPI.inRift()) return
                _currentSummoningMobs[entity.hashCode()]?.let { EntitySummoningDeSpawnEvent(it).postAndCatch() }
                    ?: _currentSkyblockMobs[entity.hashCode()]?.let {
                        if (it.isInRender()) {
                            SkyblockMobDeathEvent(it).postAndCatch()
                        } else {
                            SkyblockMobLeavingRenderEvent(it).postAndCatch()
                        }
                        SkyblockMobDeSpawnEvent(it).postAndCatch()
                    } ?: removeRetry(entity)
            }
        }
    }

    private fun retry(entity: EntityLivingBase) =
        retries.add(RetryEntityInstancing(entity, 0)).also { counter.startedRetries++ }

    private class RetryEntityInstancing(val entity: EntityLivingBase, var times: Int) : Comparable<RetryEntityInstancing> {
        override fun hashCode() = entity.hashCode()
        override fun compareTo(other: RetryEntityInstancing) = this.hashCode() - other.hashCode()
        override fun equals(other: Any?) = (other as? EntityLivingBase) == entity
    }

    private fun handleReTries() {
        val iterator = retries.iterator()
        while (iterator.hasNext()) {
            val retry = iterator.next()
            val entity = retry.entity
            if (entity.getLorenzVec()
                    .distanceChebyshevIgnoreY(LocationUtils.playerLocation()) > MAX_DISTANCE_TO_PLAYER
            ) {
                counter.outOfRangeRetries++
                continue
            }
            counter.retries++
            if (retry.times > MAX_RETRIES) {
                LorenzDebug.log(
                    "I missed. Distance: ${
                        entity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation())
                    } , ${
                        entity.getLorenzVec().subtract(LocationUtils.playerLocation())
                    }"
                )
                counter.misses++
                iterator.remove()
                continue
            }
            if (!EntitySpawn(entity)) {
                retry.times++
                continue
            }
            iterator.remove()
        }
    }


    @SubscribeEvent
    fun onSkyblockMobSpawnEvent(event: SkyblockMobSpawnEvent) {
        _currentSkyblockMobs.put(event.entity.toHashPair())
        addEntityName(event.entity)
    }

    @SubscribeEvent
    fun onSkyblockMobDeSpawnEvent(event: SkyblockMobDeSpawnEvent) {
        _currentSkyblockMobs.remove(event.entity.hashCode())
    }

    @SubscribeEvent
    fun onEntityDisplayNPCSpawnEvent(event: EntityDisplayNPCSpawnEvent) {
        _currentDisplayNPCs.put(event.entity.toHashPair())
        addEntityName(event.entity)
    }

    @SubscribeEvent
    fun onEntityDisplayNPCSpawnDeEvent(event: EntityDisplayNPCDeSpawnEvent) {
        _currentDisplayNPCs.remove(event.entity.hashCode())
    }

    @SubscribeEvent
    fun onEntityRealPlayerSpawnEvent(event: EntityRealPlayerSpawnEvent) {
        _currentRealPlayers.add(event.entity)
    }

    @SubscribeEvent
    fun onEntityRealPlayerDeSpawnEvent(event: EntityRealPlayerDeSpawnEvent) {
        _currentRealPlayers.remove(event.entity)
    }

    @SubscribeEvent
    fun onEntitySummonSpawnEvent(event: EntitySummoningSpawnEvent) {
        _currentSummoningMobs.put(event.entity.toHashPair())
    }

    @SubscribeEvent
    fun onEntitySummonDeSpawnEvent(event: EntitySummoningDeSpawnEvent) {
        _currentSummoningMobs.remove(event.entity.hashCode())
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (mobConfig.skyblockMobHighlight) {
            currentSkyblockMobs.forEach {
                val color = if (it is SkyblockBossMob) LorenzColor.DARK_GREEN else LorenzColor.GREEN
                event.drawFilledBoundingBox_nea(it.baseEntity.entityBoundingBox.expandBlock(), color.toColor(), 0.3f)
            }
        }
        if (mobConfig.displayNPCHighlight) {
            currentDisplayNPCs.forEach {
                event.drawFilledBoundingBox_nea(it.baseEntity.entityBoundingBox.expandBlock(), LorenzColor.RED.toColor(), 0.3f)
            }
        }
        if (mobConfig.realPlayerHighlight) {
            currentRealPlayers.filterNot { it is EntityPlayerSP }.forEach {
                event.drawFilledBoundingBox_nea(it.entityBoundingBox.expandBlock(), LorenzColor.BLUE.toColor(), 0.3f)
            }
        }
        if (mobConfig.summonHighlight) {
            currentSummoningMobs.forEach {
                event.drawFilledBoundingBox_nea(it.baseEntity.entityBoundingBox.expandBlock(), LorenzColor.YELLOW.toColor(), 0.3f)
            }
        }
    }
}
