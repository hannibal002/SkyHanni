package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI.addsToMilestone
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.garden.farming.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropMilestoneDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.chat
import at.hannibal2.skyhanni.utils.ChatUtils.clickableChat
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@SkyHanniModule
object GardenCropMilestones {

    private val patternGroup = RepoPattern.group("data.garden.milestone")

    /**
     * REGEX-TEST: §7Harvest §fWheat §7on your Garden to
     * REGEX-TEST: §7Harvest §fCocoa Beans §7on your
     */
    private val cropPattern by patternGroup.pattern(
        "crop",
        "§7Harvest §f(?<name>.*) §7on .*",
    )

    /**
     * REGEX-TEST: §7Total: §a36,967,397
     */
    val totalPattern by patternGroup.pattern(
        "total",
        "§7Total: §a(?<name>.*)",
    )

    /**
     * REGEX-TEST:  Cocoa Beans 31: §r§a68%
     * REGEX-TEST:  Potato 32: §r§a97.7%
     */
    @Suppress("MaxLineLength")
    private val tabListPattern by patternGroup.pattern(
        "tablist",
        " (?<crop>Wheat|Carrot|Potato|Pumpkin|Sugar Cane|Melon|Cactus|Cocoa Beans|Mushroom|Nether Wart) (?<tier>\\d+): §r§a(?<percentage>.*)%",
    )


    /**
     * REGEX-TEST:   §r§b§lGARDEN MILESTONE §3Melon §845➜§346
     */
    private val levelUpPattern by patternGroup.pattern(
        "levelup",
        " {2}§r§b§lGARDEN MILESTONE §3(?<crop>.*) §8.*➜§3(?<tier>.*)",
    )

    private val storage get() = GardenApi.storage
    private val maxMilestoneValue: MutableMap<CropType, Int> = mutableMapOf()

    fun getCropTypeByLore(itemStack: ItemStack): CropType? {
        cropPattern.firstMatcher(itemStack.getLore()) {
            val name = group("name")
            return CropType.getByNameOrNull(name)
        }
        return null
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Crop Milestones") return

        for ((_, stack) in event.inventoryItems) {
            val crop = getCropTypeByLore(stack) ?: continue
            totalPattern.firstMatcher(stack.getLore()) {
                val oldAmount = crop.getMilestoneCounter()
                val amount = group("name").formatLong()
                val change = amount - oldAmount
                forceUpdateMilestone(crop, change)
            }
        }
        storage?.lastMilestoneFix = SimpleTimeMark.now()
        GardenCropMilestonesCommunityFix.openInventory(event.inventoryItems)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        levelUpPattern.matchMatcher(event.message) {
            val cropName = group("crop")
            val crop = CropType.getByNameOrNull(cropName) ?: return

            val tier = group("tier").romanToDecimalIfNecessary()

            val crops = getCropsForTier(tier, crop) ?: return
            changedValue(crop, crops, "level up chat message", 0)
        }
    }

