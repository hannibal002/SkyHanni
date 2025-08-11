package at.hannibal2.skyhanni.data.garden.cropmilestones

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI.addsToMilestone
import at.hannibal2.skyhanni.data.garden.cropmilestones.CustomGoals.getCustomGoal
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.garden.farming.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropMilestoneDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils.chat
import at.hannibal2.skyhanni.utils.ChatUtils.clickableChat
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@SkyHanniModule
object CropMilestonesAPI {
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
    val tabListPattern by patternGroup.pattern(
        "tablist",
        " (?<crop>Wheat|Carrot|Potato|Pumpkin|Sugar Cane|Melon|Cactus|Cocoa Beans|Mushroom|Nether Wart) (?<tier>\\d+): §r§a(?<percentage>.*)%",
    )

    /**
     * REGEX-TEST:   §r§b§lGARDEN MILESTONE §3Melon §845➜§346
     */
    val levelUpPattern by patternGroup.pattern(
        "levelup",
        " {2}§r§b§lGARDEN MILESTONE §3(?<crop>.*) §8.*➜§3(?<tier>.*)",
    )

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        if ((cropMilestoneCounter?.size ?: 0) == 0) inaccurateMilestone = true
    }

    @HandleEvent
    fun onCollectionAdd(event: CropCollectionAddEvent) {
        val cropType = event.crop
        val collectionType = event.cropCollectionType
        val amount = event.amount

        if (!collectionType.addsToMilestone()) return

        cropType.addMilestoneCounter(amount)
    }

    val storage get() = GardenApi.storage
    private val maxMilestoneValue: MutableMap<CropType, Long> = mutableMapOf()
    val config get() = GardenApi.config.cropMilestones
    var inaccurateMilestone = false
    var missingMilestoneRepoData = false
    var cropMilestoneRepoData: Map<CropType, List<Int>> = emptyMap()
    private var maxTier: Int? = null
    private val cropMilestoneCounter: MutableMap<CropType, Long>? get() = storage?.cropMilestoneCounter
    private var cropMilestoneTierCache: MutableMap<CropType, Int> = mutableMapOf()
    private var amountToNextMilestoneTierCache: MutableMap<CropType, Long> = mutableMapOf()

    fun getCropTypeByLore(itemStack: ItemStack): CropType? {
        cropPattern.firstMatcher(itemStack.getLore()) {
            val name = group("name")
            return CropType.getByNameOrNull(name)
        }
        return null
    }

    fun CropType.getMilestoneCounter() = cropMilestoneCounter?.get(this) ?: 0

    fun CropType.isMaxMilestone(): Boolean {
        val maxValue = this.getMaxedMilestoneAmount()
        return getMilestoneCounter() >= maxValue
    }

    fun CropType.getCurrentMilestoneTier(): Int {
        val tier = cropMilestoneTierCache.getOrPut(this) {
            this.milestoneTierFromCropCount(this.getMilestoneCounter())
        }
        return tier
    }

    fun CropType.setProgress(amount: Long) {
        amountToNextMilestoneTierCache[this] = amount
    }

    fun getMaxTier() = maxTier ?: cropMilestoneRepoData.values.firstOrNull()?.size ?: run { missingMilestoneRepoData = true; return 0 }

    fun CropType.getMaxedMilestoneAmount(): Long {
        val msVal = maxMilestoneValue.getOrPut(this) {
            this.getCropMilestoneData()
        }

        return msVal
    }

    fun CropType.milestoneProgressToNextTier(): Long {
        return amountToNextMilestoneTierCache.getOrPut(this) {
            this.milestoneCalculateTierProgress()
        }
    }

    fun CropType.percentToNextMilestone(): Double {
        val progressAmount = this.milestoneProgressToNextTier()
        val limit = this.milestoneNextTierAmount()
        val percent = (progressAmount.toDouble() / limit)
        return percent
    }

    fun CropType.milestoneNextTierAmount(): Long {
        return this.milestoneTierAmount((this.getCurrentMilestoneTier()) + 1)
    }

    fun CropType.milestoneTierAmount(tier: Int): Long { // get the amount of crops for only that tier, eg ms. 46 is 3m
        if (tier <= 0) return 0
        val overflowTier = minOf(tier - 1, getMaxTier() - 1)
        val data = this.getMilestoneTiersList()
        return data[overflowTier].toLong()
    }

    fun CropType.milestoneTotalCropsForTier(requestedTier: Int): Long {
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

    internal fun CropType.addMilestoneCounter(counter: Long) {
        if (counter == 0L) return
        amountToNextMilestoneTierCache[this] = amountToNextMilestoneTierCache[this]?.plus(counter) ?: counter
        this.setMilestoneCounter(this.getMilestoneCounter() + counter)
        this.milestoneCheckProgress()
        CropMilestoneUpdateEvent.post()
    }

    private fun CropType.setMilestoneTier(tier: Int): Int {
        cropMilestoneTierCache[this] = tier
        return tier
    }

    private fun CropType.setMilestoneCounter(counter: Long) { // only call this with addMilestoneCounter
        cropMilestoneCounter?.set(this, counter)
    }

    private fun CropType.milestoneCheckProgress() {
        val tierProgress = this.milestoneProgressToNextTier()
        val tierCutoff = this.milestoneNextTierAmount()
        val maxTier = getMaxTier()

        if (tierProgress >= tierCutoff) {
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
            val level = this.milestoneTierFromCropCount(this.getMilestoneCounter())
            this.setMilestoneTier(level)
            this.milestoneCalculateTierProgress().let { this.setProgress(it) }
        }
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

    private fun CropType.milestoneTierFromCropCount(count: Long): Int {
        var tier = 0
        var totalCrops = 0L
        val cropMilestone = this.getMilestoneTiersList()
        val maxMilestoneAmount = this.getMaxedMilestoneAmount()

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


        tier = getMaxTier()

        totalCrops = count - maxMilestoneAmount
        tier += totalCrops.floorDiv(last).toInt()

        return tier
    }

    private fun CropType.milestoneCalculateTierProgress(): Long {
        val progress = getMilestoneCounter()
        val startTier = this.getCurrentMilestoneTier()
        val startCrops = this.milestoneTotalCropsForTier(startTier)
        val tierProgress = (progress - startCrops)

        return tierProgress
    }

    private fun onOverflowLevelUp(crop: CropType, oldLevel: Int, newLevel: Int) {
        val customGoalLevel = crop.getCustomGoal() ?: 0
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

    internal fun clearMilestoneCache() {
        cropMilestoneTierCache.clear()
        amountToNextMilestoneTierCache.clear()
        maxTier = null
        GardenCropMilestoneDisplay.update()
    }

    private fun resetMilestones() {
        cropMilestoneCounter?.clear()
        clearMilestoneCache()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(70, "#profile.garden.cropCounter", "#profile.garden.cropMilestoneCounter")
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        cropMilestoneRepoData = event.getConstant<GardenJson>("Garden").cropMilestones
        missingMilestoneRepoData = false
        CustomGoals.loadCustomGoals()
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
        event.registerBrigadier("shshowcropcache") {
            description = "Show Cached Milestone Information."
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                for (crop in cropMilestoneTierCache) {
                    chat("Crop: ${crop.key}, Tier: ${crop.value}")
                }
                for (crop in amountToNextMilestoneTierCache) {
                    chat("Crop: ${crop.key}, Progress: ${crop.value}")
                }
            }
        }
    }
}
