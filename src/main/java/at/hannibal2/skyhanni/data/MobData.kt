package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MobFilter.illegalDisplayNPCArmorStandNames
import at.hannibal2.skyhanni.data.MobFilter.isDisplayNPC
import at.hannibal2.skyhanni.data.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.MobFilter.isSkyBlockMob
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.MobUtils.rayTraceForSkyblockMob
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.getLorenzVec
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S37PacketStatistics
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

private const val MAX_RETRIES = 100

private const val MAX_DISTANCE_TO_PLAYER = 22.0

class MobData {
    private val mobDebugConfig get() = SkyHanniMod.feature.dev.mobDebug.mobDetection
    private val forceReset get() = SkyHanniMod.feature.dev.mobDebug.forceReset

    class MobSet() : HashSet<Mob>() {
        val entityList get() = this.flatMap { listOf(it.baseEntity) + (it.extraEntities ?: emptyList()) }
    }

    companion object {

        val players = MobSet()
        val displayNPCs = MobSet()
        val skyblockMobs = MobSet()
        val summoningMobs = MobSet()
        val currentMobs = MobSet()

        val entityToMob = mutableMapOf<EntityLivingBase, Mob>()

        private val currentEntityLiving = mutableSetOf<EntityLivingBase>()
        private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

        val retries = TreeSet<RetryEntityInstancing>()

        const val ENTITY_RENDER_RANGE_IN_BLOCKS = 80.0 // Entity DeRender after ~5 Chunks

        var externRemoveOfRetryAmount = 0
    }

    var gotReseted = true

    private fun mobDetectionReset() {
        if (!gotReseted) {
            currentMobs.map {
                when (it.mobType) {
                    Mob.Type.DisplayNPC -> MobEvent.DeSpawn.DisplayNPC(it)
                    Mob.Type.Summon -> MobEvent.DeSpawn.Summon(it)
                    Mob.Type.Basic, Mob.Type.Dungeon, Mob.Type.Boss, Mob.Type.Slayer -> MobEvent.DeSpawn.SkyblockMob(it)
                    Mob.Type.Player -> MobEvent.DeSpawn.Player(it)
                    Mob.Type.Projectile -> MobEvent.DeSpawn.Projectile(it)
                    Mob.Type.Special -> MobEvent.DeSpawn.Special(it)
                }
            }.forEach { it.postAndCatch() }
            gotReseted = true
        }
    }

    enum class Result {
        Found, NotYetFound, Illegal
    }

    class MobResult(val result: Result, val mob: Mob?)

    @SubscribeEvent
    fun onTickForEntityDetection(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) mobDetectionReset().run { return }
        if (event.isMod(2)) return
        gotReseted = false

        makeEntityUpdate()

        handleRetries()

        previousEntityLiving.clear()
        previousEntityLiving.addAll(currentEntityLiving)
        currentEntityLiving.clear()
        currentEntityLiving.addAll(EntityUtils.getEntities<EntityLivingBase>().filter { it !is EntityArmorStand })

        if (forceReset) {
            currentEntityLiving.clear()
        }

        (currentEntityLiving - previousEntityLiving).forEach { retry(it) }
        (previousEntityLiving - currentEntityLiving).forEach { EntityDeSpawn(it) }

