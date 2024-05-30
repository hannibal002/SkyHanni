package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.find
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CompactBingoChat {

    private val config get() = SkyHanniMod.feature.event.bingo.compactChat

    private var inSkillLevelUp = false
    private var inSkyBlockLevelUp = false
    private var inCollectionLevelUp = false
    private var collectionLevelUpLastLine: String? = null
    private var newArea = 0 // 0 = nothing, 1 = after first message, 2 = after second message

    private val patternGroup = RepoPattern.group("bingo.compactchat.new")
    private val borderPattern by patternGroup.pattern(
        "border",
        "§[e3]§l▬{64}"
    )
    private val inSkyblockPatterns by patternGroup.list(
        "inskyblock",
        "(^\\s{3}§r§7§8\\+§a. §c❁ Strength\\s*$)",
        "(^\\s{3}§r§7§8\\+§a.* §c❤ Health\\s*$)",
        "(^ {3}§r§7§bAccess to Community Shop\\s*$)",
        "(^ {3}§r§7§8\\+§aAuto-pickup block and mob drops\\s*$)",
        "(^ §r§7§6Access to Bazaar\\s*$)",
        "(^ §r§a§lREWARDS\\s*$)\n"
    )
    private val skillLevelUpPattern by patternGroup.pattern(
        "skilllevelup",
        " ^{2}§r§b§lSKILL LEVEL UP"
    )
    private val tradeRecipePattern by patternGroup.pattern(
        "traderecipe",
        "Trade|Recipe"
    )
    private val enchantPattern by patternGroup.pattern(
        "enchant",
        "Access to.* Enchantment\$"
    )
    private val collectionLevelUpPattern by patternGroup.pattern(
        "collection",
        " ^{2}§r§6§lCOLLECTION LEVEL UP"
    )
    private val skyblockLevelUpPattern by patternGroup.pattern(
        "enchant",
        " ^{2}§r§3§lSKYBLOCK LEVEL UP §bLevel"
    )
    private val newAreaDiscoverdPattern by patternGroup.pattern(
        "newareadiscovered",
        " §r§6§lNEW AREA DISCOVERED!"
    )
    private val newAreaPattern by patternGroup.pattern(
        "newarea",
        "^(§7\\s*■\\s*§r|\\s*§r.*)"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.isBingoProfile && !config.outsideBingo) return

        val message = event.message
        borderPattern.matchMatcher(message) {
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

    // TODO USE SH-REPO
    private fun onSkillLevelUp(message: String): Boolean {
        if (skillLevelUpPattern.find(message)) {
            inSkillLevelUp = true
            return false
        }


        if (inSkillLevelUp && !enchantPattern.matches(message)) {
            return true
        }

        return false
    }

    private fun onSkyBlockLevelUp(message: String): Boolean {
        if (skyblockLevelUpPattern.find(message)) {
            inSkyBlockLevelUp = true
            return false
        }


        if (inSkyBlockLevelUp && inSkyblockPatterns.any { it.matches(message) }) return true

        return false
    }

    private fun onCollectionLevelUp(message: String): Boolean {
        if (collectionLevelUpPattern.find(message)) {
            inCollectionLevelUp = true
            return false
        }

        if (inCollectionLevelUp) {
            if (tradeRecipePattern.matches(message)) {
                val text = message.removeColor().replace(" ", "")
                if (text == "Trade" || text == "Recipe") {
                    collectionLevelUpLastLine?.let { ChatUtils.chat(it, false) }
                }
            } else {
                collectionLevelUpLastLine = message
                return true
            }
        }

        return false
    }

    private fun onNewAreaDiscovered(message: String): Boolean {
        if (newAreaDiscoverdPattern.matches(message)) {
            newArea = 1
            return false
        }

        if (message != "") {
            if (newArea == 1) {
                newArea = 2
                return false
            }

            if (newArea == 2) {
                if (newAreaPattern.matches(message)) {
                    return true
                } else {
                    newArea = 0
                }
            }
        }
        return false
    }
}
