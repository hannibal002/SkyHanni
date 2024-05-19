package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
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

    private val patternGroup = RepoPattern.group("bingo.compactchat")
    private val healthPattern by patternGroup.pattern(
        "health",
        " {3}§r§7§8\\+§a.* §c❤ Health"
    )
    private val strengthPattern by patternGroup.pattern(
        "strength",
        " {3}§r§7§8\\+§a. §c❁ Strength"
    )
    private val borderPattern by patternGroup.pattern(
        "border",
        "§[e3]§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
    )
    private val skillLevelUpPattern by patternGroup.pattern(
        "levelup",
        " {2}§r§b§lSKILL LEVEL UP.*"
    )
    private val skyblockLevelUpPattern by patternGroup.pattern(
        "sblevelup",
        " {2}§r§3§lSKYBLOCK LEVEL UP §bLevel.*"
    )
    private val rewardsPattern by patternGroup.pattern(
        "rewards",
        " {2}§r§a§lREWARDS"
    )
    private val accessToBazaarPattern by patternGroup.pattern(
        "bazaar",
        " {3}§r§7§6Access to Bazaar"
    )
    private val accessToCommunityShopPattern by patternGroup.pattern(
        "communityshop",
        " {3}§r§7§bAccess to Community Shop"
    )
    private val autoPickupPattern by patternGroup.pattern(
        "pickup",
        " {3}§r§7§8+§aAuto-pickup block and mob drops"
    )
    private val collectionLevelUpPattern by patternGroup.pattern(
        "collectionlevelup",
        " {2}§r§6§lCOLLECTION LEVEL UP.*"
    )
    private val newAreaPattern by patternGroup.pattern(
        "newarea",
        " §r§6§lNEW AREA DISCOVERED!"
    )
    private val newAreaStartsWithPattern by patternGroup.pattern(
        "newareastartswith",
        "^(§7 {3}■ §r| {5}§r| {3}§r).*"
    )
    private val containsTradeRecipePattern by patternGroup.pattern(
        "containstraderecipe",
        ".*(Trade|Recipe).*"
    )
    private val exactTradeRecipePattern by patternGroup.pattern(
        "exacttraderecipe",
        "^(Trade|Recipe)$"
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
        skillLevelUpPattern.matchMatcher(message) {
            inSkillLevelUp = true
            return false
        }

        //TODO this doesn't seem to be the right message
        return inSkillLevelUp && !message.contains("Access to") && !message.endsWith(" Enchantment")
    }

    private fun onSkyBlockLevelUp(message: String): Boolean {
        skyblockLevelUpPattern.matchMatcher(message) {
            inSkyBlockLevelUp = true
            return false
        }

        if (inSkyBlockLevelUp) {
            rewardsPattern.matchMatcher(message) {
                return true
            }
            // We don't care about extra health & strength
            healthPattern.matchMatcher(message) {
                return true
            }
            strengthPattern.matchMatcher(message) {
                return true
            }
            // No Bazaar and Community Shop in bingo
            accessToBazaarPattern.matchMatcher(message) {
                return true
            }
            accessToCommunityShopPattern.matchMatcher(message) {
                return true
            }
            // Always enabled in bingo
            autoPickupPattern.matchMatcher(message) {
                return true
            }
        }
        return false
    }

    private fun onCollectionLevelUp(message: String): Boolean {
        collectionLevelUpPattern.matchMatcher(message) {
            inCollectionLevelUp = true
            return false
        }

        if (inCollectionLevelUp) {
            if (containsTradeRecipePattern.matches(message)) {
                val text = message.removeColor().replace(" ", "")
                if (exactTradeRecipePattern.matches(text)) {
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
        newAreaPattern.matchMatcher(message) {
            newArea = 1
            return false
        }

        if (message != "") {
            if (newArea == 1) {
                newArea = 2
                return false
            }

            if (newArea == 2) {
                if (newAreaStartsWithPattern.matches(message)) {
                    return true
                } else {
                    newArea = 0
                }
            }
        }
        return false
    }
}
