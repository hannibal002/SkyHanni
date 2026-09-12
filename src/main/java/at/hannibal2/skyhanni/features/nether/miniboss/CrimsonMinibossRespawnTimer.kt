package at.hannibal2.skyhanni.features.nether.miniboss

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.combat.CrimsonMiniBossEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.level.block.entity.BeaconBlockEntity
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object CrimsonMinibossRespawnTimer {

    private val config get() = SkyHanniMod.feature.crimsonIsle

    private var currentAreaBoss: CrimsonMiniBoss? = null

    private var display: Renderable? = null

    @HandleEvent
    fun onCrimsonMiniBossDeath(event: CrimsonMiniBossEvent.Death) {
        val miniBoss = event.miniBoss
        miniBoss.nextSpawnTime = ServerTimeMark.now() + 2.minutes
        miniBoss.spawned = false
        miniBoss.possibleSpawnTime = null
        miniBoss.foundBeacon = null
        update()
    }

    @HandleEvent
    fun onCrimsonMiniBossSpawning(event: CrimsonMiniBossEvent.Spawning) {
        val miniBoss = event.miniBoss
        miniBoss.spawned = true
        miniBoss.possibleSpawnTime = null
        miniBoss.foundBeacon = null
        update()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onGuiRenderOverlay() {
        if (!config.minibossRespawnTimer) return
        val renderable = display ?: drawDisplay()
        config.minibossTimerPosition.renderRenderable(renderable, posLabel = "Miniboss Timer")
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onSecondPassed() {
        if (!config.minibossRespawnTimer) return
        updateArea()
        update()
    }

    private fun updateArea() {
        CrimsonMiniBoss.entries.forEach {
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
        val now = ServerTimeMark.now()
        currentAreaBoss?.lastSeenArea = now
        val boss = currentAreaBoss ?: return
        if (boss.isTimerKnown()) return

        val isBossInArea = MobData.skyblockMobs.filter {
            it.name == boss.displayName
        }.any { boss.area.isInside(it.baseEntity.blockPosition().toLorenzVec()) }
        if (isBossInArea) {
            boss.spawned = true
            boss.foundBeacon = null
            boss.possibleSpawnTime = null
            return
        }
        boss.spawned = false

        val isThereBeacon = EntityUtils.getAllTileEntities().filter { it is BeaconBlockEntity }.any {
            boss.area.isInside(it.blockPos.toLorenzVec())
        }
        if (boss.foundBeacon == true && !isThereBeacon) {
            boss.foundBeacon = false
            boss.possibleSpawnTime = null
            boss.nextSpawnTime = now + 1.minutes
            return
        }
        if (boss.possibleSpawnTime != null) return
        if (isThereBeacon && boss.foundBeacon == null) {
            boss.foundBeacon = true
            boss.possibleSpawnTime = now + 1.minutes to now + 2.minutes
            return
        }
        if (!isThereBeacon && boss.foundBeacon == null) {
            boss.foundBeacon = false
            boss.possibleSpawnTime = now to now + 1.minutes
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): Renderable {
        val lines = CrimsonMiniBoss.entries.map {
            val timer = it.nextSpawnTime
            val possibleTimer = it.possibleSpawnTime
            Renderable.text(
                buildString {
                    append("§b${it.displayName}: ")
                    if (it.isSpawned()) append("§aSPAWNED!")
                    else when {
                        it.isSpawningSoon() -> append("§6Soon!")
                        it.isTimerKnown() -> append("§e${timer?.timeUntil()?.format()}")
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
        CrimsonMiniBoss.entries.forEach {
            it.nextSpawnTime = null
            it.possibleSpawnTime = null
            it.foundBeacon = null
            it.spawned = null
            it.lastSeenArea = ServerTimeMark.farPast()
        }
        currentAreaBoss = null
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Crimson Isle Miniboss")
        event.addIrrelevant {
            if (!IslandType.CRIMSON_ISLE.isInIsland()) {
                add("Not in Crimson Isle")
                return@addIrrelevant
            }
            add("Current Area Boss: ${currentAreaBoss?.displayName}")
            CrimsonMiniBoss.entries.forEach {
                add("")
                add(it.displayName)
                add("   Timer ${it.nextSpawnTime?.timeUntil()?.format()}")
                add(
                    "   Possible Timer ${it.possibleSpawnTime?.first?.timeUntil()?.format()} - " + "${
                        it.possibleSpawnTime?.second?.timeUntil()?.format()
                    }",
                )
                add("   Found Beacon ${it.foundBeacon}")
                add("   Spawned ${it.spawned}")
                add("   Last Seen Area ${it.lastSeenArea.passedSince().format()}")
            }
        }
    }
}
