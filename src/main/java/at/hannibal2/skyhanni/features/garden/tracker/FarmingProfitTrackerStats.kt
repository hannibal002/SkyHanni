package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig.DisplayStat
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig.TrackedSource
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.features.garden.tracker.FarmingProfitTracker.Data
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import kotlin.math.absoluteValue

object FarmingProfitTrackerStats {

    private val config get() = SkyHanniMod.feature.garden.farmingProfitTracker

    fun addStats(list: MutableList<Searchable>, data: Data) {
        list.addConfiguredStats(data)
    }

    private fun MutableList<Searchable>.addConfiguredStats(data: Data) {
        config.displayedStats.forEach { stat ->
            when (stat) {
                DisplayStat.CROPS_TRACKED -> addCropAmountLine(data)
                DisplayStat.BLOCKS_BROKEN -> addBlocksBrokenLine(data)
                DisplayStat.RARE_CROP_DROPS -> addRareCropLine(data)
                DisplayStat.BLESSED_DROPS -> addBlessedLine(data)
                DisplayStat.CROP_FEVERS -> addCropFeversLine(data)
                DisplayStat.CROP_FEVER_DROPS -> addCropFeverLine(data)
                DisplayStat.PESTS_KILLED -> addPestLine(data)
                DisplayStat.VISITORS_SERVED -> addVisitorsLine(data)
                DisplayStat.BOUNTIFUL_COINS -> addBountifulLine(data)
            }
        }
    }

    private fun MutableList<Searchable>.addCropAmountLine(data: Data) {
        val total = data.getTotalCropAmount()
        if (total == 0L) return
        add(
            Renderable.hoverTips(
                "§7Crops tracked: §e${total.addSeparators()}",
                buildList {
                    add("§7By crop:")
                    data.getCropAmountsByCrop().entries.sortedBy { it.key.cropName }.forEach { (crop, amount) ->
                        add(" §7- §e${amount.addSeparators()}x §7${crop.cropName}")
                    }
                    add("")
                    add("§7By source:")
                    data.getCropAmountsBySource().entries.sortedBy { it.key.ordinal }.forEach { (source, amount) ->
                        add(" §7- §e${amount.addSeparators()}x §7${source.displayName}")
                    }
                },
            ).toSearchable("Crops tracked"),
        )
    }

    private fun MutableList<Searchable>.addBlocksBrokenLine(data: Data) {
        val total = data.getTotalBlocksBroken()
        if (total == 0L) return
        add(
            Renderable.hoverTips(
                "§7Blocks broken: §e${total.addSeparators()}",
                data.blocksBroken.entries.sortedBy { it.key.cropName }.map { (crop, amount) ->
                    "§7${crop.cropName}: §e${amount.addSeparators()}"
                },
            ).toSearchable("Blocks broken"),
        )
    }

    private fun MutableList<Searchable>.addRareCropLine(data: Data) {
        val rareDrops = data.getRareCropDropsByType()
        if (rareDrops.isNotEmpty()) {
            val total = rareDrops.values.sum()
            add(
                Renderable.hoverTips(
                    "§6Rare crop drops: §6${total.addSeparators()}",
                    rareDrops.entries.sortedBy { it.key.dropName.removeColor() }.map { (drop, amount) ->
                        "§7${drop.dropName}: §e${amount.addSeparators()}"
                    },
                ).toSearchable("Rare crop drops"),
            )
        }
        addSeasoningLine(data)
    }

    private fun MutableList<Searchable>.addSeasoningLine(data: Data) {
        val total = data.getTotalSeasoningDrops()
        if (total == 0L) return
        add(
            Renderable.hoverTips(
                "§2Seasoning: §2${total.addSeparators()}",
                listOf("§7Automatically donated to the Harvest Feast."),
            ).toSearchable("Seasoning"),
        )
    }

    private fun MutableList<Searchable>.addBlessedLine(data: Data) {
        val total = data.getTotalBlessedDrops()
        if (total == 0L) return
        add(
            Renderable.hoverTips(
                "§9Blessed drops: §9${total.addSeparators()}",
                data.blessedDrops.entries.sortedBy { it.key.itemNameWithoutColor }.map { (drop, amount) ->
                    "§7${drop.itemNameWithoutColor}: §e${amount.addSeparators()}"
                },
            ).toSearchable("Blessed drops"),
        )
    }

    private fun MutableList<Searchable>.addCropFeversLine(data: Data) {
        val total = data.getTotalCropFevers()
        if (total == 0L) return
        add(
            Renderable.hoverTips(
                "§7Crop Fevers: §e${total.addSeparators()}",
                data.cropFevers.entries.sortedBy { it.key.cropName }.map { (crop, amount) ->
                    "§7${crop.cropName}: §e${amount.addSeparators()}"
                },
            ).toSearchable("Crop Fevers"),
        )
    }