        if (forceReset) {
            mobDetectionReset()
        }
    }

    private val entitiesThatRequireUpdatePacket = LinkedBlockingQueue<Int>()
    private val entitiesThatRequireUpdate = mutableSetOf<Int>()

    private fun makeEntityUpdate() {
        entitiesThatRequireUpdate.iterator().let { iter ->
            while (iter.hasNext()) {
                if (handleEntityUpdate(iter.next())) iter.remove()
            }
        }
        while (entitiesThatRequireUpdatePacket.isNotEmpty()) {
            entitiesThatRequireUpdate.add(entitiesThatRequireUpdatePacket.take())
        }
    }

    private fun EntitySpawn(entity: EntityLivingBase): Boolean {
        devTracker.data.spawn++
        when {
            entity is EntityPlayer && entity.isRealPlayer() -> MobEvent.Spawn.Player(MobFactories.player(entity))
                .postAndCatch()

            entity.isDisplayNPC() -> return createDisplayNPC(entity)
            entity.isSkyBlockMob() -> {
                if (islandException()) return true
                val it = MobFilter.createSkyblockEntity(entity)
                if (it.result == Result.NotYetFound) return false
                if (it.result == Result.Illegal) return true
                if (it.mob == null) throw IllegalStateException("Mob is null even though result is Found")
                when (it.mob.mobType) {
                    Mob.Type.Summon -> MobEvent.Spawn.Summon(it.mob).postAndCatch()

                    Mob.Type.Basic, Mob.Type.Dungeon, Mob.Type.Boss, Mob.Type.Slayer -> MobEvent.Spawn.SkyblockMob(it.mob)
                        .postAndCatch()

                    Mob.Type.Special -> MobEvent.Spawn.Special(it.mob).postAndCatch()
                    Mob.Type.Projectile -> MobEvent.Spawn.Projectile(it.mob).postAndCatch()
                    else -> {}
                }
            }
        }
        return true
    }

    private fun createDisplayNPC(entity: EntityLivingBase): Boolean =
        MobUtils.getArmorStandByRangeAll(entity, 2.0).firstOrNull { armorStand ->
            !illegalDisplayNPCArmorStandNames.any { armorStand.name.startsWith(it) } && !armorStand.isDefaultValue()
        }?.let { armorStand ->
            MobEvent.Spawn.DisplayNPC(MobFactories.displayNPC(entity, armorStand)).postAndCatch().also { true }
        } ?: false


    private fun islandException(): Boolean = when (LorenzUtils.skyBlockIsland) {
        IslandType.THE_RIFT -> false
        IslandType.PRIVATE_ISLAND_GUEST -> true
        else -> false
    }

    private fun EntityDeSpawn(entity: EntityLivingBase) {
        devTracker.data.deSpawn++
        entityToMob[entity]?.let {
            when (it.mobType) {
                Mob.Type.Player -> MobEvent.DeSpawn.Player(it)
                Mob.Type.Summon -> MobEvent.DeSpawn.Summon(it)
                Mob.Type.Special -> MobEvent.DeSpawn.Special(it)
                Mob.Type.Projectile -> MobEvent.DeSpawn.Projectile(it)
                Mob.Type.DisplayNPC -> MobEvent.DeSpawn.DisplayNPC(it)
                Mob.Type.Basic, Mob.Type.Dungeon, Mob.Type.Boss, Mob.Type.Slayer -> MobEvent.DeSpawn.SkyblockMob(it)
            }.postAndCatch()
        } ?: removeRetry(entity)
        packetEntityIds.remove(entity.entityId)
    }

    private fun retry(entity: EntityLivingBase) =
        retries.add(RetryEntityInstancing(entity, 0)).also { devTracker.data.startedRetries++ }

    private fun removeRetry(entity: EntityLivingBase) = retries.removeIf { it.entity == entity }

    class RetryEntityInstancing(var entity: EntityLivingBase, var times: Int) : Comparable<RetryEntityInstancing> {
        override fun hashCode() = entity.hashCode()
        override fun compareTo(other: RetryEntityInstancing) = this.hashCode() - other.hashCode()
        override fun equals(other: Any?) = (other as? EntityLivingBase) == entity
    }

    private fun handleRetries() {
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
                    "I (`${retry.entity.name}`${retry.entity.entityId} missed. Position: ${retry.entity.getLorenzVec()} Distance: ${
                        entity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation())
                    } , ${
                        entity.getLorenzVec().subtract(LocationUtils.playerLocation())
                    }"
                )
                devTracker.data.misses++
                // Temporary Change
                // iterator.remove()
                retry.times = Int.MIN_VALUE
                // continue
            }
            if (!EntitySpawn(entity)) {
                retry.times++
                continue
            }
            iterator.remove()
        }
    }

    private fun handleEntityUpdate(entityID: Int): Boolean {
        val entity = EntityUtils.getEntityByID(entityID) as? EntityLivingBase ?: return false
        retries.firstOrNull { it.hashCode() == entity.hashCode() }?.apply { this.entity = entity }
        if (currentEntityLiving.contains(entity)) {
            currentEntityLiving.remove(entity)
            currentEntityLiving.add(entity)
        }
        // update maps
        entityToMob[entity]?.internalUpdateOfEntity(entity)
        return true
    }


    @SubscribeEvent
    fun onEntitySpawnPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        when (packet) {
            is S0FPacketSpawnMob -> addEntityUpdate(packet.entityID)
            is S0CPacketSpawnPlayer -> addEntityUpdate(packet.entityID)
            // is S0EPacketSpawnObject -> addEntityUpdate(packet.entityID)
            is S37PacketStatistics -> // one of the first packets that is sent when switching servers inside the BungeeCord Network (please some prove this, I just found it out via Testing)
            {
                packetEntityIds.clear()
            }
        }
    }

    val packetEntityIds = mutableSetOf<Int>()

    private fun addEntityUpdate(id: Int) {
        if (packetEntityIds.contains(id)) {
            entitiesThatRequireUpdatePacket.put(id)
        } else {
            packetEntityIds.add(id)
        }
    }

    @SubscribeEvent
    fun onMobEventSpawn(event: MobEvent.Spawn) {
        entityToMob.putAll(event.mob.makeEntityToMobAssociation())
        currentMobs.add(event.mob)
        devTracker.addEntityName(event.mob)
    }

    @SubscribeEvent
    fun onSkyblockMobSpawnEvent(event: MobEvent.Spawn.SkyblockMob) {
        skyblockMobs.add(event.mob)
    }

    @SubscribeEvent
    fun onSummonSpawnEvent(event: MobEvent.Spawn.Summon) {
        summoningMobs.add(event.mob)
    }

    @SubscribeEvent
    fun onDisplayNPCSpawnEvent(event: MobEvent.Spawn.DisplayNPC) {
        displayNPCs.add(event.mob)
    }

    @SubscribeEvent
    fun onRealPlayerSpawnEvent(event: MobEvent.Spawn.Player) {
        players.add(event.mob)
    }

    @SubscribeEvent
    fun onMobEventDeSpawn(event: MobEvent.DeSpawn) {
        entityToMob.remove(event.mob.baseEntity)
        event.mob.extraEntities?.forEach { entityToMob.remove(it) }
        currentMobs.remove(event.mob)
    }


    @SubscribeEvent
    fun onSkyblockMobDeSpawnEvent(event: MobEvent.DeSpawn.SkyblockMob) {
        skyblockMobs.remove(event.mob)

    }

    @SubscribeEvent
    fun onSummonDeSpawnEvent(event: MobEvent.DeSpawn.Summon) {
        summoningMobs.remove(event.mob)
    }

    @SubscribeEvent
    fun onDisplayNPCSpawnDeEvent(event: MobEvent.DeSpawn.DisplayNPC) {
        displayNPCs.remove(event.mob)
    }

    @SubscribeEvent
    fun onRealPlayerDeSpawnEvent(event: MobEvent.DeSpawn.Player) {
        players.remove(event.mob)
    }

    @SubscribeEvent
    fun onWorldRenderDebug(event: LorenzRenderWorldEvent) {
        if (mobDebugConfig.skyblockMobHighlight) {
            skyblockMobs.forEach {
                val color = if (it.mobType == Mob.Type.Boss) LorenzColor.DARK_GREEN else LorenzColor.GREEN
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), color.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.displayNPCHighlight) {
            displayNPCs.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.RED.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.realPlayerHighlight) {
            players.filterNot { it.baseEntity is EntityPlayerSP }.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.BLUE.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.summonHighlight) {
            summoningMobs.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.YELLOW.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.skyblockMobShowName) {
            skyblockMobs.forEach {
                event.drawString(
                    it.baseEntity.getLorenzVec().add(y = 2.5), "§5" + it.name
                )
            }
        }
        if (mobDebugConfig.displayNPCShowName) {
            displayNPCs.forEach {
                event.drawString(
                    it.baseEntity.getLorenzVec().add(y = 2.5), "§d" + it.name
                )
            }
        }
        if (mobDebugConfig.showRayHit) {
            rayTraceForSkyblockMob(Minecraft.getMinecraft().thePlayer, event.partialTicks)?.let {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.GOLD.toColor(), 0.5f)
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
                startedRetries = 0
                entityNames.clear()
            }
        }

        fun addEntityName(mob: Mob) {
            if (mob.mobType == Mob.Type.Player) return
            val name = when (mob.mobType) {
                Mob.Type.DisplayNPC -> "DNPC"
                Mob.Type.Summon -> "SUM "
                Mob.Type.Basic -> "BASE"
                Mob.Type.Dungeon -> "DUNG"
                Mob.Type.Boss -> "BOSS"
                Mob.Type.Slayer -> "SLAY"
                Mob.Type.Player -> "PLAY"
                Mob.Type.Projectile -> "PROJ"
                Mob.Type.Special -> "SPEC"
            } + " " + mob.name
            if (data.entityNames.contains(name)) return
            data.entityNames.add(name)
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
                    data.startedRetries = loadedData.startedRetries
                    data.entityNames.clear()
                    data.entityNames.addAll(loadedData.entityNames)
                }
            } catch (e: IOException) {
                LorenzDebug.log("Error loading data from file: ${e.message}")
            }
        }

        override fun toString(): String {
            return "TrackerData(retries=${data.retries}, outOfRangeRetries=${data.outOfRangeRetries}, spawn=${data.spawn}, deSpawn=${data.deSpawn}, misses=${data.misses}, , startedRetries=${data.startedRetries}, retriesAvg=${data.retriesAvg}, entityNames=${data.entityNames})"
        }

    }

    @SubscribeEvent
    fun onJoin(event: HypixelJoinEvent) {
        devTracker.loadFromFile()
    }

    @SubscribeEvent
    fun onExit(event: IslandChangeEvent) {
        devTracker.saveToFile()
        // counter.reset()
    }
}

