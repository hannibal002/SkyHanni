package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.DropDetails
import at.hannibal2.skyhanni.data.jsonobjects.repo.SlayerDropsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.ChatComponentText
import scala.Console
import java.util.regex.Pattern

@SkyHanniModule
@Suppress("UnusedParameter", "UnusedPrivateProperty", "FunctionOnlyReturningConstant")
object DropChances {

    private val patternGroup = RepoPattern.group("slayer.drop")

    private val storage get() = ProfileStorageData.profileSpecific?.slayerRngMeter

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
     * REGEX-TEST: §r§9§lVERY RARE DROP! §r§7(§r§f§r§9Null Atom§r§7) §r§b(+253% §r§b✯ Magic Find§r§b)§r
     */
    private val blueVeryRareDropPattern by patternGroup.pattern(
        "blue-very-rare",
        "§9§lVERY RARE DROP! §r§7\\((?<itemName>.*?)\\) §r§b\\(\\+(?<magicFind>\\d+)% §r§b✯ Magic Find§r§b\\)"
    )

    /**
     * REGEX-TEST: §r§d§lCRAZY RARE DROP! §r§7(§r§f§r§6Judgement Core§r§7) §r§b(+171% §r§b✯ Magic Find§r§b)§r
     */
    private val crazyRareDropPattern by patternGroup.pattern(
        "crazy-rare",
        "§d§lCRAZY RARE DROP! §r§7\\((?<itemName>.*?)\\) §r§b\\(\\+(?<magicFind>\\d+)% §r§b✯ Magic Find§r§b\\)"
    )

    private val reg = "(\\d+)x (.+)".toRegex()

    private var dropsJson: SlayerDropsJson? = null

    @HandleEvent
    fun onRepoReload(e: RepositoryReloadEvent) {
        dropsJson = e.getConstant<SlayerDropsJson>("rng_meter/slayer/Voidgloom")
        println("DropsJson: $dropsJson")
    }

    @HandleEvent
    fun onChat(e: SkyHanniChatEvent) {
        loop@ for (pattern: Pattern in listOf(
            insaneDropPattern,
            purpleVeryRareDropPattern,
            rareDropPattern,
            blueVeryRareDropPattern,
            crazyRareDropPattern
        )) {
            pattern.matchMatcher(e.message) {
                if (dropsJson == null) return

                val rngStorage: ProfileSpecificStorage.SlayerRngMeterStorage? = storage?.entries?.elementAtOrNull(1)?.value

                var itemName: String = group("itemName")
                if (reg.find(itemName) != null) {
                    val (amm, cleanName) = reg.find(itemName)?.destructured ?: continue@loop
                    itemName = cleanName
                }
                itemName = itemName.trim()
                val magicFind: Int = group("magicFind").toInt()
                val unformattedItemName: String = itemName.removeColor()
                val internalName: NeuInternalName = NeuInternalName.fromItemNameOrNull(unformattedItemName) ?: itemName.toInternalName()

                val meterDrop = internalName == NeuInternalName.fromItemNameOrNull(rngStorage?.itemGoal ?: "")

                val dropChance = getDropChance(dropsJson!!, magicFind, internalName, meterDrop) ?: return
                val dropChanceAsFraction = "1/${(1 / dropChance).toInt()}"

                e.chatComponent = ChatComponentText(
                    e.message + " §8($dropChanceAsFraction)"
                )
            }
        }
    }

    private fun getDropChance(dropsJson: SlayerDropsJson, magicFind: Int, internalName: NeuInternalName, isMeterDrop: Boolean): Double? {
        if (isMeterDrop) {
            val totalWeight = getTotalWeight(dropsJson, magicFind, internalName, true)
            val dropWeight = getMagicFindModifiedWeight(getRngMeterModifier(dropsJson, internalName) ?: 0.0, magicFind)

            return dropWeight.toDouble() / totalWeight
        } else {
            val totalWeight = getTotalWeight(dropsJson, magicFind, internalName, false)
            val dropWeight = getMagicFindModifiedWeight(getDropWeight(dropsJson, internalName) ?: 0.0, magicFind)

            return dropWeight.toDouble() / totalWeight
        }
    }

    private fun getTotalWeight(dropsJson: SlayerDropsJson, magicFind: Int, internalName: NeuInternalName, isMeterDrop: Boolean): Double {
        var totalWeight = 10000.0

        val rngStorage: ProfileSpecificStorage.SlayerRngMeterStorage? = storage?.entries?.elementAtOrNull(1)?.value

        val selectedDrop = NeuInternalName.fromItemNameOrNull(rngStorage?.itemGoal ?: "")

        for (drop in dropsJson.table) {
            val dropDetails = drop.value

            var weight: Double = dropDetails.weight.toDouble()

            if (drop.key == selectedDrop) {
                weight = getRngMeterModifier(dropsJson, selectedDrop)?.toDouble() ?: 0.0
            }

            if (dropDetails.magicFind) {
                totalWeight += getMagicFindModifiedWeight(weight, magicFind)
            } else {
                totalWeight += weight
            }
        }

        if (selectedDrop == null) return totalWeight

        if (getTable(dropsJson, selectedDrop) == dropsJson.extraTable) {
            for (drop in dropsJson.table) {
                val dropDetails = drop.value

                var weight: Double = dropDetails.weight.toDouble()

                if (drop.key == selectedDrop) {
                    weight = getRngMeterModifier(dropsJson, selectedDrop) ?: 0.0
                }

                if (dropDetails.magicFind) {
                    totalWeight += getMagicFindModifiedWeight(weight, magicFind)
                } else {
                    totalWeight += weight
                }
            }
        }

        return totalWeight
    }

    private fun getTable(dropsJson: SlayerDropsJson, selectedDrop: NeuInternalName): Map<NeuInternalName, DropDetails>? {
        return (dropsJson.table + dropsJson.extraTable).entries
            .firstOrNull { it.key == selectedDrop }
            ?.value
            ?.let { mapOf(selectedDrop to it) }
    }

    private fun getMagicFindModifiedWeight(itemWeight: Double, magicFind: Int): Long {
        return (itemWeight * (1 + magicFind / 100)).toLong()
    }

    private fun getRngMeterModifier(dropsJson: SlayerDropsJson, drop: NeuInternalName): Double? {
        val currentXp = storage?.entries?.elementAtOrNull(1)?.value?.currentMeter ?: return null

        val dropDetails = getDropDetails(dropsJson, drop) ?: return null
        val xpNeeded = dropDetails.xpNeeded

        return dropDetails.weight * (1 + (2 * currentXp / xpNeeded).coerceAtMost(2)).toDouble()
    }

    private fun getDropWeight(dropsJson: SlayerDropsJson, internalName: NeuInternalName): Double? {
        for (table in (dropsJson.table + dropsJson.extraTable)) {
             if (table.key == internalName) {
                 return table.value.weight.toDouble()
             }
        }
        return null
    }

    private fun getDropDetails(dropsJson: SlayerDropsJson, internalDropName: NeuInternalName): DropDetails? {
        for (table in dropsJson.table) {
            if (table.key.contains(internalDropName.asString())) {
                return table.value
            }
        }
        return null
    }
}
