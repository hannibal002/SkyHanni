package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobFilter.isDisplayNPC
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.mob.MobFilter.isSkyBlockMob
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.forEachPolling
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S37PacketStatistics
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.util.TreeSet
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

private const val MAX_RETRIES = 20 * 5

private const val MOB_DETECTION_LOG_PREFIX = "MobDetection: "


class MobData {
    private val forceReset get() = SkyHanniMod.feature.dev.mobDebug.forceReset

    class MobSet() : HashSet<Mob>() {
        val entityList get() = this.flatMap { listOf(it.baseEntity) + (it.extraEntities ?: emptyList()) }
    }

    companion object {

        val players = MobSet()
        val displayNPCs = MobSet()
        val skyblockMobs = MobSet()
        val summoningMobs = MobSet()
        val special = MobSet()
        val currentMobs = MobSet()

        val entityToMob = mutableMapOf<EntityLivingBase, Mob>()

        private val currentEntityLiving = mutableSetOf<EntityLivingBase>()
        private val previousEntityLiving = mutableSetOf<EntityLivingBase>()

        val retries = TreeSet<RetryEntityInstancing>()

        const val ENTITY_RENDER_RANGE_IN_BLOCKS = 80.0 // Entity DeRender after ~5 Chunks
        const val DETECTION_RANGE = 22.0
        private const val DISPLAY_NPC_DETECTION_RANGE = 24.0 // 24.0

        var externRemoveOfRetryAmount = 0
    }

    private var shouldClear: AtomicBoolean = AtomicBoolean(false)

    private fun mobDetectionReset() {
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
    }


    internal enum class Result {
        Found, NotYetFound, Illegal, SomethingWentWrong
    }

