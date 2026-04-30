package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.awt.Color
import kotlin.math.abs
import kotlin.math.hypot

@SkyHanniModule
object PhantomleafSolver {

    private val config get() = SkyHanniMod.feature.garden.greenhouse

    private var isSearchingForPhantomleaf = false

    private var lastPos: LorenzVec? = null
    private val candidates = mutableListOf<LorenzVec>()

    private val patternGroup = RepoPattern.group("garden.greenhouse.phantomleaf")

    /**
     * REGEX-TEST: [CROP] Phantomleaf: Poof! Try and find me!
     */
    private val startPattern by patternGroup.pattern("poof", "Phantomleaf: Poof! Try and find me!")

    /**
     * REGEX-TEST: [CROP] Phantomleaf: You found me!
     */
    private val successPattern by patternGroup.pattern("found", "Phantomleaf: You found me!")

    /**
     * REGEX-TEST: [CROP] Phantomleaf: That's not me! Better luck next time!
     */
    private val failPattern by patternGroup.pattern("failure", "Phantomleaf: That's not me! Better luck next time!")

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.phantomleafSolver) return
        if (!isSearchingForPhantomleaf) return

        if (event.pitch !in 0.61..0.62) return
        if (!event.soundName.contains("basedrum")) return

        val currentPos = PlayerUtils.getLocation()

        if (lastPos?.equalsIgnoreY(currentPos) ?: false) {
            val dist = 30.0 * (1.0 - event.volume)
            updateCandidates(currentPos, dist)
            if (candidates.isEmpty())
                ChatUtils.chat("No solutions found. Try moving a little.")
            else if (candidates.size > 1)
                ChatUtils.chat("Multiple candidates found. Move a little to recalculate.")
        }
        
        lastPos = currentPos
    }

    private fun updateCandidates(center: LorenzVec, radius: Double) {
        candidates.clear()
        val rounded = center.blockCenter()
        for (dx in -11..11) {
            for (dz in -11..11) {
                val d = hypot(rounded.x + dx - center.x, rounded.z + dz - center.z)
                if (abs(d - radius) < 0.001)
                    candidates.add(LorenzVec(rounded.x + dx, 74.0, rounded.z + dz))
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.phantomleafSolver) return
        if (!isSearchingForPhantomleaf) return

        candidates.forEach { pos ->
            event.drawWaypointFilled(
                pos.roundToBlock(),
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
            isSearchingForPhantomleaf = true
        } else if (failPattern.find(msg) || successPattern.find(msg)) {
            resetData()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        resetData()
    }

    private fun resetData() {
        isSearchingForPhantomleaf = false
        candidates.clear()
        lastPos = null
    }
}
