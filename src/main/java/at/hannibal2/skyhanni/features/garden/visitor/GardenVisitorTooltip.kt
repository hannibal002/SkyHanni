package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi.blockReason
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Handles tooltip parsing and modification for visitor trade offers.
 * Extracts shopping lists, calculates prices/times, and enriches tooltips.
 */
@SkyHanniModule
object GardenVisitorTooltip {

    private val config get() = VisitorApi.config

    /**
     * Tracks the last calculated total price.
     * Used by GardenVisitorStatus to update statistics.
     */
    var lastFullPrice = 0.0
        private set

    private val patternGroup = RepoPattern.group("garden.visitor.tooltip")

    /**
     * WRAPPED-REGEX-TEST: " §8+§c20 Copper"
     * WRAPPED-REGEX-TEST: " §8+§c150 Copper §d❤"
     */
    private val copperPattern by patternGroup.pattern(
        "copper",
        " §8\\+§c(?<amount>.*) Copper(?: .*)?",
    )

    /**
     * WRAPPED-REGEX-TEST: " §8+§215 §7Garden Experience"
     */
    private val gardenExperiencePattern by patternGroup.pattern(
        "gardenexperience",
        " §8\\+§2(?<amount>.*) §7Garden Experience",
    )

    @HandleEvent(priority = HandleEvent.HIGHEST)
    private fun onVisitorOpen(event: VisitorOpenEvent) {
        val visitor = event.visitor
        val offerItem = visitor.offer?.offerItem ?: return
        val lore = offerItem.getLore()

        readShoppingList(visitor, lore)

        readToolTip(visitor, offerItem, lore.toMutableList())

        visitor.lastLore = emptyList()
        visitor.blockedLore = emptyList()

        val alreadyReady = lore.any { it == "§eClick to give!" }
        if (alreadyReady) {
            VisitorApi.changeStatus(visitor, VisitorApi.VisitorStatus.READY, "tooltipClickToGive")
        } else {
            VisitorApi.changeStatus(visitor, VisitorApi.VisitorStatus.WAITING, "tooltipMissingItems")
        }

        GardenVisitorStatus.update()
    }

    /**
     * Reads the "Items Required" section from tooltip.
     * Populates visitor.shoppingList.
     */
    private fun readShoppingList(visitor: VisitorApi.Visitor, lore: List<String>) {
        for (line in lore) {
            if (line == "§7Items Required:") continue
            if (line.isEmpty()) break

            val (itemName, amount) = ItemUtils.readItemAmount(line) ?: run {
                ErrorManager.logErrorStateWithData(
                    "Could not read Shopping List in Visitor Inventory",
                    "ItemUtils.readItemAmount returns null",
                    "line" to line,
                    "lore" to lore,
                    "visitor" to visitor,
                )
                continue
            }
            val internalName = NeuInternalName.fromItemName(itemName)
            visitor.shoppingList[internalName] = amount
        }
    }

    /**
     * Called by VisitorListener when tooltip is rendered.
     * Modifies the tooltip to show calculated prices and times.
     */
    fun onTooltip(visitor: VisitorApi.Visitor, itemStack: SafeItemStack, toolTip: MutableList<String>) {
        if (itemStack.cleanName != "Accept Offer") return

        if (visitor.lastLore.isEmpty()) {
            readToolTip(visitor, itemStack, toolTip)
        }
        toolTip.clear()
        toolTip.addAll(visitor.lastLore)
    }

    private fun readItemLine(formattedLine: String, readingShoppingList: Boolean): Pair<NeuInternalName, Int>? {
        fun String.removeCharmedSuffix() = removeSuffix(" §d❤")

        val itemLine = if (readingShoppingList) formattedLine else formattedLine.removeCharmedSuffix()
        val (itemName, amount) = ItemUtils.readItemAmount(itemLine) ?: return null
        val internalName = NeuInternalName.fromItemNameOrNull(itemName.removeColor())
            ?.replace("◆_", "") ?: return null

        // Ignoring custom NEU items like copper
        if (internalName.startsWith("SKYBLOCK_")) return null

        return internalName to amount
    }

