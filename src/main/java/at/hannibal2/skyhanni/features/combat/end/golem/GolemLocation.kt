package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.renderBeaconBeam
import net.minecraft.world.level.block.Blocks
import kotlin.time.Duration.Companion.seconds

/**
 * Knows the six fixed spawn points of the End Stone Protector, names the one it rises at and
 * marks them in the world: a beacon beam on the head structure and a highlighted block where
 * the golem actually appears.
 */
@SkyHanniModule
object GolemLocation {

    private val config get() = SkyHanniMod.feature.combat.endIsland.golem

    internal const val GOLEM_NAME = "Endstone Protector"

    /** How far a golem may be from a known point and still count as that point. */
    private const val MATCH_RADIUS = 12.0

    /** Blocks above the head position that are checked for the golem's head. */
    private const val HEAD_SCAN_HEIGHT = 4

    private val HEAD_BLOCKS = setOf(Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD)

    /**
     * @param head the head structure the beam rises from
     * @param spawn the block the golem appears at, as block corner coordinates
     */
    data class SpawnPoint(val label: String, val head: LorenzVec, val spawn: LorenzVec)

    /**
     * Measured in game. The spawn coordinates were given as block centres (x.5 / z.5) and are
     * stored floored here, because the renderer draws from the block corner.
     */
    private val spawnPoints = listOf(
        SpawnPoint("Front", LorenzVec(-644.0, 5.0, -269.0), LorenzVec(-645.0, 8.0, -271.0)),
        SpawnPoint("Center", LorenzVec(-689.0, 5.0, -273.0), LorenzVec(-690.0, 8.0, -275.0)),
        SpawnPoint("Right Front", LorenzVec(-639.0, 5.0, -328.0), LorenzVec(-640.0, 8.0, -330.0)),
        SpawnPoint("Left", LorenzVec(-649.0, 5.0, -219.0), LorenzVec(-650.0, 8.0, -221.0)),
        SpawnPoint("Right Back", LorenzVec(-678.0, 5.0, -332.0), LorenzVec(-679.0, 8.0, -334.0)),
        SpawnPoint("Back", LorenzVec(-727.0, 5.0, -284.0), LorenzVec(-728.0, 8.0, -286.0)),
    )

    private var activePoint: SpawnPoint? = null

    /** Name of the spawn point the golem is currently at, null while none is known. */
    fun currentLocationText(): String? = activePoint?.label

    /**
     * The golem's head sits in the ground as a player head block long before it rises, so the
     * spawn point can be named immediately instead of only once the entity exists.
     */
    private fun detectFromHeadBlock(): SpawnPoint? = spawnPoints.firstOrNull { point ->
        (0..HEAD_SCAN_HEIGHT).any { dy ->
            val position = point.head.up(dy.toDouble())
            position.isInLoadedChunk() && position.getBlockAt() in HEAD_BLOCKS
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onSecondPassed(event: SecondPassedEvent) {
        // Only ever set, never cleared here: the head block disappears once the golem rises,
        // but the location stays relevant for the whole fight.
        detectFromHeadBlock()?.let { activePoint = it }
    }

    /** Fallback for the case the head block was missed, e.g. when the chunk loaded late. */
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (activePoint != null) return
        val mob = event.mob
        if (mob.name != GOLEM_NAME) return

        val position = mob.baseEntity.getLorenzVec()
        activePoint = spawnPoints.minByOrNull { it.spawn.distance(position) }
            ?.takeIf { it.spawn.distance(position) <= MATCH_RADIUS }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob.name != GOLEM_NAME) return
        activePoint = null
    }

    /**
     * The waypoints are only useful while a protector is on its way: during the spawn countdown,
     * or when joining late into an already fully awoken protector.
     */
    private fun shouldShowWaypoints() =
        GolemSpawnTimer.timeUntilSpawn() > 0.seconds || GolemStage.isFullyAwoken

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.highlightSpawnPoints) return
        if (!shouldShowWaypoints()) return
        // Only the point the head block actually sits at - the other five are not going to be
        // used by this protector and would just clutter the view.
        val point = activePoint ?: return

        event.drawColor(point.spawn, config.activeSpawnColor, beacon = false, alpha = 0.8f)
        event.drawString(point.spawn.blockCenter(), "§dSpawn", seeThroughBlocks = true)
        // Beam only - the head structure block itself stays untinted.
        event.renderBeaconBeam(point.head, config.activeSpawnColor.toColor())
    }

    @HandleEvent
    private fun onIslandChange(event: IslandChangeEvent) {
        activePoint = null
    }
}
