package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.LorenzUtils
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
        "§cThe warmth of the campfire reduced your §r§b❄ Cold §r§cto 0!"
    )

    private var cold = 0
    var lastColdUpdate = SimpleTimeMark.farPast()


    fun inGlaciteArea() = glaciteAreaPattern.matches(HypixelData.skyBlockArea) || IslandType.MINESHAFT.isInIsland()

    fun getCold() = cold

    @SubscribeEvent
    fun onScoreboardChangeEvent(event: ScoreboardChangeEvent) {
        val newCold = event.newList.matchFirst(ScoreboardPattern.coldPattern) {
            group("cold").toInt().absoluteValue
        } ?: return

        if (newCold != cold) {
            updateCold(newCold)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (coldReset.matches(event.message)) {
            updateCold(0)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        updateCold(0)
        lastColdUpdate = SimpleTimeMark.farPast()
    }

    private fun updateCold(newCold: Int) {
        if (cold == 0 && lastColdUpdate.passedSince() < 1.seconds) return
        lastColdUpdate = SimpleTimeMark.now()
        ColdUpdateEvent(newCold).postAndCatch()
        cold = newCold
    }

}
