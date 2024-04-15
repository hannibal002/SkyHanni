package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

object MiningAPI {

    private val group = RepoPattern.group("data.miningapi")
    private val glaciteAreaPattern by group.pattern("area.glacite", "Glacite Tunnels")
    val coldReset by group.pattern(
        "cold.reset",
        "§cThe warmth of the campfire reduced your §r§b❄ Cold §r§cto 0!|§c ☠ §r§7You froze to death§r§7."
    )

    private var cold = 0
    var lastColdUpdate = SimpleTimeMark.farPast()
    var lastColdReset = SimpleTimeMark.farPast()


    fun inGlaciteArea() = glaciteAreaPattern.matches(HypixelData.skyBlockArea) || inColdIsland()

    fun inColdIsland() = IslandType.DWARVEN_MINES.isInIsland() || IslandType.MINESHAFT.isInIsland()

    fun getCold() = cold

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
        val newCold = event.newList.matchFirst(ScoreboardPattern.coldPattern) {
            group("cold").toInt().absoluteValue
        } ?: return

        if (newCold != cold) {
            updateCold(newCold)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!inColdIsland()) return
        if (coldReset.matches(event.message)) {
            updateCold(0)
            lastColdReset = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (cold != 0) updateCold(0)
        lastColdUpdate = SimpleTimeMark.farPast()
        lastColdReset = SimpleTimeMark.farPast()
    }

    private fun updateCold(newCold: Int) {
        // hypixel sends cold data once in scoreboard even after resetting it
        if (cold == 0 && lastColdUpdate.passedSince() < 1.seconds) return
        lastColdUpdate = SimpleTimeMark.now()
        ColdUpdateEvent(newCold).postAndCatch()
        cold = newCold
    }

}
