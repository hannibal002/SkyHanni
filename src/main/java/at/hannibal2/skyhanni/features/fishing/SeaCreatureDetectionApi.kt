package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.fishing.FishingApi.babySlugName
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.spawnTime
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerPosData
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.getLorenzVec
import com.google.common.cache.RemovalCause
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// TODO: maybe replace "handleMobs" functions with code in their event for faster detection?
@SkyHanniModule
object SeaCreatureDetectionApi {

    val DESPAWN_TIME = 6.minutes

    private const val MAX_WAIT_DEATH_DISTANCE = 25.0

    private val entityIdToData = TimeLimitedCache<Int, LivingSeaCreatureData>(DESPAWN_TIME) { id, data, cause ->
        if (cause == RemovalCause.EXPIRED && data != null && id != null) data.forceRemove()
    }

    fun getSeaCreatures(): Collection<LivingSeaCreatureData> = entityIdToData.values
    private val seaCreatures = mutableMapOf<Mob, LivingSeaCreatureData>()

    val Mob.seaCreature: LivingSeaCreatureData? get() = seaCreatures[this]

    private var lastNameFished: String? = null
    private var mobsToFind = 0
    private var lastSeaCreatureFished = SimpleTimeMark.farPast()

    private val recentMobs = mutableMapOf<Mob, ServerTimeMark>()

    private var babyMagmaSlugsToFind = 0
    private var lastMagmaSlugLocation: LorenzVec? = null
    private var lastMagmaSlugTime = SimpleTimeMark.farPast()

    private val recentBabyMagmaSlugs = mutableMapOf<Mob, ServerTimeMark>()

