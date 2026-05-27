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
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.item.ItemStack
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
     * REGEX-TEST:  §8+§c20 Copper
     */
    private val copperPattern by patternGroup.pattern(
        "copper",
        " §8\\+§c(?<amount>.*) Copper",
    )

    /**
     * REGEX-TEST:  §8+§215 §7Garden Experience
     */
    private val gardenExperiencePattern by patternGroup.pattern(
        "gardenexperience",
        " §8\\+§2(?<amount>.*) §7Garden Experience",
    )

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onVisitorOpen(event: VisitorOpenEvent) {
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
    fun onTooltip(visitor: VisitorApi.Visitor, itemStack: ItemStack, toolTip: MutableList<String>) {
        if (itemStack.cleanName() != "Accept Offer") return

        if (visitor.lastLore.isEmpty()) {
            readToolTip(visitor, itemStack, toolTip)
        }
        toolTip.clear()
        toolTip.addAll(visitor.lastLore)
    }

    /**
     * The heavy lifting. Parses the entire tooltip, calculates economics,
     * and generates enriched tooltip lines.
     */
    // TODO throw an axe on this function to split it up
    @Suppress("LongMethod", "CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
    private fun readToolTip(visitor: VisitorApi.Visitor, itemStack: ItemStack?, toolTip: MutableList<String>) {
        val stack = itemStack ?: error("Accept offer item not found for visitor ${visitor.visitorName}")

        var totalPrice = 0.0
        var farmingTimeRequired = 0.seconds
        var readingShoppingList = true
        lastFullPrice = 0.0
        val foundRewards = mutableListOf<NeuInternalName>()

        // First pass: Calculate totals
        for (formattedLine in stack.getLore()) {
            if (formattedLine.contains("Rewards")) {
                readingShoppingList = false
            }

            val (itemName, amount) = ItemUtils.readItemAmount(formattedLine) ?: continue
            val internalName = NeuInternalName.fromItemNameOrNull(itemName.removeColor())
                ?.replace("◆_", "") ?: continue

            // Ignoring custom NEU items like copper
            if (internalName.startsWith("SKYBLOCK_")) continue

            val price = VisitorPriceCalculator.calculateItemPrice(internalName, amount)

            if (readingShoppingList) {
                totalPrice += price
                lastFullPrice += price
            } else {
                foundRewards.add(internalName)
                totalPrice -= price
            }
        }

        if (totalPrice < 0) {
            totalPrice = 0.0
        }

        if (foundRewards.isNotEmpty()) {
            val wasEmpty = visitor.allRewards.isEmpty()
            visitor.allRewards = foundRewards
            if (wasEmpty && config.rewardWarning.notifyInChat) {
                visitor.getRewardWarningAwards().forEach { reward ->
                    ChatUtils.chat(
                        componentBuilder {
                            append("Found Visitor Reward ")
                            append(reward.displayName)
                            append("!")
                        }
                    )
                }
            }
        }

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
                val pricePerCopper = VisitorPriceCalculator.calculatePricePerCopper(totalPrice, copper)
                visitor.pricePerCopper = pricePerCopper
                visitor.totalPrice = totalPrice

                val totalReward = VisitorPriceCalculator.calculateTotalReward(copper)
                visitor.totalReward = totalReward

                val timePerCopper = (farmingTimeRequired / copper).format()
                var copperLine = formattedLine

                if (config.inventory.copperPrice) {
                    copperLine += " §7(paying §6${pricePerCopper.shortFormat()} §7per)"
                }
                if (config.inventory.copperTime) {
                    copperLine += if (farmingTimeRequired != 0.seconds) {
                        " §7(paying §b$timePerCopper §7per)"
                    } else {
                        " §7(§cno speed data!§7)"
                    }
                }
                finalList[index] = copperLine
            }

            if (formattedLine.contains("Rewards")) {
                readingShoppingList = false
            }

            val (itemName, amount) = ItemUtils.readItemAmount(formattedLine) ?: continue
            val internalName = NeuInternalName.fromItemNameOrNull(itemName.removeColor())
                ?.replace("◆_", "") ?: continue

            // Ignoring custom NEU items
            if (internalName.startsWith("SKYBLOCK_")) continue

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

    private fun getCropType(internalName: NeuInternalName) =
        CropType.getByNameOrNull(
            NeuItems.getPrimitiveMultiplier(internalName)
                .internalName.itemNameWithoutColor,
        )

    private fun getCropAmount(internalName: NeuInternalName, amount: Int): Long? {
        getCropType(internalName) ?: return null
        return NeuItems.getPrimitiveMultiplier(internalName).amount.toLong() * amount
    }
}
