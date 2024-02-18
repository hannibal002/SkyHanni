package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobFilter.isDisplayNPC
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.mob.MobFilter.isSkyBlockMob
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.drainForEach
import at.hannibal2.skyhanni.utils.CollectionUtils.put
import at.hannibal2.skyhanni.utils.CollectionUtils.refreshReference
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityCreeper
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

    /* Unsupported "Mobs"
        Nicked Players
        Odanate
        Silk Worm
        Fairy (in Dungeon)
        Totem of Corruption
        Worm
        Scatha
        Butterfly
        Exe
        Wai
        Zee
     */

    private val forceReset get() = !SkyHanniMod.feature.dev.mobDebug.enable

    private var shouldClear: AtomicBoolean = AtomicBoolean(false)

    init {
        MobFilter.bossMobNameFilter
        MobFilter.mobNameFilter
        MobFilter.dojoFilter
        MobFilter.minionMobPrefix
        MobFilter.summonFilter
        MobFilter.dungeonNameFilter
        MobFilter.petCareNamePattern
        MobFilter.slayerNameFilter
        MobFilter.summonOwnerPattern
        MobFilter.wokeSleepingGolemPattern
    }

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
        if (shouldClear.get()) { // Needs to work outside skyblock since it needs clearing when leaving skyblock and joining limbo
            mobDetectionReset()
            shouldClear.set(false)
        }
        if (!LorenzUtils.inSkyBlock) return
        if (event.isMod(2)) return

        makeEntityReferenceUpdate()

        handleMobsFromPacket()

        handleRetries()

        MobData.previousEntityLiving.clear()
        MobData.previousEntityLiving.addAll(MobData.currentEntityLiving)
        MobData.currentEntityLiving.clear()
        MobData.currentEntityLiving.addAll(
            EntityUtils.getEntities<EntityLivingBase>().filter { it !is EntityArmorStand })

        if (forceReset) {
            MobData.currentEntityLiving.clear() // Naturally removing the mobs using the despawn
        }

        (MobData.currentEntityLiving - MobData.previousEntityLiving).forEach { addRetry(it) }  // Spawn
        (MobData.previousEntityLiving - MobData.currentEntityLiving).forEach { entityDeSpawn(it) } // Despawn

        if (forceReset) {
            mobDetectionReset() // Ensure that all mobs are cleared 100%
        }
    }

    /** Splits the entity into player, displayNPC and other */
    private fun EntityLivingBase.getRoughType() = when {
        this is EntityPlayer && this.isRealPlayer() -> Mob.Type.Player
        this.isDisplayNPC() -> Mob.Type.DisplayNPC
        this.isSkyBlockMob() && !islandException() -> Mob.Type.Basic
        else -> null
    }

    private fun addRetry(entity: EntityLivingBase) = entity.getRoughType()?.let { type ->
        val re = MobData.RetryEntityInstancing(entity, 0, type)
        MobData.retries.put(re.toKeyValuePair())
    }

    private fun removeRetry(entity: EntityLivingBase) = MobData.retries.remove(entity.entityId)

    private fun getRetry(entity: EntityLivingBase) = MobData.retries[entity.entityId]

    /** @return always true */
    private fun mobDetectionError(string: String) = LorenzDebug.log(MOB_DETECTION_LOG_PREFIX + string).let { true }

    /**@return a false means that it should try again (later)*/
    private fun entitySpawn(entity: EntityLivingBase, roughType: Mob.Type): Boolean {
        when (roughType) {
            Mob.Type.Player -> MobEvent.Spawn.Player(MobFactories.player(entity)).postAndCatch()

            Mob.Type.DisplayNPC -> return MobFilter.createDisplayNPC(entity)
            Mob.Type.Basic -> {
                val (result, mob) = MobFilter.createSkyblockEntity(entity)
                when (result) {
                    MobData.Result.NotYetFound -> return false
                    MobData.Result.Illegal -> return true // Remove entity from the spawning queue
                    MobData.Result.SomethingWentWrong -> return mobDetectionError("Something Went Wrong!")
                    MobData.Result.Found -> {
                        if (mob == null) return mobDetectionError("Mob is null even though result is Found")
                        when (mob.mobType) {
                            Mob.Type.Summon -> MobEvent.Spawn.Summon(mob)

                            Mob.Type.Basic, Mob.Type.Dungeon, Mob.Type.Boss, Mob.Type.Slayer ->
                                MobEvent.Spawn.SkyblockMob(mob)

                            Mob.Type.Special -> MobEvent.Spawn.Special(mob)
                            Mob.Type.Projectile -> MobEvent.Spawn.Projectile(mob)
                            Mob.Type.DisplayNPC ->
                                MobEvent.Spawn.DisplayNPC(mob) // Needed for some special cases
                            Mob.Type.Player -> return mobDetectionError("An Player Ended Here. How?")
                        }.postAndCatch()
                    }
                }
            }

            else -> return true
        }
        return true
    }

    private val entityFromPacket = ConcurrentLinkedQueue<Pair<EntityPacketType, Int>>()

    /** For mobs that have default health of the entity */
    private enum class EntityPacketType {
        SPIRIT_BAT,
        VILLAGER,
        CREEPER_VAIL
    }

    /** Handles some mobs that have default health of the entity, specially using the [EntityHealthUpdateEvent] */
    private fun handleMobsFromPacket() = entityFromPacket.drainForEach { (type, id) ->
        when (type) {
            EntityPacketType.SPIRIT_BAT -> {
                val entity = EntityUtils.getEntityByID(id) as? EntityBat ?: return@drainForEach
                if (MobData.entityToMob[entity] != null) return@drainForEach
                removeRetry(entity)
                MobEvent.Spawn.Projectile(MobFactories.projectile(entity, "Spirit Scepter Bat")).postAndCatch()
            }

            EntityPacketType.VILLAGER -> {
                val entity = EntityUtils.getEntityByID(id) as? EntityVillager ?: return@drainForEach
                val mob = MobData.entityToMob[entity]
                if (mob != null && mob.mobType == Mob.Type.DisplayNPC) {
                    MobEvent.DeSpawn.DisplayNPC(mob)
                    addRetry(entity)
                    return@drainForEach
                }
                getRetry(entity)?.let {
                    if (it.roughType == Mob.Type.DisplayNPC) {
                        removeRetry(entity)
                        addRetry(entity)
                    }
                }
            }

            EntityPacketType.CREEPER_VAIL -> {
                val entity = EntityUtils.getEntityByID(id) as? EntityCreeper ?: return@drainForEach
                if (MobData.entityToMob[entity] != null) return@drainForEach
                if (!entity.powered) return@drainForEach
                removeRetry(entity)
                MobEvent.Spawn.Special(MobFactories.special(entity, "Creeper Veil")).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdateEvent(event: EntityHealthUpdateEvent) {
        when {
            event.entity is EntityBat && event.health == 6 -> {
                entityFromPacket.add(EntityPacketType.SPIRIT_BAT to event.entity.entityId)
            }

            event.entity is EntityVillager && event.health != 20 -> {
                entityFromPacket.add(EntityPacketType.VILLAGER to event.entity.entityId)
            }

            event.entity is EntityCreeper && event.health == 20 -> {
                entityFromPacket.add(EntityPacketType.CREEPER_VAIL to event.entity.entityId)
            }
        }
    }

    private fun islandException(): Boolean = when (LorenzUtils.skyBlockIsland) {
        IslandType.GARDEN_GUEST -> true
        IslandType.PRIVATE_ISLAND_GUEST -> true
        else -> false
    }

    private fun entityDeSpawn(entity: EntityLivingBase) {
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
            val (_, retry) = iterator.next()
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
                continue
            }
            if (retry.times > MAX_RETRIES) {
                mobDetectionError(
                    "(`${retry.entity.name}`${retry.entity.entityId} missed (Found? ${MobData.entityToMob[retry.entity] != null}). Position: ${retry.entity.getLorenzVec()} Distance: ${
                        entity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation())
                    } , ${
                        entity.getLorenzVec().subtract(LocationUtils.playerLocation())
                    }"
                )
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
    private val entitiesThatRequireUpdate = mutableSetOf<Int>() // needs to be distinct, therefore not using a queue

    /** Refreshes the references of the entities in entitiesThatRequireUpdate */
    private fun makeEntityReferenceUpdate() {
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
        getRetry(entity)?.apply { this.entity = entity }
        MobData.currentEntityLiving.refreshReference(entity)
        MobData.previousEntityLiving.refreshReference(entity)
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

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        MobData.currentEntityLiving.remove(Minecraft.getMinecraft().thePlayer)
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
