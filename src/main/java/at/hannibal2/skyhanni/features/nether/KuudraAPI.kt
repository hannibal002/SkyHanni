package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.KuudraCompleteEvent
import at.hannibal2.skyhanni.events.KuudraEnterEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object KuudraAPI {

    private val patternGroup = RepoPattern.group("data.kuudra")

    private val tierPattern by patternGroup.pattern(
        "scoreboard.tier",
        " §7⏣ §cKuudra's Hollow §8\\(T(?<tier>.*)\\)"
    )
    private val completePattern by patternGroup.pattern(
        "chat.complete",
        "§.\\s*(?:§.)*KUUDRA DOWN!"
    )

    var kuudraTier: Int? = null
    fun inKuudra() = kuudraTier != null

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (kuudraTier == null) {
            for (line in ScoreboardData.sidebarLinesFormatted) {
                tierPattern.matchMatcher(line) {
                    val tier = group("tier").toInt()
                    kuudraTier = tier
                    KuudraEnterEvent(tier).postAndCatch()
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        kuudraTier = null
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message
        completePattern.matchMatcher(message) {
            val tier = kuudraTier ?: return
            KuudraCompleteEvent(tier).postAndCatch()
        }
    }

    class KuudraTier(
        val name: String,
        val displayItem: NEUInternalName,
        val location: LorenzVec?,
        val tierNumber: Int,
        var doneToday: Boolean = false,
    ) {
        fun getDisplayName() = "Tier $tierNumber ($name)"
    }
}
