package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.DropDetails
import at.hannibal2.skyhanni.data.jsonobjects.repo.SlayerDropsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeWordsAtEnd
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern

@SkyHanniModule
object DropChances {

    private val patternGroup = RepoPattern.group("slayer.drop")

    /**
     * REGEX-TEST: §r§c§lINSANE DROP! §r§7(§r§5Ender Slayer VII§r§7) §r§b(+210% §r§b✯ Magic Find§r§b)§r
     */
    private val insaneDropPattern by patternGroup.pattern(
        "insane",
        "§c§lINSANE DROP! §r§7\\((?<itemName>.*?)\\) §r§b\\(\\+(?<magicFind>\\d+)% §r§b✯ Magic Find§r§b\\)"
    )

    /**
     * REGEX-TEST: §r§5§lVERY RARE DROP! §r§7(§r§fSmarty Pants I§r§7) §r§b(+253% §r§b✯ Magic Find§r§b)§r
     * REGEX-TEST: §r§5§lVERY RARE DROP! §r§7(§r§f§r§5◆ End Rune I§r§7) §r§b(+253% §r§b✯ Magic Find§r§b)§r
     */
    private val purpleVeryRareDropPattern by patternGroup.pattern(
        "purple-very-rare",
        "§5§lVERY RARE DROP! §r§7\\((?<itemName>.*?)\\) §r§b\\(\\+(?<magicFind>\\d+)% §r§b✯ Magic Find§r§b\\)"
    )

    /**
     * REGEX-TEST: §r§b§lRARE DROP! §r§7(§r§f§r§762x §r§f§r§aTwilight Arrow Poison§r§7) §r§b(+253% §r§b✯ Magic Find§r§b)§r
     */
    private val rareDropPattern by patternGroup.pattern(
        "rare",
        "§b§lRARE DROP! §r§7\\((?<itemName>.*?)\\) §r§b\\(\\+(?<magicFind>\\d+)% §r§b✯ Magic Find§r§b\\)"
    )

    /**
     * REGEX-TEST: §r§9§lVERY RARE DROP! §r§7(§r§fMana Steal I§r§7) §r§b(+191% §r§b✯ Magic Find§r§b)§r
     */
    private val blueVeryRareDropPattern by patternGroup.pattern(
        "blue-very-rare",
        "§9§lVERY RARE DROP! §r§7\\((?<itemName>.*?)\\) §r§b\\(\\+(?<magicFind>\\d+)% §r§b✯ Magic Find§r§b\\)"
    )

    /**
     * REGEX-TEST: §r§d§lCRAZY RARE DROP! §r§7(§r§f§r§6Judgement Core§r§7) §r§b(+171% §r§b? Magic Find§r§b)§r
     */
    private val crazyRareDropPattern by patternGroup.pattern(
        "crazy-rare",
        "§d§lCRAZY RARE DROP! §r§7\\((?<itemName>.*?)\\) §r§b\\(\\+(?<magicFind>\\d+)% §r§b\\? Magic Find§r§b\\)"
    )

    private val reg = "(\\d+)x (.+)".toRegex()

    private var drops: Map<String, Map<String, Map<String, DropDetails>>> = emptyMap()

    @HandleEvent
    fun onRepoReload(e: RepositoryReloadEvent) {
        drops = e.getConstant<SlayerDropsJson>("rng_meter/slayer/Voidgloom").drops
    }

    @HandleEvent
    fun onChat(e: SkyHanniChatEvent) {
        for (pattern: Pattern in listOf(
            insaneDropPattern,
            purpleVeryRareDropPattern,
            rareDropPattern,
            blueVeryRareDropPattern,
            crazyRareDropPattern
        )) {
            pattern.matchMatcher(e.message) {
                var itemName: String = group("itemName")
                if (reg.find(itemName) != null) {
                    val (amm, cleanName) = reg.find(itemName)!!.destructured
                    itemName = cleanName
                }
                itemName = itemName.trim()
                val magicFind: String = group("magicFind")
                val unformattedItemName: String = itemName.removeColor()
                val internalName: NeuInternalName = NeuInternalName.fromItemNameOrNull(unformattedItemName) ?: itemName.toInternalName()
            }
        }
    }

    fun getTotalWeight(dropsJson: SlayerDropsJson) {
    }

    fun getModifiedWeight(dropsJson: SlayerDropsJson) {
    }

    fun getRngMeterModifier(drops: Map<String, Map<String, Map<String, DropDetails>>>) {
        val storage = getStorage() ?: return

        val selectedDrop = NeuInternalName.fromItemNameOrNull(storage.itemGoal) ?: return
        val currentXp = storage.currentMeter

        val dropDetails = getDropDetails(drops, selectedDrop) ?: return

        
    }

    private fun getDropDetails(drops: Map<String, Map<String, Map<String, DropDetails>>>, internalDropName: NeuInternalName): DropDetails? {
        for (table in drops.values) {
            for (drop in table.values) {
                if (drop.containsKey(internalDropName.asString())) {
                    return drop[internalDropName.asString()]
                }
            }
        }
        return null
    }

    private fun getStorage(): ProfileSpecificStorage.SlayerRngMeterStorage? {
        return ProfileStorageData.profileSpecific?.slayerRngMeter?.getOrPut(getCurrentSlayer()) {
            ProfileSpecificStorage.SlayerRngMeterStorage()
        }
    }

    private fun getCurrentSlayer() = SlayerApi.latestSlayerCategory.removeWordsAtEnd(1).removeColor()
}
