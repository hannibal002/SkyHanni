package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI.addCollectionCounter
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.garden.farming.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
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

    private val config get() = GardenApi.config.cropMilestones

    private val storage get() = GardenApi.storage

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

            val crops = getCropsForTier(tier, crop)
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

        if (collectionType == CropCollectionType.UNKNOWN) return

        cropType.addMilestoneCounter(amount)
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

    var cropMilestoneData: Map<CropType, List<Int>> = emptyMap()

    val cropMilestoneCounter: MutableMap<CropType, Long>? get() = storage?.cropMilestoneCounter

    // TODO make nullable
    fun CropType.getMilestoneCounter() = cropMilestoneCounter?.get(this) ?: 0

    private fun CropType.setMilestoneCounter(counter: Long) {
        cropMilestoneCounter?.set(this, counter)
        CropMilestoneUpdateEvent.post()
    }

    fun CropType.addMilestoneCounter(counter: Long) {
        if (counter == 0L) return
        this.setMilestoneCounter(this.getMilestoneCounter() + counter)
    }

    fun CropType.isMaxed(useOverflow: Boolean): Boolean {
        if (useOverflow) return false

        // TODO change 1b
        val maxValue = cropMilestoneData[this]?.sum() ?: 1_000_000_000 // 1 bil for now
        return getMilestoneCounter() >= maxValue
    }

    fun CropType.getTier(allowOverflow: Boolean = false): Int =
        getTierForCrop(this, allowOverflow)

    private fun getTierForCrop(crop: CropType, allowOverflow: Boolean = false): Int =
        getTierForCropCount(crop.getMilestoneCounter(), crop, allowOverflow)

    fun getTierForCropCount(count: Long, crop: CropType, allowOverflow: Boolean = false): Int {
        var tier = 0
        var totalCrops = 0L
        val cropMilestone = cropMilestoneData[crop] ?: return 0
        val last = cropMilestone.last()

        for (tierCrops in cropMilestone) {
            totalCrops += tierCrops
            if (totalCrops >= count) {
                return tier
            }
            tier++
        }

        if (allowOverflow) {
            while (totalCrops < count) {
                totalCrops += last
                if (totalCrops >= count) {
                    return tier
                }
                tier++
            }
        }
        return tier
    }

    fun getMaxTier() = cropMilestoneData.values.firstOrNull()?.size ?: 0

    fun getCropsForTier(requestedTier: Int, crop: CropType, allowOverflow: Boolean = false): Long {
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

    fun CropType.progressToNextLevel(allowOverflow: Boolean = false): Double {
        val progress = getMilestoneCounter()
        val startTier = getTierForCropCount(progress, this, allowOverflow)
        val startCrops = getCropsForTier(startTier, this, allowOverflow)
        val end = getCropsForTier(startTier + 1, this, allowOverflow)
        return (progress - startCrops).toDouble() / (end - startCrops)
    }

    private fun changedValue(crop: CropType, tabListValue: Long, source: String, minDiff: Int) {
        val calculated = crop.getMilestoneCounter()
        val diff = tabListValue - calculated

        if (diff >= minDiff) {
            forceUpdateMilestone(crop, diff)
            storage?.lastMilestoneFix = SimpleTimeMark.now()
            GardenCropMilestoneDisplay.update()
            if (!loadedCrops.contains(crop)) {
                ChatUtils.chat("Loaded ${crop.cropName} milestone data from $source!")
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

        val baseCrops = getCropsForTier(tier, crop)
        val next = getCropsForTier(tier + 1, crop)
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

        val lastMilestoneFix = storage?.lastMilestoneFix ?: SimpleTimeMark.now()
        val lastCollectionFix = storage?.lastCollectionFix?.get(crop) ?: SimpleTimeMark.farPast()

        if (lastMilestoneFix > lastCollectionFix && !(HypixelData.coop)) {
            crop.addCollectionCounter(CropCollectionType.UNKNOWN, amount)
        }
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