    var lastBobberLocation: LorenzVec? = null

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isActive()) return
        val mob = event.mob
        val data = entityIdToData[mob.id]
        if (data != null) {
            seaCreatures[mob] = data
            data.mob = mob
            SeaCreatureEvent.ReDetect(data).post()
            return
        }

        if (mob.name == babySlugName) {
            recentBabyMagmaSlugs[mob] = ServerTimeMark.now()
            DelayedRun.runNextTick {
                handleBabySlugs()
            }
            return
        }
        if (mob.name !in SeaCreatureManager.allFishingMobs) return
        recentMobs[mob] = ServerTimeMark.now()
        handleOwnMob()
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob
        recentBabyMagmaSlugs.remove(mob)
        recentMobs.remove(mob)
        val data = seaCreatures[mob] ?: return
        seaCreatures.remove(mob)
        val oldId = data.entityId
        val newId = mob.id
        data.despawn()
        if (!mob.isAlive) {
            entityIdToData.remove(oldId)
            data.forceRemove()
            if (data.isOwn) {
                if (mob.name == "Magma Slug") {
                    lastMagmaSlugLocation = mob.getLorenzVec()
                    babyMagmaSlugsToFind += 3
                    lastMagmaSlugTime = SimpleTimeMark.now()
                    handleBabySlugs()
                }
            }
            data.sendDeath()
            return
        } else if (oldId != newId) { // we update the entity id in case the baseEntity has changed at some point
            entityIdToData.remove(oldId)
            entityIdToData[newId] = data
            data.entityId = newId
        }
        data.mob = null
    }

    @HandleEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        lastSeaCreatureFished = SimpleTimeMark.now()
        lastNameFished = event.seaCreature.name
        mobsToFind = if (event.doubleHook) 2 else 1
        handleOwnMob()
    }

    private fun addMob(
        mob: Mob,
        time: ServerTimeMark = ServerTimeMark.now(),
        isOwn: Boolean = false,
    ) {
        val seaCreature = SeaCreatureManager.allFishingMobs[mob.name] ?: return
        val data = LivingSeaCreatureData(isOwn, seaCreature, mob.id, time, mob)
        seaCreatures[mob] = data
        entityIdToData[mob.id] = data
        SeaCreatureEvent.Spawn(data).post()
    }

    private fun handleOwnMob() {
        if (lastSeaCreatureFished.passedSince() > 1.seconds) return
        val name = lastNameFished ?: return
        val bobberLocation = lastBobberLocation ?: return
        val mobs = recentMobs.asSequence().filter { (mob, data) -> mob.name == name && data.passedSince() < 1.5.seconds }.map {
            it to it.key.baseEntity.distanceTo(bobberLocation)
        }.filter { it.second <= 3 }
            .sortedBy { it.second }
            .take(mobsToFind).toList()

        mobsToFind = findMobs(mobs, mobsToFind, recentMobs) ?: return

        if (mobsToFind == 0) {
            lastNameFished = null
            lastBobberLocation = null
        }
    }

    private fun findMobs(
        mobs: List<Pair<Map.Entry<Mob, ServerTimeMark>, Double>>,
        toBeFound: Int,
        nonParsedMobs: MutableMap<Mob, ServerTimeMark>,
    ): Int? {
        if (mobs.isEmpty()) return null
        for (mob in mobs.map { it.first.key }) {
            addMob(mob, mob.baseEntity.spawnTime, isOwn = true)
            nonParsedMobs.remove(mob)
        }
        return toBeFound - mobs.size
    }

    private fun handleBabySlugs() {
        if (lastMagmaSlugTime.passedSince() > 1.seconds) return
        if (babyMagmaSlugsToFind == 0) return
        val location = lastMagmaSlugLocation ?: return
        val slugs = recentBabyMagmaSlugs.asSequence().map {
            it to it.key.baseEntity.distanceTo(location)
        }.filter { it.second <= 2 }
            .sortedBy { it.second }
            .take(babyMagmaSlugsToFind).toList()

        babyMagmaSlugsToFind = findMobs(slugs, babyMagmaSlugsToFind, recentBabyMagmaSlugs) ?: return

        if (babyMagmaSlugsToFind == 0) {
            lastMagmaSlugLocation = null
        }
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGHEST)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        for (data in seaCreatures.values) data.updateWorld(event)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        for (data in seaCreatures.values) data.updateNonWorld()
        recentMobs.removeIf { (mob, time) ->
            if (time.passedSince() < 1.2.seconds) return@removeIf false
            addMob(mob, time, isOwn = false)
            return@removeIf true
        }
        recentBabyMagmaSlugs.removeIf { (mob, time) ->
            if (time.passedSince() < 1.2.seconds) return@removeIf false
            addMob(mob, time, isOwn = false)
            return@removeIf true
        }
        val bobber = FishingApi.bobber ?: return
        lastBobberLocation = bobber.getLorenzVec()
        if (babyMagmaSlugsToFind != 0 && lastMagmaSlugTime.passedSince() > 2.seconds) babyMagmaSlugsToFind = 0
    }

    // This should hopefully make it so that if a sea creature dies while the player isn't in the area and the despawn timer
    // isn't up yet, it will be assumed that it died
    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        val playerPos = LocationUtils.playerLocation()
        for ((_, data) in entityIdToData) {
            if (!assumeDeathIfAreaLeft(data, playerPos)) continue
            data.sendDeath(false)
        }
    }

    private fun assumeDeathIfAreaLeft(data: LivingSeaCreatureData, playerPos: LorenzVec): Boolean {
        if (data.exists()) return false
        val lastPos = data.actualLastPos ?: return false
        if (lastPos.distance(playerPos) > MAX_WAIT_DEATH_DISTANCE) return false
        val timeAroundPos = PlayerPosData.timeAtPos(lastPos, MAX_WAIT_DEATH_DISTANCE) ?: return false
        return timeAroundPos >= 5.seconds
    }

    @HandleEvent
    fun onWorldChange() = reset()

    private fun reset() {
        entityIdToData.values.forEach { it.forceRemove() }
        entityIdToData.clear()
        seaCreatures.clear()
        recentMobs.clear()
        recentBabyMagmaSlugs.clear()
        lastMagmaSlugLocation = null
        babyMagmaSlugsToFind = 0
        lastMagmaSlugTime = SimpleTimeMark.farPast()
        lastSeaCreatureFished = SimpleTimeMark.farPast()
        lastNameFished = null
        mobsToFind = 0
        lastBobberLocation = null
    }

    @HandleEvent
    fun onCommand(event: CommandRegistrationEvent) {
        event.registerBrigadier("skyhanniresetlivingseacreaturedata") {
            this.aliases = listOf("shresetscdata")
            this.description = "Resets Living Sea Creature Data"
            this.category = CommandCategory.DEVELOPER_TEST
            callback { reset() }
        }
    }

    private fun isActive(): Boolean = !DungeonApi.inDungeon()

    @Suppress("MaxLineLength")
    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Sea Creatures")
        event.addIrrelevant {
            entityIdToData.forEach { (entityID, data) ->
                with(data) {
                    val rare = if (seaCreature.rare) " rare!" else ""
                    val name = "${seaCreature.name} ($rarity$rare) "
                    add("$entityID: isOwn=$isOwn repoSeaCreatureData=$name spawnTime=$spawnTime mob=$mob pos=$pos aabb=$aabb")
                }
            }
            seaCreatures.forEach { (_, data) ->
                with(data) {
                    val rare = if (seaCreature.rare) " rare!" else ""
                    val name = "${seaCreature.name} ($rarity$rare) "
                    add("repoSeaCreatureData=$name spawnTime=$spawnTime mob=$mob pos=$pos aabb=$aabb")
                }
            }
            add("lastNameFished $lastNameFished")
            add("mobsToFind: $mobsToFind")
            add("lastSeaCreatureFishedTime: $lastSeaCreatureFished")
            add("recentMobs ${recentMobs.entries}")
            add("babyMagmaSlugsToFind $babyMagmaSlugsToFind")
            add("lastMagmaSlugLocation $lastMagmaSlugLocation")
            add("lastMagmaSlugTime $lastMagmaSlugTime")
            add("recentBabyMagmaSlugs ${recentBabyMagmaSlugs.entries}")
        }
    }
}

