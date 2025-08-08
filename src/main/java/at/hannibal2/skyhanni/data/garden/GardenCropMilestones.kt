package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI.addsToMilestone
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
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
    private val config get() = GardenApi.config.cropMilestones
    var inaccurateMilestone = false

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
        inaccurateMilestone = false
        storage?.lastMilestoneFix = SimpleTimeMark.now()
        GardenCropMilestonesCommunityFix.openInventory(event.inventoryItems)
        clearMilestoneCache()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGardenJoin(event: IslandChangeEvent) {
        if ((cropMilestoneCounter?.size ?: 0) < cropMilestoneData.size) inaccurateMilestone = true
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
    var percentToNextTier: Double? = null
    private var cropMilestoneTierCache: MutableMap<CropType, Int?> = mutableMapOf()
    private var amountToNextMilestoneTierCache: MutableMap<CropType, Long?> = mutableMapOf()

    fun CropType.getMilestoneCounter() = cropMilestoneCounter?.get(this) ?: 0

    fun CropType.addMilestoneCounter(counter: Long) {
        if (counter == 0L) return
        ChatUtils.debug("Adding Milestone Counter: Crop: $this Amount: $counter")
        amountToNextMilestoneTierCache[this] = amountToNextMilestoneTierCache[this]?.plus(counter) ?: counter
        this.checkPercentage()
        this.setMilestoneCounter(this.getMilestoneCounter() + counter)
    }

    private fun CropType.checkPercentage() {
        val percent = this.percentToNextTier() ?: return
        if (percent >= 100) {
            val levelUpTimes = percent.toInt().floorDiv(100)
            val oldLevel = getMilestoneTier() ?: return
            val newLevel = oldLevel + levelUpTimes

            if (config.overflow.chat) {
                if (newLevel > (getMaxTier() ?: return)) {
                    onOverflowLevelUp(this, maxOf(oldLevel, getMaxTier() ?: return), newLevel)
                }
            }
        }
        percentToNextTier = percent
    }

    private fun CropType.setMilestoneCounter(counter: Long) { //only call this with addMilestoneCounter
        ChatUtils.debug("Setting Milestone Counter: Crop: $this Amount: $counter")
        cropMilestoneCounter?.set(this, counter)
        CropMilestoneUpdateEvent.post()
    }

    fun CropType.isMaxMilestone(): Boolean? {
        val maxValue = this.getMaxedMilestoneAmount() ?: return null
        return getMilestoneCounter() >= maxValue
    }

    fun CropType.getMilestoneTier(): Int? {
        val tier = cropMilestoneTierCache.getOrPut(this) {
            calculateTierForCropCount(this.getMilestoneCounter(), this)
        }
        return tier
    }

    fun getMaxTier() = cropMilestoneData.values.firstOrNull()?.size

    private fun CropType.getMaxedMilestoneAmount(): Int? {
        return maxMilestoneValue.getOrPut(this) {
            cropMilestoneData[this]?.sum() ?: return null
        }
    }

    private fun calculateTierForCropCount(count: Long?, crop: CropType): Int? {
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

        totalCrops = count - maxMilestoneAmount
        tier += totalCrops.floorDiv(last).toInt()

        return tier
    }

    fun getCropsForTier(requestedTier: Int?, crop: CropType): Long? {
        if (requestedTier == null) return null

        var totalCrops = 0L
        var tier = 0
        val cropMilestone = cropMilestoneData[crop] ?: return 0
        val definedTiers = cropMilestone.size

        if (requestedTier <= definedTiers) {
            for (tierCrops in cropMilestone) {
                totalCrops += tierCrops
                tier++
                if (tier == requestedTier) {
                    return totalCrops
                }
            }

            return totalCrops
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

    fun CropType.getProgressToNextTier(): Long? {
        return amountToNextMilestoneTierCache.getOrPut(this) {
            this.calculateProgressToNextTier()
        }
    }

    fun CropType.percentToNextTier(): Double? {
        val progressAmount = getProgressToNextTier() ?: return null
        val percent =
            progressAmount.toDouble() / (this.getTierAmount(this.getMilestoneTier())?.toDouble() ?: return null)
        return percent
    }

    private fun CropType.calculateProgressToNextTier(): Long? {
        val progress = getMilestoneCounter()
        val startTier = this.getMilestoneTier() ?: return null
        val startCrops = getCropsForTier(startTier, this) ?: return null
        val end = this.getTierAmount(startTier + 1) ?: return null
        val amount = end - (progress - startCrops)

        ChatUtils.debug("Progress: $progress")
        ChatUtils.debug("Start Tier: $startTier")
        ChatUtils.debug("Start Crops: $startCrops")
        ChatUtils.debug("End Tier: $end")
        ChatUtils.debug("Amount: $amount")

        return amount
    }

    fun CropType.getTierAmount(tier: Int?): Long? {
        if (tier == null) return null
        val overflowTier = minOf(tier - 1, (getMaxTier()?.minus(1)) ?: return null)
        val data = cropMilestoneData[this] ?: return null
        return data.getOrNull(overflowTier)?.toLong()
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
        ChatUtils.debug("Force Updating Milestone: Crop: $crop, Amount: ${amount.addSeparators()}")
        crop.addMilestoneCounter(amount)
    }

    private fun clearMilestoneCache() {
        cropMilestoneTierCache.clear()
        amountToNextMilestoneTierCache.clear()
    }

    private fun resetMilestones() {
        cropMilestoneCounter?.clear()
        clearMilestoneCache()
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
        clearMilestoneCache()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetcropmilestones") {
            description = "Resets crop milestones."
            category = CommandCategory.DEVELOPER_DEBUG
            literal("reset") {
                callback {
                    resetMilestones()
                    chat("§cReset Crop Milestones!")
                }
            }
        }
    }

    @HandleEvent
    fun showCache(event: CommandRegistrationEvent) {
        event.registerBrigadier("shshowcropcache") {
            description = "Show Cached Milestone Information."
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                for (crop in cropMilestoneTierCache) {
                    ChatUtils.chat("Crop: ${crop.key}, Tier: ${crop.value}")
                }
                for (crop in amountToNextMilestoneTierCache) {
                    chat("Crop: ${crop.key}, Progress: ${crop.value}")
                }
            }
        }
    }
}
