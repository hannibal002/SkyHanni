package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CompactBingoChat {

    private var blockedSkillLevelUp = false
    private var blockedCollectionLevelUp = false
    private var collectionLevelUpLastLine: String? = null
    private var newArea = 0//0 = nothing, 1 = after first message, 2 = after second message
    private var blockedBestiarity = false

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!SkyHanniMod.feature.bingo.compactChatMessages) return

        onSkillLevelUp(event)
        onCollectionLevelUp(event)
        onNewAreaDiscovered(event)
        onBestiarityUpgrade(event)
    }

    private fun onSkillLevelUp(event: LorenzChatEvent) {
        val message = event.message
        if (message.startsWith("  §r§b§lSKILL LEVEL UP ")) {
            blockedSkillLevelUp = true
            return
        }
        if (message == "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") {
            blockedSkillLevelUp = false
            return
        }

        if (blockedSkillLevelUp) {
            if (!message.contains("Access to") && !message.endsWith(" Enchantment")) {
                event.blockedReason = "compact skill level up"
            }
        }
    }

    private fun onCollectionLevelUp(event: LorenzChatEvent) {
        val message = event.message
        if (message.startsWith("  §r§6§lCOLLECTION LEVEL UP ")) {
            blockedCollectionLevelUp = true
            return
        }
        if (message == "§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") {
            blockedCollectionLevelUp = false
            return
        }

        if (blockedCollectionLevelUp) {
            if (message.contains("Trade") || message.contains("Recipe")) {
                var text = message.removeColor().replace(" ", "")
                if (text == "Trade" || text == "Recipe") {
                    collectionLevelUpLastLine?.let { LorenzUtils.chat(it) }
                }
            } else {
                event.blockedReason = "compact collection level up"
                collectionLevelUpLastLine = message
            }
        }
    }

    private fun onNewAreaDiscovered(event: LorenzChatEvent) {
        var message = event.message

        if (message == " §r§6§lNEW AREA DISCOVERED!") {
            newArea = 1
            println("new area $newArea $message")
            return
        }

        if (message != "") {
            if (newArea == 1) {
                newArea = 2
                println("new area $newArea $message")
                return
            }

            if (newArea == 2) {
                if (message.startsWith("§7   ■ §r") || message.startsWith("     §r")) {
                    event.blockedReason = "compact new area discovered"
                } else {
                    newArea = 0
                    println("new area $newArea $message")
                }
            }
        }
    }

    private fun onBestiarityUpgrade(event: LorenzChatEvent) {
        val message = event.message
        if (message.startsWith("  §r§3§lBESTIARY §b§l")) {
            blockedBestiarity = true
            return
        }
        if (message == "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") {
            blockedBestiarity = false
            return
        }

        if (blockedBestiarity) {
            event.blockedReason = "compact bestiarity upgrade"
        }
    }
}