    private fun MutableList<Searchable>.addCropFeverLine(data: Data) {
        val total = data.getTotalCropFeverDrops()
        if (total == 0L) return
        add(
            Renderable.hoverTips(
                "§7Crop Fever drops: §e${total.addSeparators()}",
                data.cropFeverDrops.entries.sortedBy { it.key.ordinal }.map { (drop, amount) ->
                    "§7$drop: §e${amount.addSeparators()}"
                },
            ).toSearchable("Crop Fever drops"),
        )
    }

    private fun MutableList<Searchable>.addPestLine(data: Data) {
        val total = data.getTotalPestKills()
        if (total == 0L) return
        add(
            Renderable.hoverTips(
                "§2Pests killed: §2${total.addSeparators()}",
                data.pestKills.entries
                    .filter { it.key != PestType.UNKNOWN && it.value > 0 }
                    .sortedBy { it.key.displayName }
                    .map { (pest, amount) -> "§7${pest.pluralName}: §e${amount.addSeparators()}" },
            ).toSearchable("Pests killed"),
        )
    }

    private fun MutableList<Searchable>.addVisitorsLine(data: Data) {
        if (!data.isShowing(TrackedSource.VISITORS)) return
        val visitorsServed = data.visitorsServed
        val vinylSetsGiven = data.visitorVinylSetsGiven
        if (visitorsServed == 0L && vinylSetsGiven == 0L) return

        val visitorItems = data.bucketedItems[TrackedSource.VISITORS].orEmpty()
        var netValue = 0.0
        val hoverTips = buildList {
            val costLines = mutableListOf<String>()
            val rewardLines = mutableListOf<String>()

            if (vinylSetsGiven > 0L) {
                add("§7Vinyl sets gifted: §e${vinylSetsGiven.addSeparators()}")
                add("")
            }

            visitorItems.entries.sortedBy { it.key.itemNameWithoutColor }.forEach { (internalName, item) ->
                val amount = item.totalAmount
                if (amount == 0L) return@forEach
                val countedInProfit =
                    !item.hidden || !FarmingProfitTracker.trackerDisplayConfig.itemTracker.excludeHiddenItemsInPrice
                val displayAmount = if (internalName == SKYBLOCK_COIN && data.visitorCopper > 0) {
                    data.visitorCopper
                } else {
                    amount.absoluteValue
                }
                val itemName = if (internalName == SKYBLOCK_COIN) "Copper" else internalName.itemNameWithoutColor
                val price = if (internalName == SKYBLOCK_COIN) 1.0 else data.getCustomPricePer(internalName, FarmingProfitTracker)
                val total = price * amount
                val profitText = if (countedInProfit) {
                    netValue += total
                    signedCoinFormat(total)
                } else {
                    "§8hidden"
                }
                val line = "§7$itemName: §e${displayAmount.addSeparators()} §7($profitText§7)"
                if (amount > 0) rewardLines.add(line) else costLines.add(line)
            }

            if (costLines.isNotEmpty()) {
                add("§7Items given:")
                addAll(costLines)
            }
            if (rewardLines.isNotEmpty()) {
                if (isNotEmpty()) add("")
                add("§7Rewards:")
                addAll(rewardLines)
            }
            if (isNotEmpty()) add("")
            add("§7Net visitor value: ${signedCoinFormat(netValue)}")
        }

        add(
            Renderable.hoverTips(
                data.visitorLineText(visitorsServed, vinylSetsGiven, netValue),
                hoverTips,
            ).toSearchable("Visitor profit"),
        )
    }

    private fun Data.visitorLineText(visitorsServed: Long, vinylSetsGiven: Long, netValue: Double): String =
        if (visitorsServed > 0L) {
            "§7Visitors served: §a${visitorsServed.addSeparators()} §7(${signedCoinFormat(netValue)}§7)"
        } else {
            "§7Visitor vinyl gifts: §a${vinylSetsGiven.addSeparators()} §7(${signedCoinFormat(netValue)}§7)"
        }

    private fun signedCoinFormat(value: Double): String = when {
        value > 0.0 -> "§a+${value.shortFormat()}"
        value < 0.0 -> "§c-${value.absoluteValue.shortFormat()}"
        else -> "§60"
    }

    private fun MutableList<Searchable>.addBountifulLine(data: Data) {
        if (!data.isShowing(TrackedSource.BOUNTIFUL)) return
        val coins = data.bountifulCoins
        if (coins == 0L) return
        add(
            Renderable.hoverTips(
                "§6Bountiful coins: §6${coins.addSeparators()}",
                listOf("§7Coins gained directly from the Bountiful reforge."),
            ).toSearchable("Bountiful coins"),
        )
    }

}