    @HandleEvent
    fun onTabListUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.CROP_MILESTONE)) return
        tabListPattern.firstMatcher(event.lines) {
            val tier = group("tier").toInt()
            val percentage = group("percentage").toDouble()
            val cropName = group("crop")

            checkTabDifference(cropName, tier, percentage)
        }
    }

    @HandleEvent
    fun onCollectionAdd(event: CropCollectionAddEvent) {
        val cropType = event.crop
        val collectionType = event.cropCollectionType
        val amount = event.amount

        if (!collectionType.addsToMilestone()) return

        cropType.addMilestoneCounter(amount)
    }

    val cropMilestoneCounter: MutableMap<CropType, Long>? get() = storage?.cropMilestoneCounter
    var cropMilestoneData: Map<CropType, List<Int>> = emptyMap()
    private var cropMilestoneTierCache: MutableMap<CropType, Int?> = mutableMapOf()
    private var amountToNextMilestoneTierCache: MutableMap<CropType, Long?> = mutableMapOf()

    fun CropType.getMilestoneCounter() = cropMilestoneCounter?.get(this) ?: 0

    private fun CropType.setMilestoneCounter(counter: Long) {
        cropMilestoneCounter?.set(this, counter)
        CropMilestoneUpdateEvent.post()
    }

    private fun CropType.addMilestoneCounter(counter: Long) {
        if (counter == 0L) return
        this.setMilestoneCounter(this.getMilestoneCounter() + counter)
    }

    fun CropType.isMaxMilestone(useOverflow: Boolean): Boolean? {
        if (useOverflow) return false

        val maxValue = this.getMaxedMilestoneAmount() ?: return null
        return getMilestoneCounter() >= maxValue
    }

    fun CropType.getTier(allowOverflow: Boolean = false): Int? {
        val tier = cropMilestoneTierCache.getOrPut(this) {
            calculateTierForCropCount(this.getMilestoneCounter(), this, allowOverflow)
        }
        // TODO void cache on config change event instead
        if (!allowOverflow && (tier ?: return null) > (cropMilestoneData[this]?.size ?: return null)) {
            cropMilestoneTierCache[this] = calculateTierForCropCount(this.getMilestoneCounter(), this, allowOverflow)
        }
        return tier
    }

    fun getMaxTier() = cropMilestoneData.values.firstOrNull()?.size

    private fun CropType.getMaxedMilestoneAmount(): Int? {
        return maxMilestoneValue.getOrPut(this) {
            cropMilestoneData[this]?.sum() ?: return null
        }
    }

    private fun calculateTierForCropCount(count: Long?, crop: CropType, allowOverflow: Boolean = false): Int? {
        var tier = 0
        var totalCrops = 0L
        val cropMilestone = cropMilestoneData[crop]
        val maxMilestoneAmount = crop.getMaxedMilestoneAmount()

        if (cropMilestone.isNullOrEmpty() || maxMilestoneAmount == null || count == null) {
            return null
        }
        val last = cropMilestone.last()

        if (count < (maxMilestoneAmount)) {
            for (tierCrops in cropMilestone) {
                totalCrops += tierCrops
                if (totalCrops >= count) {
                    return tier
                }
                tier++
            }
            return tier
        }

        tier = getMaxTier() ?: return null

        if (allowOverflow) {
            totalCrops -= maxMilestoneAmount
            tier += totalCrops.floorDiv(last).toInt()
        }

        return tier
    }

    fun getCropsForTier(requestedTier: Int?, crop: CropType, allowOverflow: Boolean = false): Long? {
        if (requestedTier == null) return null

        var totalCrops = 0L
        var tier = 0
        val cropMilestone = cropMilestoneData[crop] ?: return 0
        val definedTiers = cropMilestone.size

        if (requestedTier <= definedTiers || !allowOverflow) {
            for (tierCrops in cropMilestone) {
                totalCrops += tierCrops
                tier++
                if (tier == requestedTier) {
                    return totalCrops
                }
            }

            return if (!allowOverflow) 0 else totalCrops
        }


        for (tierCrops in cropMilestone) {
            totalCrops += tierCrops
            tier++
        }

        val additionalTiers = requestedTier - definedTiers

        val lastIncrement = cropMilestone.last().toLong()

        totalCrops += lastIncrement * additionalTiers

        return totalCrops
    }

    fun CropType.getProgressToNextTier(allowOverflow: Boolean = false): Long? {
        return amountToNextMilestoneTierCache.getOrPut(this) {
            this.calculateProgressToNextTier(allowOverflow)
        }
    }

    fun CropType.percentToNextTier(allowOverflow: Boolean = false): Double? {
        val progressAmount = getProgressToNextTier(allowOverflow) ?: return null
        val percent =
            progressAmount.toDouble() / (this.getTierAmount(this.getTier(allowOverflow), allowOverflow)?.toDouble() ?: return null)
        return percent
    }

    private fun CropType.calculateProgressToNextTier(allowOverflow: Boolean = false): Long? {
        val progress = getMilestoneCounter()
        val startTier = this.getTier() ?: return null
        val startCrops = getCropsForTier(startTier, this, allowOverflow)  ?: return null
        val end = this.getTierAmount(startTier + 1, allowOverflow) ?: return null
        val amount = end - (progress - startCrops)

        return amount
    }

    fun CropType.getTierAmount(tier: Int?, allowOverflow: Boolean = false): Long? {
        if (tier == null) return null

        val data = cropMilestoneData[this] ?: return null
        return data.getOrNull(tier)?.toLong() ?: if (allowOverflow) data.last().toLong() else -1L
    }

    fun onOverflowLevelUp(crop: CropType, oldLevel: Int, newLevel: Int) {
        val customGoalLevel = ProfileStorageData.profileSpecific?.garden?.customGoalMilestone?.get(crop) ?: 0
        val goalReached = newLevel == customGoalLevel

        // TODO utils function that is shared with Garden Level Display
        val rewards = buildList {
            add("    §r§8+§aRespect from Elite Farmers and SkyHanni members :)")
            add("    §r§8+§b1 Flexing Point")
            if (newLevel % 5 == 0)
                add("    §r§7§8+§d2 SkyHanni User Luck")
        }

        val cropName = crop.cropName
        val levelUpLine = "§r§b§lGARDEN MILESTONE §3$cropName §8$oldLevel➜§3$newLevel§r"
        val messages = listOf(
            "§r§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r",
            "  $levelUpLine",
            if (goalReached)
                listOf(
                    "",
                    "  §r§d§lGOAL REACHED!",
                    "",
                ).joinToString("\n")
            else
                "",
            "  §r§a§lREWARDS§r",
            rewards.joinToString("\n"),
            "§r§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r",
        )

        clickableChat(
            messages.joinToString("\n"),
            { ClipboardUtils.copyToClipboard(levelUpLine.removeColor()) },
            "Click to copy!",
            prefix = false
        )

        val message = "§e§lYou have reached your milestone goal of §b§l$customGoalLevel " +
            "§e§lin the §b§l$cropName §e§lcrop!"
        if (goalReached) {
            chat(message, false)
        }

        SoundUtils.createSound("random.levelup", 1f, 1f).playSound()
    }

    private fun changedValue(crop: CropType, tabListValue: Long, source: String, minDiff: Int) {
        val calculated = crop.getMilestoneCounter()
        val diff = tabListValue - calculated

        if (diff >= minDiff) {
            forceUpdateMilestone(crop, diff)
            storage?.lastMilestoneFix = SimpleTimeMark.now()
            GardenCropMilestoneDisplay.update()
            if (!loadedCrops.contains(crop)) {
                chat("Loaded ${crop.cropName} milestone data from $source!")
                loadedCrops.add(crop)
            }
        } else if (diff <= minDiff) {
            ChatUtils.debug("Fixed wrong ${crop.cropName} milestone data from $source: ${diff.addSeparators()}")
        }
    }

    private fun checkTabDifference(cropName: String, tier: Int, percentage: Double) {
        if (!ProfileStorageData.loaded) return

        val crop = CropType.getByNameOrNull(cropName)
        if (crop == null) {
            ChatUtils.debug("GardenCropMilestoneFix: crop is null: '$cropName'")
            return
        }

        val baseCrops = getCropsForTier(tier, crop) ?: return
        val next = getCropsForTier(tier + 1, crop) ?: return
        val progressCrops = next - baseCrops

        val progress = progressCrops * (percentage / 100)
        val smallestPercentage = progressCrops * 0.0005

        val tabListValue = baseCrops + progress - smallestPercentage

        val newValue = tabListValue.toLong()
        if (tabListCropProgress[crop] != newValue && tabListCropProgress.containsKey(crop)) {
            changedValue(crop, newValue, "tab list", smallestPercentage.toInt())
        }
        tabListCropProgress[crop] = newValue
    }

    private fun forceUpdateMilestone(crop: CropType, amount: Long) {
        if (amount == 0L) return
        crop.addMilestoneCounter(amount)
    }

    private val tabListCropProgress = mutableMapOf<CropType, Long>()

    private val loadedCrops = mutableListOf<CropType>()

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(70, "#profile.garden.cropCounter", "#profile.garden.cropMilestoneCounter")
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        cropMilestoneData = event.getConstant<GardenJson>("Garden").cropMilestones
    }
}
