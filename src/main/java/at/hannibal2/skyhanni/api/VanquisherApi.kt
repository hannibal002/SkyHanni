package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.combat.VanquisherEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.spawnTime
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addAll
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object VanquisherApi {

    data class VanquisherData(
        val isOwn: Boolean,
        val mob: Mob,
        val spawnTime: ServerTimeMark,
    ) {
        private var hasSentDespawn: Boolean = false
        fun postDespawn() {
            if (hasSentDespawn) return
            hasSentDespawn = true
            VanquisherEvent.DeSpawn(this).post()
        }
    }

    data object VanquisherOwnMessageEvent : SkyHanniEvent()

    private val patternGroup = RepoPattern.group("combat.crimsonisle.vanquisherapi")

    private val spawnPattern by patternGroup.pattern(
        "spawnpattern",
        "A Vanquisher is spawning nearby!",
    )

    private var lastOwnTime = SimpleTimeMark.farPast()
    private var spawnEntity: ArmorStand? = null

    private var lastPossibleSpawnEntity: ArmorStand? = null

    private var lastSpawnEntityPos: LorenzVec? = null
    private var lastSpawnEntityTime = SimpleTimeMark.farPast()
    private var lastSoundPos: LorenzVec? = null
    private var lastSoundTime = SimpleTimeMark.farPast()

    private val vanquishers = TimeLimitedCache<Mob, VanquisherData>(6.minutes) { mob, data, _ ->
        if (mob != null && data != null) data.postDespawn()
    }

    private val vanquisherShortTimeout = 2.seconds
    private val vanquisherLongTimeout = 5.seconds

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (spawnPattern.matches(event.cleanMessage)) {
            lastOwnTime = SimpleTimeMark.now()
            VanquisherOwnMessageEvent.post()
            DelayedRun.runNextTick(::handleOwnVanquisher)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onSound(event: PlaySoundEvent) {
        if (event.soundName != "entity.wither.spawn" || event.pitch != 1f || event.volume != 2f) return
        lastSoundPos = event.location
        lastSoundTime = SimpleTimeMark.now()
        DelayedRun.runNextTick(::handleOwnVanquisher)
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        val entity = event.entity as? ArmorStand ?: return
        val helmet = entity.getStandHelmet() ?: return
        if (helmet.item != Items.WITHER_SKELETON_SKULL) return
        lastSpawnEntityPos = entity.getLorenzVec()
        lastPossibleSpawnEntity = entity
        lastSpawnEntityTime = SimpleTimeMark.now()
        DelayedRun.runNextTick(::handleOwnVanquisher)
    }

    private fun handleOwnVanquisher() {
        val soundPos = lastSoundPos ?: return
        val entityPos = lastSpawnEntityPos ?: return
        val entity = lastPossibleSpawnEntity ?: return
        val now = SimpleTimeMark.now()
        if (now - lastSoundTime > vanquisherShortTimeout) return
        if (now - lastSpawnEntityTime > vanquisherShortTimeout) return
        if (now - lastOwnTime > vanquisherShortTimeout) return
        if (soundPos.distance(entityPos) > 3) return
        spawnEntity = entity
        lastSpawnEntityPos = null
        lastSoundPos = null
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.name != "Vanquisher") return
        val isOwn = mob.isOwnVanq()
        val spawnTime = mob.baseEntity.spawnTime
        val data = VanquisherData(isOwn, mob, spawnTime)
        vanquishers[mob] = data
        VanquisherEvent.Spawn(data).post()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob
        val data = vanquishers.remove(mob) ?: return
        data.postDespawn()
        if (!mob.isAlive) VanquisherEvent.Death(data).post()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onSecondPassed() {
        if ((lastPossibleSpawnEntity != null || lastSpawnEntityPos != null || lastSoundPos != null) &&
            lastOwnTime.passedSince() > vanquisherLongTimeout
        ) {
            lastPossibleSpawnEntity = null
            lastSpawnEntityPos = null
            lastSoundPos = null
        }

        if (spawnEntity != null && lastOwnTime.passedSince() > 8.seconds) {
            spawnEntity = null
        }
    }

    @HandleEvent
    fun onWorldChange() {
        vanquishers.values.forEach { it.postDespawn() }
        vanquishers.clear()
        lastPossibleSpawnEntity = null
        lastSpawnEntityPos = null
        lastSoundPos = null
        spawnEntity = null
    }

    private fun Mob.isOwnVanq(): Boolean {
        val spawnEntity = spawnEntity ?: return false
        if (baseEntity.distanceTo(spawnEntity) > 4) return false
        if (lastOwnTime.passedSince() > 7.seconds) return false // TODO: actually get good time
        ChatUtils.debug("Expected Own Vanquisher Took ${lastOwnTime.passedSince().format()}")
        return true
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("VanquisherAPI")
        event.addIrrelevant {
            addAll(
                "vanquishers $vanquishers",
                "lastOwnVanqTime $lastOwnTime",
                "vanqSpawnEntity $spawnEntity",
                "lastPossibleVanqSpawnEntity $lastPossibleSpawnEntity",
                "lastVanqSpawnEntityPos $lastSpawnEntityPos",
                "lastVanqSpawnEntityTime $lastSpawnEntityTime",
                "lastVanqSoundPos $lastSoundPos",
                "lastVanqSoundTime $lastSoundTime",
            )
        }
    }
}
