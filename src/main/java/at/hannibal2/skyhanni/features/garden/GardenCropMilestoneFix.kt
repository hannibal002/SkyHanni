package at.hannibal2.hanni.features.garden

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.GardenCropMilestones
import at.hannibal2.hanni.data.GardenCropMilestones.getCounter
import at.hannibal2.hanni.data.GardenCropMilestones.setCounter
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.events.WidgetUpdateEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.garden.farming.GardenCropMilestoneDisplay
import at.hannibal2.hanni.features.garden.pests.PestApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object GardenCropMilestoneFix {
    private val patternGroup = RepoPattern.group("garden.cropmilestone.fix")

    /**
     * REGEX-TEST:  Cocoa Beans 31: §r§a68%
     * REGEX-TEST:  Potato 32: §r§a97.7%
     */
    @Suppress("MaxLineLength")
    private val tabListPattern by patternGroup.pattern(
        "tablist",
        " (?<crop>Wheat|Carrot|Potato|Pumpkin|Sugar Cane|Melon|Cactus|Cocoa Beans|Mushroom|Nether Wart) (?<tier>\\d+): §r§a(?<percentage>.*)%",
    )
    private val levelUpPattern by patternGroup.pattern(
        "levelup",
        " {2}§r§b§lGARDEN MILESTONE §3(?<crop>.*) §8.*➜§3(?<tier>.*)",
    )

    /**
     * REGEX-TEST: §6§lRARE DROP! §9Mutant Nether Wart §6(§6+1,344☘)
     */
    private val pestRareDropPattern by patternGroup.pattern(
        "pests.raredrop",
        "§6§lRARE DROP! (?:§.)*(?<item>.+) §6\\(§6\\+.*☘\\)",
    )

    private val tabListCropProgress = mutableMapOf<CropType, Long>()

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        levelUpPattern.matchMatcher(event.message) {
            val cropName = group("crop")
            val crop = CropType.getByNameOrNull(cropName) ?: return

            val tier = group("tier").romanToDecimalIfNecessary()

            val crops = GardenCropMilestones.getCropsForTier(tier, crop)
            changedValue(crop, crops, "level up chat message", 0)
        }
        PestApi.pestDeathChatPattern.matchMatcher(event.message) {
            val amount = group("amount").toInt()
            val item = NeuInternalName.fromItemNameOrNull(group("item")) ?: return

            val primitiveStack = NeuItems.getPrimitiveMultiplier(item)
            val rawName = primitiveStack.internalName.itemNameWithoutColor
            val cropType = CropType.getByNameOrNull(rawName) ?: return

            cropType.setCounter(
                cropType.getCounter() + (amount * primitiveStack.amount),
            )
            GardenCropMilestoneDisplay.update()
        }
        pestRareDropPattern.matchMatcher(event.message) {
            val item = NeuInternalName.fromItemNameOrNull(group("item")) ?: return

            val primitiveStack = NeuItems.getPrimitiveMultiplier(item)
            val rawName = primitiveStack.internalName.itemNameWithoutColor
            val cropType = CropType.getByNameOrNull(rawName) ?: return

            cropType.setCounter(
                cropType.getCounter() + primitiveStack.amount,
            )
            GardenCropMilestoneDisplay.update()
        }
    }

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.CROP_MILESTONE)) return
        tabListPattern.firstMatcher(event.lines) {
            val tier = group("tier").toInt()
            val percentage = group("percentage").toDouble()
            val cropName = group("crop")

            check(cropName, tier, percentage)
        }
    }

    private fun check(cropName: String, tier: Int, percentage: Double) {
        if (!ProfileStorageData.loaded) return

        val crop = CropType.getByNameOrNull(cropName)
        if (crop == null) {
            ChatUtils.debug("GardenCropMilestoneFix: crop is null: '$cropName'")
            return
        }

        val baseCrops = GardenCropMilestones.getCropsForTier(tier, crop)
        val next = GardenCropMilestones.getCropsForTier(tier + 1, crop)
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

    private val loadedCrops = mutableListOf<CropType>()

    private fun changedValue(crop: CropType, tabListValue: Long, source: String, minDiff: Int) {
        val calculated = crop.getCounter()
        val diff = calculated - tabListValue

        if (diff <= -minDiff) {
            crop.setCounter(tabListValue)
            GardenCropMilestoneDisplay.update()
            if (!loadedCrops.contains(crop)) {
                ChatUtils.chat("Loaded ${crop.cropName} milestone data from $source!")
                loadedCrops.add(crop)
            }
        } else if (diff >= minDiff) {
            ChatUtils.debug("Fixed wrong ${crop.cropName} milestone data from $source: ${diff.addSeparators()}")
            crop.setCounter(tabListValue)
            GardenCropMilestoneDisplay.update()
        }
    }
}
