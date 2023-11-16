package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MobFilter.isDisplayNPC
import at.hannibal2.skyhanni.data.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.MobFilter.isSkyBlockMob
import at.hannibal2.skyhanni.data.skyblockentities.DisplayNPC
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockBossMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockEntity
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockMob
import at.hannibal2.skyhanni.data.skyblockentities.SummingOrSkyblockMob
import at.hannibal2.skyhanni.data.skyblockentities.SummoningMob
import at.hannibal2.skyhanni.data.skyblockentities.toPair
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
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.LorenzUtils.put
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.getLorenzVec
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
import java.io.File
import java.io.IOException
import java.util.*

private const val MAX_RETRIES = 100

private const val MAX_DISTANCE_TO_PLAYER = 22.8

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
        currentSkyblockMobs.clear()
        currentDisplayNPCs.clear()
        currentRealPlayers.clear()
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

        val currentRealPlayers = mutableSetOf<EntityPlayer>()
        val currentDisplayNPCs get() = currentDisplayNPCsMap.values
        val currentSkyblockMobs = mutableSetOf<SkyblockMob>()
        val currentSummoningMobs = mutableSetOf<SummoningMob>()

        val currentDisplayNPCsMap = mutableMapOf<EntityLivingBase, DisplayNPC>()
        val currentSkyblockMobsMap = mutableMapOf<EntityLivingBase, SkyblockMob>()
        val currentSummoningMobsMap = mutableMapOf<EntityLivingBase, SummoningMob>()
        private val currentEntityLiving = mutableSetOf<EntityLivingBase>()
        private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

        fun getSummonOrSkyblockMob(entity: EntityLivingBase) =
            currentSkyblockMobsMap[entity] ?: currentSummoningMobsMap[entity]

        fun putSummonOrSkyblockMob(entity: EntityLivingBase, mob: SummingOrSkyblockMob) =
            if (mob is SkyblockMob) currentSkyblockMobsMap[entity] =
                mob else if (mob is SummoningMob) currentSummoningMobsMap[entity] = mob else {
            }

        fun putAllSummonOrSkyblockMob(entity: Collection<EntityLivingBase>, mob: SummingOrSkyblockMob) =
            if (mob is SkyblockMob) currentSkyblockMobsMap.putAll(entity.associateWith { mob }) else if (mob is SummoningMob) currentSummoningMobsMap.putAll(entity.associateWith { mob }) else {
            }

        val retries = TreeSet<RetryEntityInstancing>()

        const val ENTITY_RENDER_RANGE_IN_BLOCKS = 80.0 // Entity DeRender after ~5 Chunks

        var externRemoveOfRetryAmount = 0
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

    private fun EntitySpawn(entity: EntityLivingBase): Boolean {
        devTracker.data.spawn++
        when {
            entity is EntityPlayer && entity.isRealPlayer() -> EntityRealPlayerSpawnEvent(entity).postAndCatch()
            entity.isDisplayNPC() -> return createDisplayNPC(entity)
            entity.isSkyBlockMob() -> {
                if (islandException()) return true
                val it = MobFilter.createSkyblockEntity(entity) ?: return false
                if (it is SummoningMob) {
                    EntitySummoningSpawnEvent(it).postAndCatch()
                } else if (it is SkyblockMob) {
                    SkyblockMobSpawnEvent(it).postAndCatch()
                }
            }
        }
        return true
    }

    private fun createDisplayNPC(entity: EntityLivingBase): Boolean = DisplayNPC(entity).let { npc ->
        if (npc.name.isEmpty()) false else EntityDisplayNPCSpawnEvent(npc).postAndCatch().let { true }
    }


    private fun islandException(): Boolean = when (LorenzUtils.skyBlockIsland) {
        IslandType.THE_RIFT -> false
        IslandType.PRIVATE_ISLAND_GUEST -> true
        else -> false
    }

    private fun EntityDeSpawn(entity: EntityLivingBase) {
        devTracker.data.deSpawn++
        when {
            entity is EntityPlayer && entity.isRealPlayer() -> EntityRealPlayerDeSpawnEvent(entity).postAndCatch()
            entity.isDisplayNPC() -> currentDisplayNPCsMap[entity]?.let { EntityDisplayNPCDeSpawnEvent(it).postAndCatch() }
                ?: removeRetry(entity)

            entity.isSkyBlockMob() -> {
                if (islandException()) return
                currentSummoningMobsMap[entity]?.let { EntitySummoningDeSpawnEvent(it).postAndCatch() }
                    ?: currentSkyblockMobsMap[entity]?.let {
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
        retries.add(RetryEntityInstancing(entity, 0)).also { devTracker.data.startedRetries++ }

    private fun removeRetry(entity: EntityLivingBase) = retries.removeIf { it.entity == entity }

    class RetryEntityInstancing(val entity: EntityLivingBase, var times: Int) : Comparable<RetryEntityInstancing> {
        override fun hashCode() = entity.hashCode()
        override fun compareTo(other: RetryEntityInstancing) = this.hashCode() - other.hashCode()
        override fun equals(other: Any?) = (other as? EntityLivingBase) == entity
    }

    private fun handleReTries() {
        val iterator = retries.iterator()
        while (iterator.hasNext()) {
            val retry = iterator.next()
            if (externRemoveOfRetryAmount > 0) {
                iterator.remove()
                externRemoveOfRetryAmount--
                continue
            }
            val entity = retry.entity
            if (entity.getLorenzVec()
                    .distanceChebyshevIgnoreY(LocationUtils.playerLocation()) > MAX_DISTANCE_TO_PLAYER
            ) {
                devTracker.data.outOfRangeRetries++
                continue
            }
            devTracker.data.retries++
            if (retry.times > MAX_RETRIES) {
                LorenzDebug.log(
                    "I missed. Distance: ${
                        entity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation())
                    } , ${
                        entity.getLorenzVec().subtract(LocationUtils.playerLocation())
                    }"
                )
                devTracker.data.misses++
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
        currentSkyblockMobs.add(event.entity)
        currentSkyblockMobsMap.put(event.entity.toPair())
        event.entity.extraEntities?.filter { it !is EntityArmorStand }?.associateWith { event.entity }
            ?.also { currentSkyblockMobsMap.putAll(it) }
        devTracker.addEntityName(event.entity)
    }

    @SubscribeEvent
    fun onSkyblockMobDeSpawnEvent(event: SkyblockMobDeSpawnEvent) {
        val entity = event.entity
        entity.extraEntities?.forEach { currentSkyblockMobsMap.remove(it);currentEntityLiving.remove(it) }
        currentSkyblockMobsMap.remove(entity.baseEntity)
        currentSkyblockMobs.remove(entity)

    }

    @SubscribeEvent
    fun onEntitySummonSpawnEvent(event: EntitySummoningSpawnEvent) {
        currentSummoningMobs.add(event.entity)
        currentSummoningMobsMap.put(event.entity.toPair())
        event.entity.extraEntities?.filter { it !is EntityArmorStand }?.associateWith { event.entity }
            ?.also { currentSummoningMobsMap.putAll(it) }
        devTracker.addEntityName(event.entity)
    }

    @SubscribeEvent
    fun onEntitySummonDeSpawnEvent(event: EntitySummoningDeSpawnEvent) {
        val entity = event.entity
        entity.extraEntities?.forEach { currentSummoningMobsMap.remove(it);currentEntityLiving.remove(it) }
        currentSummoningMobsMap.remove(entity.baseEntity)
        currentSummoningMobs.remove(entity)
    }

    @SubscribeEvent
    fun onEntityDisplayNPCSpawnEvent(event: EntityDisplayNPCSpawnEvent) {
        currentDisplayNPCsMap.put(event.entity.toPair())
        devTracker.addEntityName(event.entity)
    }

    @SubscribeEvent
    fun onEntityDisplayNPCSpawnDeEvent(event: EntityDisplayNPCDeSpawnEvent) {
        currentDisplayNPCsMap.remove(event.entity.baseEntity)
    }

    @SubscribeEvent
    fun onEntityRealPlayerSpawnEvent(event: EntityRealPlayerSpawnEvent) {
        currentRealPlayers.add(event.entity)
    }

    @SubscribeEvent
    fun onEntityRealPlayerDeSpawnEvent(event: EntityRealPlayerDeSpawnEvent) {
        currentRealPlayers.remove(event.entity)
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (mobConfig.skyblockMobHighlight) {
            currentSkyblockMobs.forEach {
                val color = if (it is SkyblockBossMob) LorenzColor.DARK_GREEN else LorenzColor.GREEN
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), color.toColor(), 0.3f)
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
        if (mobConfig.skyblockMobShowName) {
            currentSkyblockMobs.forEach {
                event.drawDynamicText(
                    it.baseEntity.getLorenzVec(), "§5" + it.name, 0.5, ignoreBlocks = false, smallestDistanceVew = 10.0, yOff = 1.5f
                )
            }
        }
        if (mobConfig.displayNPCShowName) {
            currentDisplayNPCs.forEach {
                event.drawDynamicText(
                    it.baseEntity.getLorenzVec(), "§d" + it.name, 0.5, ignoreBlocks = false, smallestDistanceVew = 10.0, yOff = 1.5f
                )
            }
        }
    }

    private object devTracker {

        const val FILE_NAME: String = "config/skyhanni/logs/mob/Tracker.txt"

        val data = Data()

        class Data {
            var retries = 0
            var outOfRangeRetries = 0
            var spawn = 0
            var deSpawn = 0
            var misses = 0
            var instancedFinish = 0
            var startedRetries = 0
            val retriesAvg: Int
                get() = if (startedRetries != 0) retries / startedRetries else 0

            val entityNames = sortedSetOf<String>()

            fun reset() {
                retries = 0
                spawn = 0
                outOfRangeRetries = 0
                deSpawn = 0
                misses = 0
                instancedFinish = 0
                startedRetries = 0
                entityNames.clear()
            }
        }

        fun addEntityName(entity: SkyblockEntity) {
            if (data.entityNames.contains(entity.name)) return
            val type = when (entity) {
                is DisplayNPC -> "DNPC "
                is SkyblockMob -> "SMOB "
                else -> "NONE "
            }
            data.entityNames.add(type + entity.name)
        }

        fun saveToFile() {
            try {
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(data)

                // Write the JSON data to the file
                File(FILE_NAME).writeText(json)
            } catch (e: IOException) {
                LorenzDebug.log("Error saving data to file: ${e.message}")
            }
        }

        fun loadFromFile() {
            try {
                // Create the parent directory and its ancestors recursively if they don't exist
                val parentDir = File(FILE_NAME).parentFile
                parentDir.mkdirs()

                // Load data from the file
                if (File(FILE_NAME).exists()) {
                    val gson = Gson()
                    val json = File(FILE_NAME).readText()
                    val loadedData = gson.fromJson(json, Data::class.java)
                    // Update the existing data with loaded data
                    data.retries = loadedData.retries
                    data.outOfRangeRetries = loadedData.outOfRangeRetries
                    data.spawn = loadedData.spawn
                    data.deSpawn = loadedData.deSpawn
                    data.misses = loadedData.misses
                    data.instancedFinish = loadedData.instancedFinish
                    data.startedRetries = loadedData.startedRetries
                    data.entityNames.clear()
                    data.entityNames.addAll(loadedData.entityNames)
                }
            } catch (e: IOException) {
                LorenzDebug.log("Error loading data from file: ${e.message}")
            }
        }

        override fun toString(): String {
            return "TrackerData(retries=${data.retries}, outOfRangeRetries=${data.outOfRangeRetries}, " + "spawn=${data.spawn}, deSpawn=${data.deSpawn}, misses=${data.misses}, " + "instancedFinish=${data.instancedFinish}, startedRetries=${data.startedRetries}, " + "retriesAvg=${data.retriesAvg}, entityNames=${data.entityNames})"
        }

    }

    @SubscribeEvent
    fun onIslandJoin(event: HypixelJoinEvent) {
        devTracker.loadFromFile()
    }

    @SubscribeEvent
    fun onExit(event: IslandChangeEvent) {
        devTracker.saveToFile()
        // counter.reset()
    }
}
