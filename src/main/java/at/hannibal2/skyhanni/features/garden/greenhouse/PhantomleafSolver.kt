package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.awt.Color
import kotlin.math.abs
import kotlin.math.hypot

@SkyHanniModule
object PhantomleafSolver {

    private val config get() = SkyHanniMod.feature.garden.greenhouse

    private const val HYPIXEL_VOLUME_SCALING_FACTOR = 30
    private const val MUTATION_Y_LEVEL = 74.0

    private const val MINIMUM_ALLOWED_PITCH = 0.61
    private const val MAXIMUM_ALLOWED_PITCH = 0.62

    private val SEARCH_RANGE = (-HYPIXEL_VOLUME_SCALING_FACTOR - 1)..(HYPIXEL_VOLUME_SCALING_FACTOR + 1)

    private var isSearching = false

    private var lastPos: LorenzVec? = null
    private val candidates = mutableListOf<LorenzVec>()

    private val patternGroup = RepoPattern.group("garden.greenhouse.phantomleaf")

    /**
     * REGEX-TEST: [CROP] Phantomleaf: Poof! Try and find me!
     */
    private val startPattern by patternGroup.pattern(
        "poof",
        "Phantomleaf: Poof! Try and find me!",
    )

    /**
     * REGEX-TEST: [CROP] Phantomleaf: You found me!
     */
    private val successPattern by patternGroup.pattern(
        "found",
        "Phantomleaf: You found me!",
    )

    /**
     * REGEX-TEST: [CROP] Phantomleaf: That's not me! Better luck next time!
     */
    private val failPattern by patternGroup.pattern(
        "failure",
        "Phantomleaf: " +
            "That's not me! Better luck next time!",
    )

    /**
     * When a note is played with a pitch between 0.61 and 0.62, its volume follows the formula:
     * vol = 1 - dist / 30
     * where dist is the distance from the center of the phantomleaf hiding spot.
     * We can check all nearby positions to see which ones have a distance that matches
     * the expected distance based on the sound.
     */
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.phantomleafSolver) return
        if (!isSearching) return

        if (event.pitch !in MINIMUM_ALLOWED_PITCH..MAXIMUM_ALLOWED_PITCH) return
        if (event.soundName != "block.note_block.basedrum") return

        val currentPos = LocationUtils.playerLocation()

        if (lastPos?.equalsIgnoreY(currentPos) ?: false) {
            val dist = HYPIXEL_VOLUME_SCALING_FACTOR * (1.0 - event.volume)
            updateCandidates(currentPos, dist)
            if (candidates.isEmpty()) {
                ChatUtils.chat("No solutions found. Try moving a little.")
            } else if (candidates.size > 1) {
                ChatUtils.chat("Multiple candidates found. Move a little to recalculate.")
            }
        }

        lastPos = currentPos
    }

    /**
     * Given a center position and a radius from that center, find all candidate blocks
     * near the center with that exact distance from the center (within 0.001 tolerance)
     */
    private fun updateCandidates(center: LorenzVec, radius: Double) {
        candidates.clear()
        val rounded = center.blockCenter()
        for (dx in SEARCH_RANGE) {
            for (dz in SEARCH_RANGE) {
                // calculate distance from candidate (rounded.x + dx, rounded.z + dz) to center
                val d = hypot(rounded.x + dx - center.x, rounded.z + dz - center.z)
                if (abs(d - radius) < 0.001) {
                    candidates.add(LorenzVec(rounded.x + dx, MUTATION_Y_LEVEL, rounded.z + dz).roundToBlock())
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.phantomleafSolver) return
        if (!isSearching) return

        candidates.forEach { pos ->
            event.drawWaypointFilled(
                pos,
                if (candidates.size > 1) Color.YELLOW else Color.GREEN,
                seeThroughBlocks = true,
                beacon = true,
            )
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.phantomleafSolver) return

        val msg = event.cleanMessage
        if (startPattern.find(msg)) {
            isSearching = true
        } else if (failPattern.find(msg) || successPattern.find(msg)) {
            resetData()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        resetData()
    }

    private fun resetData() {
        isSearching = false
        candidates.clear()
        lastPos = null
    }
}
