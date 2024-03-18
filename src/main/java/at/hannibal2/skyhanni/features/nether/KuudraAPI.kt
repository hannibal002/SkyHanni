package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.KuudraCompleteEvent
import at.hannibal2.skyhanni.events.KuudraEnterEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class KuudraAPI {

    private val group = RepoPattern.group("data.kuudra")
    private val scoreboardGroup = group.group("scoreboard")
    private val chatGroup = group.group("chat")

    private val tierPattern by scoreboardGroup.pattern(
        "tier",
        " §7⏣ §cKuudra's Hollow §8\\(T(?<tier>.*)\\)"
    )

    private val completePattern by chatGroup.pattern(
        "complete",
        "§.\\s*(?:§.)*KUUDRA DOWN!"
    )

    companion object {
        var kuudraTier: Int? = null
        fun inKuudra() = kuudraTier != null
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (kuudraTier == null) {
            for (line in ScoreboardData.sidebarLinesFormatted) {
                tierPattern.matchMatcher(line) {
                    val tier = group("tier").toInt()
                    ChatUtils.chat("a")
                    ChatUtils.chat("Entered kuudra tier $tier")
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
            ChatUtils.chat("completed $tier")
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