    /**
     * The heavy lifting. Parses the entire tooltip, calculates economics,
     * and generates enriched tooltip lines.
     */
    // TODO throw an axe on this function to split it up
    @Suppress("LongMethod", "CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
    private fun readToolTip(visitor: VisitorApi.Visitor, itemStack: SafeItemStack?, toolTip: MutableList<String>) {
        val stack = itemStack ?: error("Accept offer item not found for visitor ${visitor.visitorName}")

        var totalPrice = 0.0
        var farmingTimeRequired = 0.seconds
        var readingShoppingList = true
        lastFullPrice = 0.0
        val foundRewards = mutableListOf<NeuInternalName>()

        // The rendered tooltip can lose its color codes, which breaks amount parsing in the second pass.
        val requiredAmounts = mutableMapOf<NeuInternalName, Int>()
        val rewardAmounts = mutableMapOf<NeuInternalName, Int>()

        // First pass: Calculate totals
        for (formattedLine in stack.getLore()) {
            if (formattedLine.contains("Rewards")) {
                readingShoppingList = false
            }

            val (internalName, amount) = readItemLine(formattedLine, readingShoppingList) ?: continue

            val price = VisitorPriceCalculator.calculateItemPrice(internalName, amount)

            if (readingShoppingList) {
                requiredAmounts[internalName] = amount
                totalPrice += price
                lastFullPrice += price
            } else {
                rewardAmounts[internalName] = amount
                foundRewards.add(internalName)
                totalPrice -= price
            }
        }

        if (totalPrice < 0) {
            totalPrice = 0.0
        }

        notifyFoundRewards(visitor, foundRewards)

        // Second pass: Build enriched tooltip
        readingShoppingList = true
        val finalList = toolTip.map { it.removePrefix("§5§o") }.toMutableList()
        var offset = 0

        for ((i, formattedLine) in finalList.toMutableList().withIndex()) {
            val index = i + offset

            if (config.inventory.experiencePrice) {
                gardenExperiencePattern.matchMatcher(formattedLine) {
                    val gardenExp = group("amount").formatInt()
                    val pricePerExp = (totalPrice / gardenExp).toInt().shortFormat()
                    finalList[index] = "$formattedLine §7(paying §6$pricePerExp §7per)"
                }
            }

            copperPattern.matchMatcher(formattedLine) {
                val copper = group("amount").formatInt()
                finalList[index] = updateCopperLine(visitor, formattedLine, copper, totalPrice, farmingTimeRequired)
            }

            if (formattedLine.contains("Rewards")) {
                readingShoppingList = false
            }

            val (internalName, parsedAmount) = readItemLine(formattedLine, readingShoppingList) ?: continue

            val knownAmounts = if (readingShoppingList) requiredAmounts else rewardAmounts
            val amount = knownAmounts[internalName] ?: parsedAmount
            val price = VisitorPriceCalculator.calculateItemPrice(internalName, amount)

            if (config.inventory.showPrice) {
                val format = price.shortFormat()
                finalList[index] = "$formattedLine §7(§6$format§7)"
            }

            if (!readingShoppingList) continue

            if (config.inventory.exactAmountAndTime) {
                val farmingTime = VisitorPriceCalculator.calculateFarmingTime(internalName, amount)
                if (farmingTime != null) {
                    farmingTimeRequired += farmingTime

                    val cropType = getCropType(internalName)
                    val cropAmount = getCropAmount(internalName, amount)

                    if (cropType != null && cropAmount != null) {
                        val formattedName = "§e${cropAmount.addSeparators()}§7x ${cropType.cropName} "
                        val formattedTime = "in §b${farmingTime.format()}"

                        finalList.add(index + 1, "§7- $formattedName($formattedTime§7)")
                        offset++
                    }
                }
            }
        }

        visitor.lastLore = finalList
        visitor.blockReason = visitor.blockReason()
    }

    private fun notifyFoundRewards(visitor: VisitorApi.Visitor, foundRewards: List<NeuInternalName>) {
        if (foundRewards.isEmpty()) return
        val wasEmpty = visitor.allRewards.isEmpty()
        visitor.allRewards = foundRewards
        if (!wasEmpty || !config.rewardWarning.notifyInChat) return
        visitor.getRewardWarningAwards().forEach { reward ->
            val message = componentBuilder {
                append("Found Visitor Reward ")
                append(reward.displayName)
                append("!")
            }
            ChatUtils.chat(message)
        }
    }

    private fun updateCopperLine(
        visitor: VisitorApi.Visitor,
        formattedLine: String,
        copper: Int,
        totalPrice: Double,
        farmingTimeRequired: Duration,
    ): String {
        val pricePerCopper = VisitorPriceCalculator.calculatePricePerCopper(totalPrice, copper)
        visitor.pricePerCopper = pricePerCopper
        visitor.totalPrice = totalPrice
        visitor.totalReward = VisitorPriceCalculator.calculateTotalReward(copper)

        var copperLine = formattedLine
        if (config.inventory.copperPrice) {
            copperLine += " §7(paying §6${pricePerCopper.shortFormat()} §7per)"
        }
        if (config.inventory.copperTime) {
            copperLine += if (farmingTimeRequired != 0.seconds) {
                " §7(paying §b${(farmingTimeRequired / copper).format()} §7per)"
            } else {
                " §7(§cno speed data!§7)"
            }
        }
        return copperLine
    }

    private fun getCropType(internalName: NeuInternalName): CropType? {
        val itemName = NeuItems.getPrimitiveMultiplier(internalName).internalName.itemNameWithoutColor
        return CropType.getByNameOrNull(itemName)
    }

    private fun getCropAmount(internalName: NeuInternalName, amount: Int): Long? {
        getCropType(internalName) ?: return null
        return NeuItems.getPrimitiveMultiplier(internalName).amount.toLong() * amount
    }
}
