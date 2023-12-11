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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

private const val MAX_RETRIES = 20 * 5

private const val MOB_DETECTION_LOG_PREFIX = "MobDetection: "

class MobDetection {

    private val forceReset get() = SkyHanniMod.feature.dev.mobDebug.forceReset

    private var shouldClear: AtomicBoolean = AtomicBoolean(false)

    private fun mobDetectionReset() {
        MobData.currentMobs.map {
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

        MobData.previousEntityLiving.clear()
        MobData.previousEntityLiving.addAll(MobData.currentEntityLiving)
        MobData.currentEntityLiving.clear()
        MobData.currentEntityLiving.addAll(EntityUtils.getEntities<EntityLivingBase>().filter { it !is EntityArmorStand })

        if (forceReset) {
            MobData.currentEntityLiving.clear()
        }

        (MobData.currentEntityLiving - MobData.previousEntityLiving).forEach { retry(it) }
        (MobData.previousEntityLiving - MobData.currentEntityLiving).forEach { entityDeSpawn(it) }

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

    private fun retry(entity: EntityLivingBase) = entity.getRoughType()?.let { type ->
        MobData.retries.add(MobData.RetryEntityInstancing(entity, 0, type)).also { MobDevTracker.data.startedRetries++ }
    }

    private fun removeRetry(entity: EntityLivingBase) = MobData.retries.remove(MobData.RetryEntityInstancing(entity))


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
                if (mobResult.result == MobData.Result.NotYetFound) return false
                if (mobResult.result == MobData.Result.Illegal) return true
                if (mobResult.result == MobData.Result.SomethingWentWrong) return mobDetectionError("Something Went Wrong!")
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
            if (MobData.entityToMob[entity] != null) return@forEachPolling
            MobData.retries.remove(MobData.RetryEntityInstancing(entity))
            MobEvent.Spawn.Projectile(MobFactories.projectile(entity, "Spirit Scepter Bat")).postAndCatch() // Needs different handling because 6 is default health of Bat
        }
        villagerFromPacket.forEachPolling { id ->
            val entity = EntityUtils.getEntityByID(id) as? EntityLivingBase ?: return@forEachPolling
            val mob = MobData.entityToMob[entity]
            if (mob != null && mob.mobType == Mob.Type.DisplayNPC) {
                MobEvent.DeSpawn.DisplayNPC(mob)
                retry(entity)
                return@forEachPolling
            }
            val retryInstance = MobData.RetryEntityInstancing(entity)
            MobData.retries.find { it == retryInstance }?.let {
                if (it.roughType == Mob.Type.DisplayNPC) {
                    MobData.retries.remove(retryInstance)
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

    private fun entityDeSpawn(entity: EntityLivingBase) {
        MobDevTracker.data.deSpawn++
        MobData.entityToMob[entity]?.let {
            when (it.mobType) {
                Mob.Type.Player -> MobEvent.DeSpawn.Player(it)
                Mob.Type.Summon -> MobEvent.DeSpawn.Summon(it)
                Mob.Type.Special -> MobEvent.DeSpawn.Special(it)
                Mob.Type.Projectile -> MobEvent.DeSpawn.Projectile(it)
                Mob.Type.DisplayNPC -> MobEvent.DeSpawn.DisplayNPC(it)
                Mob.Type.Basic, Mob.Type.Dungeon, Mob.Type.Boss, Mob.Type.Slayer -> MobEvent.DeSpawn.SkyblockMob(it)
            }.postAndCatch()
        } ?: removeRetry(entity)
        allEntitiesViaPacketId.remove(entity.entityId)
    }

    private fun handleRetries() {
        val iterator = MobData.retries.iterator()
        while (iterator.hasNext()) {
            val retry = iterator.next()
            if (MobData.externRemoveOfRetryAmount > 0) {
                iterator.remove()
                MobData.externRemoveOfRetryAmount--
                continue
            }
            val entity = retry.entity
            val type = retry.roughType
            if (entity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation()) > when (type) {
                    Mob.Type.DisplayNPC -> MobData.DISPLAY_NPC_DETECTION_RANGE
                    Mob.Type.Player -> Double.POSITIVE_INFINITY
                    else -> MobData.DETECTION_RANGE
                }
            ) {
                MobDevTracker.data.outOfRangeRetries++
                continue
            }
            MobDevTracker.data.retries++
            if (retry.times > MAX_RETRIES) {
                mobDetectionError(
                    "(`${retry.entity.name}`${retry.entity.entityId} missed (Found? ${MobData.entityToMob[retry.entity] != null}). Position: ${retry.entity.getLorenzVec()} Distance: ${
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
        MobData.retries.firstOrNull { it.hashCode() == entity.hashCode() }?.apply { this.entity = entity }
        if (MobData.currentEntityLiving.contains(entity)) {
            MobData.currentEntityLiving.remove(entity)
            MobData.currentEntityLiving.add(entity)
        }
        // update map
        MobData.entityToMob[entity]?.internalUpdateOfEntity(entity)
        return true
    }


    @SubscribeEvent
    fun onEntitySpawnPacket(event: PacketEvent.ReceiveEvent) {
        when (val packet = event.packet) {
            is S0FPacketSpawnMob -> addEntityUpdate(packet.entityID)
            is S0CPacketSpawnPlayer -> addEntityUpdate(packet.entityID)
            // is S0EPacketSpawnObject -> addEntityUpdate(packet.entityID)
            is S37PacketStatistics -> // one of the first packets that is sent when switching servers inside the BungeeCord Network (please some prove this, I just found it out via Testing)
            {
                shouldClear.set(true)
                allEntitiesViaPacketId.clear()
            }
        }
    }

    private val allEntitiesViaPacketId = mutableSetOf<Int>()

    private fun addEntityUpdate(id: Int) {
        if (allEntitiesViaPacketId.contains(id)) {
            entityUpdatePackets.put(id)
        } else {
            allEntitiesViaPacketId.add(id)
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        shouldClear.set(true)
    }

}