    internal class MobResult(val result: Result, val mob: Mob?) {
        companion object {
            val illegal = MobResult(Result.Illegal, null)
            val notYetFound = MobResult(Result.NotYetFound, null)
            val somethingWentWrong = MobResult(Result.SomethingWentWrong, null)
            fun found(mob: Mob) = MobResult(Result.Found, mob)

            fun EntityArmorStand?.makeMobResult(mob: (EntityArmorStand) -> Mob?) =
                this?.let { armor ->
                    mob.invoke(armor)?.let { found(it) } ?: somethingWentWrong
                } ?: notYetFound
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (shouldClear.get()) {
            mobDetectionReset()
            shouldClear.set(false)
        }
        if (!LorenzUtils.inSkyBlock) return
        if (event.isMod(2)) return

        makeEntityUpdate()

        handleMobsFromPacket()

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

    private fun EntityLivingBase.getRoughType() = when {
        this is EntityPlayer && this.isRealPlayer() -> Mob.Type.Player
        this.isDisplayNPC() -> Mob.Type.DisplayNPC
        this.isSkyBlockMob() -> Mob.Type.Basic
        else -> null
    }

    /** @return always true */
    private fun mobDetectionError(string: String) = LorenzDebug.log(MOB_DETECTION_LOG_PREFIX + string).let { true }

    /**@return a false means that it should try again (later)*/
    private fun entitySpawn(entity: EntityLivingBase, roughType: Mob.Type): Boolean {
        MobDevTracker.data.spawn++
        when (roughType) {
            Mob.Type.Player -> MobEvent.Spawn.Player(MobFactories.player(entity)).postAndCatch()

            Mob.Type.DisplayNPC -> return MobFilter.createDisplayNPC(entity)
            Mob.Type.Basic -> {
                if (islandException()) return true
                val mobResult = MobFilter.createSkyblockEntity(entity)
                if (mobResult.result == Result.NotYetFound) return false
                if (mobResult.result == Result.Illegal) return true
                if (mobResult.result == Result.SomethingWentWrong) return mobDetectionError("Something Went Wrong!")
                if (mobResult.mob == null) return mobDetectionError("Mob is null even though result is Found")
                when (mobResult.mob.mobType) {
                    Mob.Type.Summon -> MobEvent.Spawn.Summon(mobResult.mob).postAndCatch()

                    Mob.Type.Basic, Mob.Type.Dungeon, Mob.Type.Boss, Mob.Type.Slayer -> MobEvent.Spawn.SkyblockMob(mobResult.mob).postAndCatch()

                    Mob.Type.Special -> MobEvent.Spawn.Special(mobResult.mob).postAndCatch()
                    Mob.Type.Projectile -> MobEvent.Spawn.Projectile(mobResult.mob).postAndCatch()
                    Mob.Type.DisplayNPC -> MobEvent.Spawn.DisplayNPC(mobResult.mob).postAndCatch()
                    Mob.Type.Player -> return mobDetectionError("An Player Ended Here. How?")
                }
            }

            else -> return true
        }
        return true
    }

    private val batFromPacket = ConcurrentLinkedQueue<Int>()
    private val villagerFromPacket = ConcurrentLinkedQueue<Int>()

    private fun handleMobsFromPacket() {
        batFromPacket.forEachPolling { id ->
            val entity = EntityUtils.getEntityByID(id) as? EntityLivingBase ?: return@forEachPolling
            if (entityToMob[entity] != null) return@forEachPolling
            retries.remove(RetryEntityInstancing(entity))
            MobEvent.Spawn.Projectile(MobFactories.projectile(entity, "Spirit Scepter Bat")).postAndCatch() // Needs different handling because 6 is default health of Bat
        }
        villagerFromPacket.forEachPolling { id ->
            val entity = EntityUtils.getEntityByID(id) as? EntityLivingBase ?: return@forEachPolling
            val mob = entityToMob[entity]
            if (mob != null && mob.mobType == Mob.Type.DisplayNPC) {
                MobEvent.DeSpawn.DisplayNPC(mob)
                retry(entity)
                return@forEachPolling
            }
            val retryInstance = RetryEntityInstancing(entity)
            retries.find { it == retryInstance }?.let {
                if (it.roughType == Mob.Type.DisplayNPC) {
                    retries.remove(retryInstance)
                    retry(entity)
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdateEvent(event: EntityHealthUpdateEvent) {
        if (event.entity is EntityBat && event.health == 6) {
            batFromPacket.add(event.entity.entityId)
        }
        if (event.entity is EntityVillager && event.health != 20) {
            villagerFromPacket.add(event.entity.entityId)
        }
    }


    private fun islandException(): Boolean = when (LorenzUtils.skyBlockIsland) {
        IslandType.GARDEN_GUEST -> true
        IslandType.PRIVATE_ISLAND_GUEST -> true
        else -> false
    }

    private fun EntityDeSpawn(entity: EntityLivingBase) {
        MobDevTracker.data.deSpawn++
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

    private fun retry(entity: EntityLivingBase) = entity.getRoughType()?.let { type ->
        retries.add(RetryEntityInstancing(entity, 0, type)).also { MobDevTracker.data.startedRetries++ }
    }

    private fun removeRetry(entity: EntityLivingBase) = retries.remove(RetryEntityInstancing(entity))

    class RetryEntityInstancing(var entity: EntityLivingBase, var times: Int, val roughType: Mob.Type) : Comparable<RetryEntityInstancing> {

        constructor(entity: EntityLivingBase) : this(entity, 0, Mob.Type.Special) // Only use this for compare

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
            val type = retry.roughType
            if (entity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation()) > when (type) {
                    Mob.Type.DisplayNPC -> DISPLAY_NPC_DETECTION_RANGE
                    Mob.Type.Player -> Double.POSITIVE_INFINITY
                    else -> DETECTION_RANGE
                }
            ) {
                MobDevTracker.data.outOfRangeRetries++
                continue
            }
            MobDevTracker.data.retries++
            if (retry.times > MAX_RETRIES) {
                mobDetectionError(
                    "(`${retry.entity.name}`${retry.entity.entityId} missed (Found? ${entityToMob[retry.entity] != null}). Position: ${retry.entity.getLorenzVec()} Distance: ${
                        entity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation())
                    } , ${
                        entity.getLorenzVec().subtract(LocationUtils.playerLocation())
                    }"
                )
                MobDevTracker.data.misses++
                // Temporary Change
                // iterator.remove()
                retry.times = Int.MIN_VALUE
                // continue
            }
            if (!entitySpawn(entity, type)) {
                retry.times++
                continue
            }
            iterator.remove()
        }
    }

    private val entityUpdatePackets = LinkedBlockingQueue<Int>()
    private val entitiesThatRequireUpdate = mutableSetOf<Int>()

    private fun makeEntityUpdate() {
        entitiesThatRequireUpdate.iterator().let { iterator ->
            while (iterator.hasNext()) {
                if (handleEntityUpdate(iterator.next())) iterator.remove()
            }
        }
        while (entityUpdatePackets.isNotEmpty()) {
            entitiesThatRequireUpdate.add(entityUpdatePackets.take())
        }
    }


    private fun handleEntityUpdate(entityID: Int): Boolean {
        val entity = EntityUtils.getEntityByID(entityID) as? EntityLivingBase ?: return false
        retries.firstOrNull { it.hashCode() == entity.hashCode() }?.apply { this.entity = entity }
        if (currentEntityLiving.contains(entity)) {
            currentEntityLiving.remove(entity)
            currentEntityLiving.add(entity)
        }
        // update map
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
                shouldClear.set(true)
                packetEntityIds.clear()
            }
        }
    }

    val packetEntityIds = mutableSetOf<Int>()

    private fun addEntityUpdate(id: Int) {
        if (packetEntityIds.contains(id)) {
            entityUpdatePackets.put(id)
        } else {
            packetEntityIds.add(id)
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        shouldClear.set(true)
    }

    @SubscribeEvent
    fun onMobEventSpawn(event: MobEvent.Spawn) {
        entityToMob.putAll(event.mob.makeEntityToMobAssociation())
        currentMobs.add(event.mob)
        MobDevTracker.addEntityName(event.mob)
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
    fun onSpecialSpawnEvent(event: MobEvent.Spawn.Special) {
        special.add(event.mob)
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
    fun onSpecialDeSpawnEvent(event: MobEvent.DeSpawn.Special) {
        special.remove(event.mob)
    }

    @SubscribeEvent
    fun onDisplayNPCSpawnDeEvent(event: MobEvent.DeSpawn.DisplayNPC) {
        displayNPCs.remove(event.mob)
    }

    @SubscribeEvent
    fun onRealPlayerDeSpawnEvent(event: MobEvent.DeSpawn.Player) {
        players.remove(event.mob)
    }
}

