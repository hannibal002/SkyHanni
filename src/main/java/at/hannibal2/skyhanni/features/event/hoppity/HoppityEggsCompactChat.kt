package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.config.features.event.hoppity.HoppityChatConfig
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.HoppityStateDataSet
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType.Companion.resettingEntries
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEventSummary.getRabbitsFormat
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFTimeTowerManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import net.minecraft.init.Items
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

typealias RarityType = HoppityChatConfig.CompactRarityTypes

@SkyHanniModule
object HoppityEggsCompactChat {

    private var lockedHitmanClaimCount: Int? = null
    private val config get() = CFApi.config
    private val chatConfig get() = HoppityEggsManager.config.chat
    private val waypointsConfig get() = HoppityEggsManager.config.waypoints
    private val hitmanCompactDataSets: MutableList<HoppityStateDataSet> = mutableListOf()
    private var hoppityDataSet = HoppityStateDataSet()

    private fun reset() {
        lockedHitmanClaimCount = null
        hoppityDataSet.reset()
        hitmanCompactDataSets.clear()
    }

    fun compactChat(event: SkyHanniChatEvent?, dataSet: HoppityStateDataSet) {
        if (!chatConfig.compact) return
        hoppityDataSet = dataSet.copy()
        event?.let { it.blockedReason = "compact_hoppity" }
        if (hoppityDataSet.hoppityMessages.size == 3) sendCompact()
    }

    private fun sendCompact() {
        if (hoppityDataSet.lastMeal?.let { it == HoppityEggType.HITMAN } == true) compactMultipleFinds()
        else sendSingularFind()
    }

    private fun sendSingularFind() {
        if (HoppityEggType.resettingEntries.contains(hoppityDataSet.lastMeal) && waypointsConfig.shared) {
            DelayedRun.runDelayed(5.milliseconds) {
                createWaypointShareCompactMessage(HoppityEggsManager.getAndDisposeWaypointOnclick())
                reset()
            }
        } else {
            ChatUtils.hoverableChat(createCompactMessage(), hover = hoppityDataSet.hoppityMessages, prefix = false)
            reset()
        }
    }

    private fun getExpectedHitmanFinds(): Int {
        val lockedValue = lockedHitmanClaimCount
        val storageValue = ProfileStorageData.profileSpecific?.chocolateFactory?.hitmanStats?.availableHitmanEggs
        val inventoryValue = if (InventoryUtils.openInventoryName() == "Claim All") {
            InventoryUtils.getItemsInOpenChest().count { it.stack.item == Items.skull }
        } else null
        return lockedValue ?: storageValue ?: inventoryValue ?: 0
    }

    private fun compactMultipleFinds() {
        val eggsBeingClaimedCount = getExpectedHitmanFinds().takeIf {
            it > chatConfig.compactHitmanThreshold
        } ?: return sendSingularFind()
        lockedHitmanClaimCount = eggsBeingClaimedCount

        hitmanCompactDataSets.add(hoppityDataSet.copy().also { hoppityDataSet.reset() })

        val hitmanFindsNow = hitmanCompactDataSets.size
        if (hitmanFindsNow >= eggsBeingClaimedCount) sendHitmanSummary()
        else DelayedRun.runDelayed(2.seconds) {
            // Runaway check to make sure data doesn't sit still if expected finds don't calculate correctly
            if (hitmanCompactDataSets.size == hitmanFindsNow) sendHitmanSummary()
        }
    }

