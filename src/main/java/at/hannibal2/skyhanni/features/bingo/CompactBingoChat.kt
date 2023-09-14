package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CompactBingoChat {
    private val config get() = SkyHanniMod.feature.bingo.compactChat

    private var inSkillLevelUp = false
    private var inSkyBlockLevelUp = false
    private var inCollectionLevelUp = false
    private var collectionLevelUpLastLine: String? = null
    private var newArea = 0//0 = nothing, 1 = after first message, 2 = after second message
    private val healthPattern = "   §r§7§8\\+§a.* §c❤ Health".toPattern()
    private val strengthPattern = "   §r§7§8\\+§a. §c❁ Strength".toPattern()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.isBingoProfile && !config.outsideBingo) return

        val message = event.message
        if (message == "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬" ||
            message == "§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        ) {
            inSkillLevelUp = false
            inSkyBlockLevelUp = false
            inCollectionLevelUp = false
            if (config.hideBorder) {
                event.blockedReason = "compact_bingo_border"
            }
            return
        }

        if (onSkillLevelUp(message)) event.blockedReason = "compact_skill_level_up"
        if (onSkyBlockLevelUp(message)) event.blockedReason = "compact_skyblock_level_up"
        if (onCollectionLevelUp(message)) event.blockedReason = "compact_collection_level_up"
        if (onNewAreaDiscovered(message)) event.blockedReason = "compact_new_area_discovered"
    }

    private fun onSkillLevelUp(message: String): Boolean {
        if (message.startsWith("  §r§b§lSKILL LEVEL UP ")) {
            inSkillLevelUp = true
            return false
        }

        if (inSkillLevelUp) {
            if (!message.contains("Access to") && !message.endsWith(" Enchantment")) {
                return true
            }
        }

        return false
    }

    private fun onSkyBlockLevelUp(message: String): Boolean {
        if (message.startsWith("  §r§3§lSKYBLOCK LEVEL UP §bLevel ")) {
            inSkyBlockLevelUp = true
            return false
        }

        if (inSkyBlockLevelUp) {
            if (message == "  §r§a§lREWARDS") return true
            // We don't care about extra health & strength
            healthPattern.matchMatcher(message) {
                return true
            }
            strengthPattern.matchMatcher(message) {
                return true
            }

            // No Bazaar and Community Shopin bingo
            if (message == "   §r§7§6Access to Bazaar") return true
            if (message == "   §r§7§bAccess to Community Shop") return true

            // Always enabled in bingo
            if (message == "   §r§7§8+§aAuto-pickup block and mob drops") return true
        }

        return false
    }

    private fun onCollectionLevelUp(message: String): Boolean {
        if (message.startsWith("  §r§6§lCOLLECTION LEVEL UP ")) {
            inCollectionLevelUp = true
            return false
        }

        if (inCollectionLevelUp) {
            if (message.contains("Trade") || message.contains("Recipe")) {
                val text = message.removeColor().replace(" ", "")
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
            return false
        }

        if (message != "") {
            if (newArea == 1) {
                newArea = 2
                return false
            }

            if (newArea == 2) {
                if (message.startsWith("§7   ■ §r") || message.startsWith("     §r") || message.startsWith("   §r")) {
                    return true
                } else {
                    newArea = 0
                }
            }
        }
        return false
    }
}