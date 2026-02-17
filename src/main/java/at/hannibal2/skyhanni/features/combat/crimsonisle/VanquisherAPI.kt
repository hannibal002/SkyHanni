package at.hannibal2.skyhanni.features.combat.crimsonisle

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.combat.VanquisherEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.spawnTime
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.clearAnd
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

    private val patternGroup = RepoPattern.group("combat.crimsonisle.vanquisherapi")

    private val spawnPattern by patternGroup.pattern(
        "spawnpattern",
        "A Vanquisher is spawning nearby!"

    )

    private var lastOwnVanqTime = SimpleTimeMark.farPast()
    private var vanqSpawnEntity: ArmorStand? = null

    private var lastPossibleVanqSpawnEntity: ArmorStand? = null

    private var lastVanqSpawnEntityPos: LorenzVec? = null
    private var lastVanqSpawnEntityTime = SimpleTimeMark.farPast()
    private var lastVanqSoundPos: LorenzVec? = null
    private var lastVanqSoundTime = SimpleTimeMark.farPast()

    private val vanquishers = TimeLimitedCache<Mob, VanquisherData>(6.minutes) { mob, data, _ ->
        if (mob != null && data != null) data.postDespawn()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (spawnPattern.matches(event.cleanMessage)) {
            lastOwnVanqTime = SimpleTimeMark.now()
            VanquisherEvent.OwnSpawn.post()
            DelayedRun.runNextTick(::handleOwnVanq)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onSound(event: PlaySoundEvent) {
        if (event.soundName != "mob.wither.spawn" || event.pitch != 1f || event.volume != 2f) return
        lastVanqSoundPos = event.location
        lastVanqSoundTime = SimpleTimeMark.now()
        DelayedRun.runNextTick(::handleOwnVanq)
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        val entity = event.entity as? ArmorStand ?: return
        val helmet = entity.getStandHelmet() ?: return
        if (helmet.item != Items.WITHER_SKELETON_SKULL) return // wither skeleton skull
        lastVanqSpawnEntityPos = entity.getLorenzVec()
        lastPossibleVanqSpawnEntity = entity
        lastVanqSpawnEntityTime = SimpleTimeMark.now()
        DelayedRun.runNextTick(::handleOwnVanq)
    }

    private fun handleOwnVanq() {
        val soundPos = lastVanqSoundPos ?: return
        val entityPos = lastVanqSpawnEntityPos ?: return
        val entity = lastPossibleVanqSpawnEntity ?: return
        val now = SimpleTimeMark.now()
        if (now - lastVanqSoundTime > 2.seconds) return
        if (now - lastVanqSpawnEntityTime > 2.seconds) return
        if (now - lastOwnVanqTime > 2.seconds) return
        if (soundPos.distance(entityPos) > 3) return
        vanqSpawnEntity = entity
        lastVanqSpawnEntityPos = null
        lastVanqSoundPos = null
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
        if ((lastPossibleVanqSpawnEntity != null || lastVanqSpawnEntityPos != null || lastVanqSoundPos != null) &&
            lastOwnVanqTime.passedSince() > 5.seconds
        ) {
            lastPossibleVanqSpawnEntity = null
            lastVanqSpawnEntityPos = null
            lastVanqSoundPos = null
        }

        if (vanqSpawnEntity != null && lastOwnVanqTime.passedSince() > 8.seconds) {
            vanqSpawnEntity = null
        }

    }

    @HandleEvent
    fun onWorldChange() {
        vanquishers.clearAnd { it.value.postDespawn() }
        lastPossibleVanqSpawnEntity = null
        lastVanqSpawnEntityPos = null
        lastVanqSoundPos = null
        vanqSpawnEntity = null
    }

    private fun Mob.isOwnVanq(): Boolean {
        val spawnEntity = vanqSpawnEntity ?: return false
        if (baseEntity.distanceTo(spawnEntity) > 4) return false
        if (lastOwnVanqTime.passedSince() > 7.seconds) return false // TODO: actually get good time
        ChatUtils.debug("Expected Own Vanquisher Took ${lastOwnVanqTime.passedSince().format()}")
        return true
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("NautilusApi")
        event.addIrrelevant {
            "vanquishers $vanquishers"
            "lastOwnVanqTime $lastOwnVanqTime"
            "vanqSpawnEntity $vanqSpawnEntity"
            "lastPossibleVanqSpawnEntity $lastPossibleVanqSpawnEntity"
            "lastVanqSpawnEntityPos $lastVanqSpawnEntityPos"
            "lastVanqSpawnEntityTime $lastVanqSpawnEntityTime"
            "lastVanqSoundPos $lastVanqSoundPos"
            "lastVanqSoundTime $lastVanqSoundTime"
        }
    }

}
