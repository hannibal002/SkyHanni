package at.hannibal2.skyhanni.features.nether.miniboss

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.combat.CrimsonMinibossEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.level.block.entity.BeaconBlockEntity
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CrimsonMinibossRespawnTimer {

    private val config get() = SkyHanniMod.feature.crimsonIsle

    private var currentAreaBoss: CrimsonMiniBoss? = null

    private var display: Renderable? = null

    private val respawnData: Map<CrimsonMiniBoss, MiniBossRespawnData> = CrimsonMiniBoss.entries.associateWith { MiniBossRespawnData() }

    private data class MiniBossRespawnData(
        var nextSpawnTime: SimpleTimeMark? = null,
        var possibleSpawnTime: Pair<SimpleTimeMark, SimpleTimeMark>? = null,
        var foundBeacon: Boolean? = null,
        var spawned: Boolean? = null,
        var lastSeenArea: SimpleTimeMark = SimpleTimeMark.farPast(),
    ) {
        fun isTimerKnown(): Boolean {
            val timer = nextSpawnTime ?: return false
            return timer.passedSince() < 2.minutes + 5.seconds
        }

        fun isSpawningSoon(): Boolean {
            if (spawned == true) return false
            val timer = nextSpawnTime ?: return false
            return timer.passedSince() in 0.seconds..10.seconds
        }

        fun isSpawned(): Boolean {
            if (spawned == true) return true
            val timer = nextSpawnTime ?: return false
            return (timer.passedSince() - 2.minutes) in 0.seconds..20.seconds
        }
    }

    @HandleEvent
    fun onCrimsonMiniBossSpawning(event: CrimsonMinibossEvent) {
        if (!isEnabled()) return
        val data = respawnData[event.miniboss] ?: return
        data.spawned = true
        data.possibleSpawnTime = null
        data.foundBeacon = null
        update()
    }

    @HandleEvent
    fun onCrimsonMiniBossDeath(event: CrimsonMinibossEvent) {
        if (!isEnabled()) return
        val data = respawnData[event.miniboss] ?: return
        data.nextSpawnTime = SimpleTimeMark.now() + 2.minutes
        data.spawned = false
        data.possibleSpawnTime = null
        data.foundBeacon = null
        update()
    }

    @HandleEvent
    fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        val renderable = display ?: drawDisplay()
        config.minibossTimerPosition.renderRenderable(renderable, posLabel = "Miniboss Timer")
    }

    @HandleEvent
    fun onSecondPassed() {
        if (!isEnabled()) return
        updateArea()
        update()
    }

    private fun updateArea() {
        respawnData.values.forEach {
            if (it.lastSeenArea.passedSince() > 2.minutes) {
                it.nextSpawnTime = null
                it.possibleSpawnTime = null
                it.foundBeacon = null
                it.spawned = null
            }
        }
        currentAreaBoss = CrimsonMiniBoss.entries.firstOrNull {
            it.area.isPlayerInside()
        }
        val boss = currentAreaBoss ?: return
        val data = respawnData[boss] ?: return
        val now = SimpleTimeMark.now()
        data.lastSeenArea = now
        if (data.isTimerKnown()) return

        val isBossInArea = MobData.skyblockMobs.filter {
            it.name == boss.displayName
        }.any { boss.area.isInside(it.baseEntity.blockPosition().toLorenzVec()) }
        if (isBossInArea) {
            data.spawned = true
            data.foundBeacon = null
            data.possibleSpawnTime = null
            return
        }
        data.spawned = false

        val isThereBeacon = EntityUtils.getAllTileEntities().filter { it is BeaconBlockEntity }.any {
            boss.area.isInside(it.blockPos.toLorenzVec())
        }
        if (data.foundBeacon == true && !isThereBeacon) {
            data.foundBeacon = false
            data.possibleSpawnTime = null
            data.nextSpawnTime = now + 1.minutes
            return
        }
        if (data.possibleSpawnTime != null) return
        if (isThereBeacon && data.foundBeacon == null) {
            data.foundBeacon = true
            data.possibleSpawnTime = now + 1.minutes to now + 2.minutes
            return
        }
        if (!isThereBeacon && data.foundBeacon == null) {
            data.foundBeacon = false
            data.possibleSpawnTime = now to now + 1.minutes
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): Renderable {
        val lines = respawnData.entries.map { (miniboss, data) ->
            val timer = data.nextSpawnTime
            val possibleTimer = data.possibleSpawnTime
            Renderable.text(
                buildString {
                    append("§b${miniboss.displayName}: ")
                    if (data.isSpawned()) append("§aSPAWNED!")
                    else when {
                        data.isSpawningSoon() -> append("§6Soon!")
                        data.isTimerKnown() -> append("§e${timer?.timeUntil()?.format()}")
                        possibleTimer != null -> {
                            val (start, end) = possibleTimer
                            if (start.timeUntil().isNegative()) append("§e~Now - ")
                            else append("§e~${start.timeUntil().format()} - ")
                            if (end.timeUntil().isNegative()) append("§eNow")
                            else append("§e${end.timeUntil().format()}")
                        }

                        else -> append("§cUnknown")
                    }
                },
            )
        }
        return Renderable.vertical(lines)
    }

    @HandleEvent
    fun onWorldChange() {
        respawnData.values.forEach {
            it.nextSpawnTime = null
            it.possibleSpawnTime = null
            it.foundBeacon = null
            it.spawned = null
            it.lastSeenArea = SimpleTimeMark.farPast()
        }
        currentAreaBoss = null
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Crimson Isle Miniboss")
        event.addIrrelevant {
            if (!isEnabled()) {
                add("Feature is Disabled")
                return@addIrrelevant
            }
            add("Current Area Boss: ${currentAreaBoss?.displayName}")
            respawnData.entries.forEach { (miniboss, data) ->
                add("")
                add(miniboss.displayName)
                add("   Timer ${data.nextSpawnTime?.timeUntil()?.format()}")
                add(
                    "   Possible Timer ${data.possibleSpawnTime?.first?.timeUntil()?.format()} - " + "${
                        data.possibleSpawnTime?.second?.timeUntil()?.format()
                    }",
                )
                add("   Found Beacon ${data.foundBeacon}")
                add("   Spawned ${data.spawned}")
                add("   Last Seen Area ${data.lastSeenArea.passedSince().format()}")
            }
        }
    }

    private fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.minibossRespawnTimer
}