    private fun sendHitmanSummary() {
        if (hitmanCompactDataSets.isEmpty()) return
        val summaryMessage = buildString {
            appendLine("§c§lHitman Summary")
            appendLine()

            fun StringBuilder.formatRabbits(sets: List<HoppityStateDataSet>, text: String) {
                // Create a Map of LorenzRarity -> Int so we can use the existing EventSummary logic
                getRabbitsFormat(sets.getGroupedRarityMap(), text) { appendLine(it) }
            }

            hitmanCompactDataSets.filter { !it.duplicate }.takeIfNotEmpty()?.let { sets ->
                formatRabbits(hitmanCompactDataSets, "Total Hitman")
                appendLine()

                formatRabbits(sets, "New")
                appendLine()
            }

            hitmanCompactDataSets.filter { it.duplicate }.takeIfNotEmpty()?.let { sets ->
                formatRabbits(sets, "Duplicate")

                // Add the total amount of chocolate from duplicates
                val dupeChocolateAmount = sets.sumOf { it.lastDuplicateAmount ?: 0 }
                val timeFormat = dupeChocolateAmount.getChocExtraTimeString()
                appendLine(" §6+${dupeChocolateAmount.addSeparators()} §6Chocolate§7$timeFormat")
            }
        }
        ChatUtils.hoverableChat(
            summaryMessage,
            hover = hitmanCompactDataSets.sortedBy {
                if (it.duplicate) 1 else 0
            }.map { it.createCompactMessage(withMeal = false) },
            prefix = false,
        )
        reset()
    }

    private fun Collection<HoppityStateDataSet>.getGroupedRarityMap(): Map<LorenzRarity, Int> =
        this.mapNotNull { it.lastRarity }
            .groupingBy { it }
            .eachCount()

    private fun Long?.getChocExtraTimeString(): String {
        if (this == null) return "?"
        val extraTime = CFApi.timeUntilNeed(this)
        return if (config.showDuplicateTime) ", §a+§b${extraTime.format(maxUnits = 2)}§7" else ""
    }

    private fun HoppityStateDataSet.getNameFormat(): String =
        lastName.takeIf { it.isNotEmpty() } ?: "§C§L???"

    private fun HoppityStateDataSet.getRarityString(): String =
        lastRarity?.let { "${it.chatColorCode}§l${it.rawName}" } ?: "§C§L???"

    private fun HoppityStateDataSet.getRarityFormat(): String = when {
        hoppityDataSet.duplicate && chatConfig.rarityInCompact in listOf(RarityType.BOTH, RarityType.DUPE) -> "${getRarityString()} "
        !hoppityDataSet.duplicate && chatConfig.rarityInCompact in listOf(RarityType.BOTH, RarityType.NEW) -> "${getRarityString()} "
        else -> ""
    }

    private fun HoppityStateDataSet.createCompactMessage(withMeal: Boolean = true): String {
        val mealNameFormat = if (withMeal) when (lastMeal) {
            in resettingEntries -> "${lastMeal?.coloredName.orEmpty()} Egg! "
            else -> "${lastMeal?.coloredName.orEmpty()} Rabbit! "
        } else ""

        val nameFormat = getNameFormat()
        val rarityFormat = getRarityFormat()

        return if (duplicate) {
            val dupeChocAmount = lastDuplicateAmount?.shortFormat() ?: "?"
            val dupeNumberFormat = if (chatConfig.showDuplicateNumber) {
                (HoppityCollectionStats.getRabbitCount(lastName)).takeIf { it > 0 }?.let {
                    " §7(§b#$it§7)"
                }.orEmpty()
            } else ""

            val timeStr = lastDuplicateAmount.getChocExtraTimeString()
            val dupeChocColor = if (chatConfig.recolorTTChocolate && CFTimeTowerManager.timeTowerActive()) "§d" else "§6"

            val dupeChocFormat = " §7(§6+$dupeChocColor$dupeChocAmount §6Chocolate§7$timeStr)"

            "$mealNameFormat§7Duplicate $rarityFormat$nameFormat$dupeNumberFormat$dupeChocFormat"
        } else {
            "$mealNameFormat§d§lNEW $rarityFormat$nameFormat §7($lastProfit§7)"
        }
    }

    private fun createCompactMessage(withMeal: Boolean = true) = hoppityDataSet.createCompactMessage(withMeal)

    private fun createWaypointShareCompactMessage(onClick: () -> Unit) {
        val hover = hoppityDataSet.hoppityMessages.joinToString("\n") +
            " \n§eClick here to share the location of this chocolate egg with the server!"
        ChatUtils.clickableChat(
            createCompactMessage(),
            hover = hover,
            onClick = onClick,
            expireAt = 30.seconds.fromNow(),
            oneTimeClick = true,
            prefix = false,
        )
    }
}
