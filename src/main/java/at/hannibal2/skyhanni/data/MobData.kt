package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
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
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.EntityUtils.isRunic
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.union
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.toSingletonListOrEmpty
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.getLorenzVec
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.util.AxisAlignedBB
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

    companion object {

        val currentRealPlayers = mutableSetOf<Mob>()
        val currentDisplayNPCs = mutableSetOf<Mob>()
        val currentSkyblockMobs = mutableSetOf<Mob>()
        val currentSummoningMobs = mutableSetOf<Mob>()
        val currentMobs = mutableSetOf<Mob>()

        val currentRealPlayersMap = mutableMapOf<EntityLivingBase, Mob>()
        val currentDisplayNPCsMap = mutableMapOf<EntityLivingBase, Mob>()
        val currentSkyblockMobsMap = mutableMapOf<EntityLivingBase, Mob>()
        val currentSummoningMobsMap = mutableMapOf<EntityLivingBase, Mob>()

        val currentEntityToMobMap = mutableMapOf<EntityLivingBase, Mob>()
        private val currentEntityLiving = mutableSetOf<EntityLivingBase>()
        private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

        val retries = TreeSet<RetryEntityInstancing>()

        const val ENTITY_RENDER_RANGE_IN_BLOCKS = 80.0 // Entity DeRender after ~5 Chunks

        var externRemoveOfRetryAmount = 0

        val entitiesThatRequireUpdate = LinkedBlockingQueue<Int>()
    }

    private fun mobDetectionReset() {
        currentSkyblockMobs.clear()
        currentSkyblockMobsMap.clear()
        currentDisplayNPCs.clear()
        currentDisplayNPCsMap.clear()
        currentSummoningMobs.clear()
        currentSummoningMobsMap.clear()
        currentRealPlayers.clear()
    }


    @SubscribeEvent
    fun onTickForEntityDetection(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.isMod(2)) return

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

    private fun makeEntityUpdate() {
        while (entitiesThatRequireUpdate.isNotEmpty()) {
            handleEntityUpdate(entitiesThatRequireUpdate.take())
        }
    }

    private fun EntitySpawn(entity: EntityLivingBase): Boolean {
        devTracker.data.spawn++
        when {
            entity is EntityPlayer && entity.isRealPlayer() -> MobEvent.Spawn.Player(factories.player(entity))
                .postAndCatch()

            entity.isDisplayNPC() -> return createDisplayNPC(entity)
            entity.isSkyBlockMob() -> {
                if (islandException()) return true
                val it = MobFilter.createSkyblockEntity(entity)
                if (it.result == Result.NotYetFound) return false
                if (it.result == Result.Illegal) return true
                if (it.mob == null) throw IllegalStateException("Mob is null even though result is Found")
                when (it.mob.mobType) {
                    MobType.Summon -> MobEvent.Spawn.Summon(it.mob).postAndCatch()

                    MobType.Basic, MobType.Dungeon, MobType.Boss, MobType.Slayer -> MobEvent.Spawn.SkyblockMob(it.mob)
                        .postAndCatch()

                    MobType.Special -> MobEvent.Spawn.Special(it.mob).postAndCatch()
                    MobType.Projectile -> MobEvent.Spawn.Projectile(it.mob).postAndCatch()
                    else -> {}
                }
            }
        }
        return true
    }

    private fun createDisplayNPC(entity: EntityLivingBase): Boolean = MobUtils.getArmorStandByRangeAll(entity, 1.0)
        .firstOrNull { !it.name.startsWith("§e§lCLICK") && !it.isDefaultValue() }?.let { armorStand ->
            MobEvent.Spawn.DisplayNPC(factories.displayNPC(entity, armorStand)).postAndCatch().also { true }
        } ?: false


    private fun islandException(): Boolean = when (LorenzUtils.skyBlockIsland) {
        IslandType.THE_RIFT -> false
        IslandType.PRIVATE_ISLAND_GUEST -> true
        else -> false
    }

    private fun EntityDeSpawn(entity: EntityLivingBase) {
        devTracker.data.deSpawn++
        currentEntityToMobMap[entity]?.let {
            when (it.mobType) {
                MobType.Player -> MobEvent.DeSpawn.Summon(it)
                MobType.Summon -> MobEvent.DeSpawn.Summon(it)
                MobType.Special -> MobEvent.DeSpawn.Special(it)
                MobType.Projectile -> MobEvent.DeSpawn.Projectile(it)
                MobType.DisplayNPC -> MobEvent.DeSpawn.DisplayNPC(it)
                MobType.Basic, MobType.Dungeon, MobType.Boss, MobType.Slayer -> MobEvent.DeSpawn.SkyblockMob(it)
            }.postAndCatch()
        } ?: removeRetry(entity)
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
                    "I (${retry.entity.name} missed. Position: ${retry.entity.getLorenzVec()} Distance: ${
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

    private fun handleEntityUpdate(entityID: Int) {
        val entity = EntityUtils.getEntityByID(entityID) as? EntityLivingBase ?: return
        retries.firstOrNull { it.hashCode() == entity.hashCode() }?.apply { this.entity = entity }
        currentEntityLiving.remove(entity)
        currentEntityLiving.add(entity)
        // update maps
        currentEntityToMobMap[entity]?.internalUpdateOfEntity(entity)
    }


    @SubscribeEvent
    fun onEntitySpawnPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        when (packet) {
            is S0FPacketSpawnMob -> addEntityUpdate(packet.entityID)
            is S0CPacketSpawnPlayer -> addEntityUpdate(packet.entityID)
        }
    }

    private fun addEntityUpdate(id: Int) {
        EntityUtils.getEntityByID(id)?.let {
            entitiesThatRequireUpdate.put(id)
            LorenzDebug.log("ID: $id")
        }
    }

    @SubscribeEvent
    fun onMobEventSpawn(event: MobEvent.Spawn) {
        currentEntityToMobMap.putAll(event.mob.makeEntityToMobAssociation())
        currentMobs.add(event.mob)
        devTracker.addEntityName(event.mob)
    }

    @SubscribeEvent
    fun onSkyblockMobSpawnEvent(event: MobEvent.Spawn.SkyblockMob) {
        currentSkyblockMobsMap.putAll(event.mob.makeEntityToMobAssociation())
        currentSkyblockMobs.add(event.mob)
    }

    @SubscribeEvent
    fun onSummonSpawnEvent(event: MobEvent.Spawn.Summon) {
        currentSummoningMobsMap.putAll(event.mob.makeEntityToMobAssociation())
        currentSummoningMobs.add(event.mob)
    }

    @SubscribeEvent
    fun onDisplayNPCSpawnEvent(event: MobEvent.Spawn.DisplayNPC) {
        currentDisplayNPCsMap.putAll(event.mob.makeEntityToMobAssociation())
        currentDisplayNPCs.add(event.mob)
    }

    @SubscribeEvent
    fun onRealPlayerSpawnEvent(event: MobEvent.Spawn.Player) {
        currentRealPlayersMap.putAll(event.mob.makeEntityToMobAssociation())
        currentRealPlayers.add(event.mob)
    }

    @SubscribeEvent
    fun onMobEventDeSpawn(event: MobEvent.DeSpawn) {
        currentEntityToMobMap.remove(event.mob.baseEntity)
        event.mob.extraEntities?.forEach { currentEntityToMobMap.remove(it) }
        currentMobs.remove(event.mob)
    }


    @SubscribeEvent
    fun onSkyblockMobDeSpawnEvent(event: MobEvent.DeSpawn.SkyblockMob) {
        currentSkyblockMobsMap.remove(event.mob.baseEntity)
        event.mob.extraEntities?.forEach { currentSkyblockMobsMap.remove(it) }
        currentSkyblockMobs.remove(event.mob)

    }

    @SubscribeEvent
    fun onSummonDeSpawnEvent(event: MobEvent.DeSpawn.Summon) {
        currentSummoningMobsMap.remove(event.mob.baseEntity)
        event.mob.extraEntities?.forEach { currentSummoningMobsMap.remove(it) }
        currentSummoningMobs.remove(event.mob)
    }

    @SubscribeEvent
    fun onDisplayNPCSpawnDeEvent(event: MobEvent.DeSpawn.DisplayNPC) {
        currentDisplayNPCsMap.remove(event.mob.baseEntity)
        event.mob.extraEntities?.forEach { currentDisplayNPCsMap.remove(it) }
        currentDisplayNPCs.remove(event.mob)
    }

    @SubscribeEvent
    fun onRealPlayerDeSpawnEvent(event: MobEvent.DeSpawn.Player) {
        currentRealPlayersMap.remove(event.mob.baseEntity)
        event.mob.extraEntities?.forEach { currentRealPlayersMap.remove(it) }
        currentRealPlayers.remove(event.mob)
    }

    @SubscribeEvent
    fun onWorldRenderDebug(event: LorenzRenderWorldEvent) {
        if (mobDebugConfig.skyblockMobHighlight) {
            currentSkyblockMobs.forEach {
                val color = if (it.mobType == MobType.Boss) LorenzColor.DARK_GREEN else LorenzColor.GREEN
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), color.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.displayNPCHighlight) {
            currentDisplayNPCs.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.RED.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.realPlayerHighlight) {
            currentRealPlayers.filterNot { it.baseEntity is EntityPlayerSP }.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.BLUE.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.summonHighlight) {
            currentSummoningMobs.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.YELLOW.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.skyblockMobShowName) {
            currentSkyblockMobs.forEach {
                event.drawString(
                    it.baseEntity.getLorenzVec().add(y = 2.5), "§5" + it.name
                )
            }
        }
        if (mobDebugConfig.displayNPCShowName) {
            currentDisplayNPCs.forEach {
                event.drawString(
                    it.baseEntity.getLorenzVec().add(y = 2.5), "§d" + it.name
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
            if (mob.mobType == MobType.Player) return
            val name = when (mob.mobType) {
                MobType.DisplayNPC -> "DNPC"
                MobType.Summon -> "SUM "
                MobType.Basic -> "BASE"
                MobType.Dungeon -> "DUNG"
                MobType.Boss -> "BOSS"
                MobType.Slayer -> "SLAY"
                MobType.Player -> "PLAY"
                MobType.Projectile -> "PROJ"
                MobType.Special -> "SPEC"
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


    enum class Result {
        Found, NotYetFound, Illegal
    }

    enum class MobType {
        DisplayNPC, Summon, Basic, Dungeon, Boss, Slayer, Player, Projectile, Special;

        fun isSkyblockMob() = when (this) {
            Basic, Dungeon, Boss, Slayer -> true
            else -> false
        }
    }

    class MobResult(val result: Result, val mob: Mob?)

    class Mob(
        var baseEntity: EntityLivingBase,
        val mobType: MobType,
        val armorStand: EntityArmorStand? = null,
        val name: String = "",
        additionalEntities: List<EntityLivingBase>? = null,
        ownerName: String? = null,
        val hasStar: Boolean = false,
        val attribute: String = "",
        val levelOrTier: Int = -1,
    ) {

        val owner: MobUtils.OwnerShip?

        val hologram1 by lazy { MobUtils.getArmorStand(armorStand ?: baseEntity, 1) }
        val hologram2 by lazy { MobUtils.getArmorStand(armorStand ?: baseEntity, 2) }

        val extraEntities: List<EntityLivingBase>? get() = extraEntitiesList

        override fun hashCode(): Int {
            return baseEntity.hashCode()
        }

        val isCorrupted get() = baseEntity.isCorrupted() // Can change
        val isRunic = baseEntity.isRunic() // Does not Change

        fun isInRender() = baseEntity.distanceToPlayer() < ENTITY_RENDER_RANGE_IN_BLOCKS

        fun canBeSeen() = baseEntity.canBeSeen()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Mob

            return baseEntity == other.baseEntity
        }

        private var extraEntitiesList = additionalEntities?.toMutableList()
        private var relativeBoundingBox: AxisAlignedBB?
        val boundingBox: AxisAlignedBB
            get() = (relativeBoundingBox?.offset(baseEntity.posX, baseEntity.posY, baseEntity.posZ)
                ?: baseEntity.entityBoundingBox).expandBlock()

        private val summonOwnerRegex = "Spawned by: (.*)".toRegex()

        init {
            removeExtraEntitiesFromChecking()
            relativeBoundingBox = makeRelativeBoundingBox()

            owner = (ownerName ?: if (mobType == MobType.Slayer) hologram2?.let {
                summonOwnerRegex.find(it.cleanName())?.groupValues?.get(1)
            } else null)?.let { MobUtils.OwnerShip(it) }
        }

        private fun removeExtraEntitiesFromChecking() =
            extraEntities?.count { MobData.retries.contains(MobData.RetryEntityInstancing(it, 0)) }?.also {
                MobData.externRemoveOfRetryAmount += it
            }

        private fun makeRelativeBoundingBox() =
            (baseEntity.entityBoundingBox.union(extraEntities?.filter { it !is EntityArmorStand }
                ?.mapNotNull { it.entityBoundingBox }))?.offset(-baseEntity.posX, -baseEntity.posY, -baseEntity.posZ)

        fun addEntityInFront(entity: EntityLivingBase) {
            extraEntitiesList?.add(0, entity) ?: run { extraEntitiesList = mutableListOf(entity) }
            relativeBoundingBox = makeRelativeBoundingBox()
            // MobData.putSummonOrSkyblockMob(entity, this) TODO
        }

        fun addEntityInFront(entities: Collection<EntityLivingBase>) {
            extraEntitiesList?.addAll(0, entities) ?: run { extraEntitiesList = entities.toMutableList() }
            relativeBoundingBox = makeRelativeBoundingBox()
            removeExtraEntitiesFromChecking()
            // MobData.putAllSummonOrSkyblockMob(entities, this) TODO
        }

        fun internalUpdateOfEntity(entity: EntityLivingBase) {
            if (entity == baseEntity) baseEntity = entity else {
                extraEntitiesList?.remove(entity)
                extraEntitiesList?.add(entity)
            }
        }

        fun makeEntityToMobAssociation() =
            (baseEntity.toSingletonListOrEmpty() + (extraEntities ?: emptyList())).associateWith { this }


    }

    object factories {
        fun slayer(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
            MobFilter.slayerNameFilter.find(armorStand.cleanName())?.let {
                Mob(baseEntity = baseEntity, mobType = MobType.Slayer, armorStand = armorStand, name = it.groupValues[1], additionalEntities = extraEntityList, levelOrTier = it.groupValues[2].romanToDecimal())
            }

        fun boss(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
            MobFilter.bossMobNameFilter.find(armorStand.cleanName())?.let {
                Mob(baseEntity = baseEntity, mobType = MobType.Slayer, armorStand = armorStand, name = it.groupValues[3], additionalEntities = extraEntityList)
            }

        fun dungeon(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase> = emptyList()): Mob? {
            var initStartIndex = 0
            val nameWithoutColor = armorStand.cleanName()
            val words = nameWithoutColor.split(" ", ignoreCase = true)

            val hasStar = (words[initStartIndex] == "✯").also { if (it) initStartIndex++ }

            val attribute =
                MobFilter.dungeonAttribute.firstOrNull { it == words[initStartIndex] }?.also { initStartIndex++ } ?: ""

            // For a wierd reason the Undead Skeletons (or similar)
            // can spawn with a level if they are summoned with the 3 skulls
            words[initStartIndex].startsWith("[").also { if (it) initStartIndex++ }

            val name = words.subList(initStartIndex, words.lastIndex).joinToString(separator = " ")
            return Mob(baseEntity, MobType.Dungeon, armorStand, name, extraEntityList, hasStar = hasStar, attribute = attribute)
        }

        fun basic(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
            MobFilter.mobNameFilter.find(armorStand.cleanName())?.let {
                Mob(baseEntity = baseEntity, mobType = MobType.Basic, armorStand = armorStand, name = it.groupValues[4].removeCorruptedSuffix(it.groupValues[3].isNotEmpty()), additionalEntities = extraEntityList, levelOrTier = it.groupValues[2].toInt())
            }

        fun basic(baseEntity: EntityLivingBase, name: String) =
            Mob(baseEntity = baseEntity, mobType = MobType.Basic, name = name)

        fun summon(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
            MobFilter.summoningRegex.find(armorStand.cleanName())?.let {
                Mob(baseEntity = baseEntity, mobType = MobType.Summon, armorStand = armorStand, name = it.groupValues[2], additionalEntities = extraEntityList, ownerName = it.groupValues[1])
            }

        fun displayNPC(baseEntity: EntityLivingBase, armorStand: EntityArmorStand): Mob =
            Mob(baseEntity = baseEntity, mobType = MobType.DisplayNPC, armorStand = armorStand, name = armorStand.cleanName())

        fun player(baseEntity: EntityLivingBase): Mob = Mob(baseEntity, MobType.Player)
        fun projectile(baseEntity: EntityLivingBase, name: String): Mob =
            Mob(baseEntity = baseEntity, mobType = MobType.Projectile, name = name)

        fun special(baseEntity: EntityLivingBase, name: String) =
            Mob(baseEntity = baseEntity, mobType = MobType.Special, name = name)

        private fun String.removeCorruptedSuffix(case: Boolean) = if (case) this.dropLast(1) else this

    }
}

