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

        if (onSkillLevelUp(event.message)) event.blockedReason = "compact_skill_level_up"
        if (onCollectionLevelUp(event.message)) event.blockedReason = "compact_collection_level_up"
        if (onNewAreaDiscovered(event.message)) event.blockedReason = "compact_new_area_discovered"
        if (onBestiarityUpgrade(event.message)) event.blockedReason = "compact_skill_level_up"
    }

    private fun onSkillLevelUp(message: String): Boolean {
        if (message.startsWith("  §r§b§lSKILL LEVEL UP ")) {
            blockedSkillLevelUp = true
            return false
        }
        if (message == "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") {
            blockedSkillLevelUp = false
            return true
        }

        if (blockedSkillLevelUp) {
            if (!message.contains("Access to") && !message.endsWith(" Enchantment")) {
                return true
            }
        }

        return false
    }

    private fun onCollectionLevelUp(message: String): Boolean {
        if (message.startsWith("  §r§6§lCOLLECTION LEVEL UP ")) {
            blockedCollectionLevelUp = true
            return false
        }
        if (message == "§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") {
            blockedCollectionLevelUp = false
            return true
        }

        if (blockedCollectionLevelUp) {
            if (message.contains("Trade") || message.contains("Recipe")) {
                var text = message.removeColor().replace(" ", "")
                if (text == "Trade" || text == "Recipe") {
                    collectionLevelUpLastLine?.let { LorenzUtils.chat(it) }
                }
            } else {
                collectionLevelUpLastLine = message
                return true
            }
        }

        return false
    }

    private fun onNewAreaDiscovered(message: String): Boolean {
        if (message == " §r§6§lNEW AREA DISCOVERED!") {
            newArea = 1
            println("new area $newArea $message")
            return false
        }

        if (message != "") {
            if (newArea == 1) {
                newArea = 2
                println("new area $newArea $message")
                return false
            }

            if (newArea == 2) {
                if (message.startsWith("§7   ■ §r") || message.startsWith("     §r")) {
                    return true
                } else {
                    newArea = 0
                    println("new area $newArea $message")
                }
            }
        }
        return false
    }

    private fun onBestiarityUpgrade(message: String): Boolean {
        if (message.contains("§r§6§lBESTIARY MILESTONE")) return false

        if (message.startsWith("  §r§3§lBESTIARY §b§l")) {
            blockedBestiarity = true
            return false
        }
        if (message == "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") {
            blockedBestiarity = false
            return true
        }

        return blockedBestiarity
    }
}