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
import at.hannibal2.skyhanni.features.garden.inventory.GardenCropMilestoneInventory
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
object CropMilestones {
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

    // TODO add check for max
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
    private val maxMilestoneValue: MutableMap<CropType, Long> = mutableMapOf()
    val config get() = GardenApi.config.cropMilestones
    var inaccurateMilestone = false
    var missingMilestoneRepoData = false

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

        clearMilestoneCache()
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
        inaccurateMilestone = false
        CropMilestonesCommunityFix.openInventory(event.inventoryItems)
        GardenCropMilestoneInventory.updateAverage()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGardenJoin(event: IslandChangeEvent) {
        if ((cropMilestoneCounter?.size ?: 0) < cropMilestoneRepoData.size) inaccurateMilestone = true
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        levelUpPattern.matchMatcher(event.message) {
            val cropName = group("crop")
            val crop = CropType.getByNameOrNull(cropName) ?: return

            val tier = group("tier").romanToDecimalIfNecessary()

            val crops = crop.milestoneTotalCropsForTier(tier)
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
    var cropMilestoneRepoData: Map<CropType, List<Int>> = emptyMap()
    private var maxTier: Int? = null
    private var cropMilestoneTierCache: MutableMap<CropType, Int> = mutableMapOf()
    private var amountToNextMilestoneTierCache: MutableMap<CropType, Long> = mutableMapOf()

    fun CropType.getMilestoneCounter() = cropMilestoneCounter?.get(this) ?: 0

    private fun CropType.addMilestoneCounter(counter: Long) { // Unless we move milestone fixes out of this class, this should always be called by cropCollectionAddEvent
        if (counter == 0L) return
        ChatUtils.debug("Adding Milestone Counter: Crop: $this Amount: $counter")
        amountToNextMilestoneTierCache[this] = amountToNextMilestoneTierCache[this]?.plus(counter) ?: counter
        this.setMilestoneCounter(this.getMilestoneCounter() + counter)
        this.milestoneCheckProgress()
        CropMilestoneUpdateEvent.post()
    }

    private fun CropType.setMilestoneCounter(counter: Long) { // only call this with addMilestoneCounter
        ChatUtils.debug("Setting Milestone Counter: Crop: $this Amount: $counter")
        cropMilestoneCounter?.set(this, counter)
    }

    private fun CropType.milestoneCheckProgress() {
        val tierProgress = this.milestoneProgressToNextTier()
        val tierCutoff = this.milestoneNextTierAmount()
        val maxTier = getMaxTier()

        ChatUtils.debug("Tier Progress: $tierProgress, maxTier: $maxTier, tierCutoff: $tierCutoff")

        if (tierProgress >= tierCutoff) {
            ChatUtils.debug("Level up detected!")
            val oldLevel = this.getCurrentMilestoneTier()
            val newLevel = this.milestoneCalculateCurrentTier()


            if (config.overflow.chat) {
                if (newLevel > (maxTier)) {
                    onOverflowLevelUp(this, maxOf(oldLevel, maxTier), newLevel)
                }
            }

            this.setMilestoneTier(newLevel)
            this.milestoneCalculateTierProgress().let { this.setProgress(it) }
            return
        }

        if (tierProgress < 0) {
            ChatUtils.debug("Negative progress detected! Fixing!")
            val level = this.milestoneTierFromCropCount(this.getMilestoneCounter())
            this.setMilestoneTier(level)
            this.milestoneCalculateTierProgress().let { this.setProgress(it) }
        }
    }

    fun CropType.isMaxMilestone(): Boolean {
        val maxValue = this.getMaxedMilestoneAmount()
        return getMilestoneCounter() >= maxValue
    }

    fun CropType.getCurrentMilestoneTier(): Int {
        ChatUtils.debug("Get Milestone Tier")
        val tier = cropMilestoneTierCache.getOrPut(this) {
            this.milestoneTierFromCropCount(this.getMilestoneCounter())
        }
        return tier
    }

    fun CropType.setMilestoneTier(tier: Int): Int {
        ChatUtils.debug("Setting Milestone Tier: $tier")
        cropMilestoneTierCache[this] = tier
        return tier
    }

    fun CropType.setProgress(amount: Long) {
        ChatUtils.debug("Setting Progress: $amount")
        amountToNextMilestoneTierCache[this] = amount
    }

    fun getMaxTier() = maxTier ?: cropMilestoneRepoData.values.firstOrNull()?.size ?: run { missingMilestoneRepoData = true; return 0}

    fun CropType.getMaxedMilestoneAmount(): Long {
        val msVal = maxMilestoneValue.getOrPut(this) {
            this.getCropMilestoneData()
        }
        ChatUtils.debug("Getting maxed milestone amount: $msVal")

        return msVal
    }

    private fun CropType.getCropMilestoneData(): Long {
        return this.getMilestoneTiersList().sum().toLong()
    }

    private fun CropType.getMilestoneTiersList(): List<Int> {
        return cropMilestoneRepoData[this] ?: run {
            missingMilestoneRepoData = true
            return emptyList()
        }
    }

    private fun CropType.milestoneCalculateCurrentTier(): Int {
        return this.milestoneTierFromCropCount(this.getMilestoneCounter())
    }

    // should work
    private fun CropType.milestoneTierFromCropCount(count: Long): Int {
        ChatUtils.debug("Calculating Tier for Crop Count: $count")
        var tier = 0
        var totalCrops = 0L
        val cropMilestone = this.getMilestoneTiersList()
        val maxMilestoneAmount = this.getMaxedMilestoneAmount()

        ChatUtils.debug("CalculateTierForCropCount: Tier: $tier, totalCrops: $totalCrops, maxMilestoneAmount: $maxMilestoneAmount")

        val last = cropMilestone.last()

        if (count < (maxMilestoneAmount)) {
            for (tierCrops in cropMilestone) {
                totalCrops += tierCrops
                if (totalCrops >= count) {
                    ChatUtils.debug("CalcTier: $tier")
                    return tier
                }
                tier++
            }
            ChatUtils.debug("Calc Tier: $tier")
            return tier
        }


        tier = getMaxTier()

        totalCrops = count - maxMilestoneAmount
        tier += totalCrops.floorDiv(last).toInt()

        ChatUtils.debug("CalculateTierForCropCount: Tier: $tier, totalCrops: $totalCrops, maxMilestoneAmount: $maxMilestoneAmount, last: $last")

        return tier
    }

    fun CropType.milestoneTotalCropsForTier(requestedTier: Int): Long {
        ChatUtils.debug("getCropsForTier: Requested: $requestedTier")
        if (requestedTier == 0) return 0

        var totalCrops = 0L
        var tier = 0
        val cropMilestone = this.getMilestoneTiersList()
        val definedTiers = cropMilestone.size

        if (requestedTier <= definedTiers) {
            for (tierCrops in cropMilestone) {
                totalCrops += tierCrops
                tier++
                if (tier == requestedTier) {
                    ChatUtils.debug("GetCropsForTier: $totalCrops")
                    return totalCrops
                }
            }

            ChatUtils.debug("GetCropsForTier: $totalCrops")
            return totalCrops
        }


        for (tierCrops in cropMilestone) {
            totalCrops += tierCrops
            tier++
        }

        val additionalTiers = requestedTier - definedTiers

        val lastIncrement = cropMilestone.last().toLong()

        totalCrops += lastIncrement * additionalTiers

        ChatUtils.debug("GetCropsForTier: Total Crops: $totalCrops, additionalTiers: $additionalTiers, last: $lastIncrement")
        return totalCrops
    }

    fun CropType.milestoneProgressToNextTier(): Long {
        ChatUtils.debug("GetProgressToNextTier")
        return amountToNextMilestoneTierCache.getOrPut(this) {
            this.milestoneCalculateTierProgress()
        }
    }

    fun CropType.percentToNextMilestone(): Double {
        ChatUtils.debug("PercentToNextTier")
        val progressAmount = this.milestoneProgressToNextTier()
        val limit = this.milestoneNextTierAmount()
        val percent = (progressAmount.toDouble() / limit)
        ChatUtils.debug("PercentToNextTier: $percent")
        return percent
    }

    private fun CropType.milestoneCalculateTierProgress(): Long {
        ChatUtils.debug("CalculateProgressToNextTier")
        val progress = getMilestoneCounter()
        val startTier = this.getCurrentMilestoneTier()
        val startCrops = this.milestoneTotalCropsForTier(startTier)
        val tierProgress = (progress - startCrops)

        ChatUtils.debug("CalcProgToNextTier: Progress: $progress, startTier: $startTier, startCrops: $startCrops")
        return tierProgress
    }

    fun CropType.milestoneNextTierAmount(): Long {
        return this.milestoneTierAmount((this.getCurrentMilestoneTier()) + 1)
    }

    fun CropType.milestoneTierAmount(tier: Int): Long { // get the amount of crops for only that tier, eg ms. 46 is 3m
        ChatUtils.debug("GetTierAmount")
        if (tier <= 0) return 0
        val overflowTier = minOf(tier - 1, getMaxTier() - 1)
        val data = this.getMilestoneTiersList()
        ChatUtils.debug("getTierAmount: Tier: $overflowTier, Amount: ${data.getOrNull(overflowTier)?.toLong()}")
        return data.get(overflowTier).toLong()
    }

    private fun onOverflowLevelUp(crop: CropType, oldLevel: Int, newLevel: Int) {
        val customGoalLevel = ProfileStorageData.profileSpecific?.garden?.customGoalMilestone?.get(crop) ?: 0
        val goalReached = newLevel == customGoalLevel // TODO move

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

    // TODO fix this
    private fun checkTabDifference(cropName: String, tier: Int, percentage: Double) {
        if (!ProfileStorageData.loaded) return

        val crop = CropType.getByNameOrNull(cropName)
        if (crop == null) {
            ChatUtils.debug("GardenCropMilestoneFix: crop is null: '$cropName'")
            return
        }

        val baseCrops = crop.milestoneTotalCropsForTier(tier)
        val next = crop.milestoneTotalCropsForTier(tier + 1)
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
        maxTier = null
        GardenCropMilestoneDisplay.update()
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
        cropMilestoneRepoData = event.getConstant<GardenJson>("Garden").cropMilestones
        missingMilestoneRepoData = false
        CropMilestonesCustomGoals.loadCustomGoals()
        clearMilestoneCache()
        CropMilestoneUpdateEvent.post()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetcropmilestones") {
            description = "Resets crop milestones."
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                resetMilestones()
                chat("§cReset Crop Milestones!")
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
