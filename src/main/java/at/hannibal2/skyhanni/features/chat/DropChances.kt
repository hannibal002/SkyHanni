package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.api.event.HandleEvent
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
import java.util.regex.Pattern

@SkyHanniModule
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

    private var drops: Map<String, Map<String, Map<String, DropDetails>>> = emptyMap()
    private var dropsJson: SlayerDropsJson? = null

    @HandleEvent
    fun onRepoReload(e: RepositoryReloadEvent) {
        drops = e.getConstant<SlayerDropsJson>("rng_meter/slayer/Voidgloom").drops
        dropsJson = e.getConstant("rng_meter/slayer/Voidgloom")
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
                val magicFind: Int = group("magicFind").toInt()
                val unformattedItemName: String = itemName.removeColor()
                val internalName: NeuInternalName = NeuInternalName.fromItemNameOrNull(unformattedItemName) ?: itemName.toInternalName()

                val dropChance: Double? = getDropChance(dropsJson!!, magicFind, internalName)
                val dropChanceAsFraction = "1/${(1 / dropChance!!).toInt()}"

                e.chatComponent = ChatComponentText(
                    e.message + " §8($dropChanceAsFraction)"
                )
            }
        }
    }

    private fun getDropChance(dropsJson: SlayerDropsJson, magicFind: Int, internalName: NeuInternalName): Double? {
        val totalWeight = getTotalWeight(dropsJson, magicFind) ?: return null
        ChatUtils.chat("Total weight: $totalWeight")

        if (storage == null) return null

        var selectedDropWeight = 0L
        @Suppress("MaxLineLength")
        selectedDropWeight = if (internalName == NeuInternalName.fromItemNameOrNull(storage!!.entries.elementAtOrNull(1)?.value?.itemGoal!!)) {
            getModifiedWeight(getRngMeterModifier(dropsJson.drops)!!.toInt(), magicFind)
        } else {
            @Suppress("MaxLineLength")
            getModifiedWeight(getDropDetails(dropsJson.drops, internalName)!!.weight, magicFind)
        }
        ChatUtils.chat("Selected drop weight: $selectedDropWeight")

        return selectedDropWeight.toDouble() / totalWeight.toDouble()
    }

    private fun getTotalWeight(dropsJson: SlayerDropsJson, magicFind: Int): Long? {
        var totalWeight = 10000L
        if (storage == null) ChatUtils.chat("Storage not found.")

        val selectedDrop = NeuInternalName.fromItemNameOrNull(storage!!.entries.elementAtOrNull(1)?.value?.itemGoal!!) ?: return null

        for (drop in dropsJson.drops["main_table"]?.values!!) {
            for ((dropKey, dropDetails) in drop.entries) {
                if (dropKey == selectedDrop.asString()) continue

                val itemWeight = dropDetails.weight

                if (dropDetails.magicFind) {
                    val modifiedWeight = getModifiedWeight(itemWeight, magicFind)
                    totalWeight += modifiedWeight.toInt()
                } else {
                    totalWeight += itemWeight
                }
            }
        }

        if (getSelectedTable(dropsJson, selectedDrop) == dropsJson.drops["extra_table"]) {
            for (drop in dropsJson.drops["extra_table"]?.values!!) {
                for ((dropKey, dropDetails) in drop.entries) {
                    if (dropKey == selectedDrop.asString()) continue

                    val itemWeight = dropDetails.weight

                    if (dropDetails.magicFind) {
                        val modifiedWeight = getModifiedWeight(itemWeight, magicFind)
                        totalWeight += modifiedWeight.toInt()
                    } else {
                        totalWeight += itemWeight
                    }
                }
            }
        }

        val selectedDropWeight = getModifiedWeight(getRngMeterModifier(dropsJson.drops)!!.toInt(), magicFind)
        ChatUtils.chat("Selected drop weight: $selectedDropWeight")
        totalWeight += selectedDropWeight
        ChatUtils.chat("Total weight: $totalWeight")

        return totalWeight
    }

    private fun getSelectedTable(dropsJson: SlayerDropsJson, selectedDrop: NeuInternalName): Map<String, Map<String, DropDetails>>? {
        for (table in dropsJson.drops.values) {
            for (drop in table.values) {
                if (drop.containsKey(selectedDrop.asString())) {
                    ChatUtils.chat("Selected table: $table")
                    return table
                }
            }
        }
        return null
    }

    private fun getModifiedWeight(itemWeight: Int, magicFind: Int): Long {
        return (itemWeight * (1 + magicFind / 100)).toLong()
    }

    private fun getRngMeterModifier(drops: Map<String, Map<String, Map<String, DropDetails>>>): Long? {
        val selectedDrop = NeuInternalName.fromItemNameOrNull(storage!!.entries.elementAtOrNull(1)?.value?.itemGoal!!) ?: return null
        ChatUtils.chat("Selected drop: $selectedDrop")
        val currentXp = storage!!.entries.elementAtOrNull(1)?.value?.currentMeter!!
        ChatUtils.chat("Current xp: $currentXp")

        val dropDetails = getDropDetails(drops, selectedDrop) ?: return null
        ChatUtils.chat("Drop details found.")
        val xpNeeded = dropDetails.xpNeeded
        ChatUtils.chat("Xp needed: $xpNeeded")

        return dropDetails.weight * (1 + (2 * currentXp / xpNeeded).coerceAtMost(2))
    }

    private fun getDropDetails(drops: Map<String, Map<String, Map<String, DropDetails>>>, internalDropName: NeuInternalName): DropDetails? {
        for (table in drops.values) {
            for (drop in table.values) {
                if (drop.containsKey(internalDropName.asString())) {
                    ChatUtils.chat(
                        "Found drop details for $internalDropName: \n" +
                            "xpNeeded: ${drop[internalDropName.asString()]!!.xpNeeded}\n" +
                            "weight: ${drop[internalDropName.asString()]!!.weight}\n" +
                            "magicFind: ${drop[internalDropName.asString()]!!.magicFind}"
                    )
                    return drop[internalDropName.asString()]
                }
            }
        }
        return null
    }
}
