package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.CropMilestonesConfig.MilestoneTextEntry
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.MushroomPetPerkConfig.MushroomTextEntry
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getMilestoneCounter
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getProgressToNextTier
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getMilestoneTier
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getTierAmount
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.isMaxMilestone
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.garden.DisplayCropChange
import at.hannibal2.skyhanni.events.garden.farming.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.setSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenCropMilestoneDisplay {

    private var progressDisplay = emptyList<Renderable>()
    private var mushroomCowPerkDisplay = emptyList<Renderable>()
    private val config get() = GardenApi.config.cropMilestones
    private val overflowConfig get() = config.overflow
    private val storage get() = ProfileStorageData.profileSpecific?.garden?.customGoalMilestone

    private var countdownTitleContext: TitleContext? = null
    private var lastTitleWarnedLevel = -1
    private var needsInventory = false

    private var lastWarnedLevel = -1
    private var previousNext = 0

    private var lastMushWarnedLevel = -1
    private var previousMushNext = 0

    private var currentCrop: CropType? = null

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            config.bestShowMaxedNeeded,
            config.highestTimeFormat,
        ) {
            GardenBestCropTime.updateTimeTillNextCrop()
            update()
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (GardenApi.hideExtraGuis()) return

        config.progressDisplayPos.renderRenderables(
            progressDisplay, posLabel = "Crop Milestone Progress",
        )

        if (config.mushroomPetPerk.enabled) {
            config.mushroomPetPerk.pos.renderRenderables(
                mushroomCowPerkDisplay, posLabel = "Mushroom Cow Perk",
            )
        }

        if (config.next.bestDisplay) {
            config.next.displayPos.renderRenderable(GardenBestCropTime.display, posLabel = "Best Crop Time")
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onProfileJoin(event: ProfileJoinEvent) {
        GardenCropMilestones.cropMilestoneCounter?.let {
            if (it.values.sum() == 0L) {
                needsInventory = true
            }
        }
    }

    @HandleEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        needsInventory = false
        GardenBestCropTime.updateTimeTillNextCrop()
        update()
    }

    @HandleEvent
    fun onDisplayCropChange(event: DisplayCropChange) {
        currentCrop = event.crop
        update()
    }

    fun update() {
        progressDisplay = emptyList()
        mushroomCowPerkDisplay = emptyList()
        GardenBestCropTime.display = null
        val displayCrop =
            currentCrop ?: if (config.showWithoutTool) CropCollectionAPI.lastGainedCrop else GardenApi.getCurrentlyFarmedCrop()
        displayCrop?.let {
            progressDisplay = drawProgressDisplay(it)
        }

        if (config.next.bestDisplay && config.next.bestAlwaysOn.get() || displayCrop != null) {
            GardenBestCropTime.display = GardenBestCropTime.drawBestDisplay(displayCrop)
        }
    }

    private fun drawProgressDisplay(crop: CropType): List<Renderable> {
        val counter = crop.getMilestoneCounter()
        val lineMap = mutableMapOf<MilestoneTextEntry, Renderable>()
        val errorMessage = listOf<Renderable>(Renderable.text("§eError: Repo failed to load."))
        lineMap[MilestoneTextEntry.TITLE] = Renderable.text("§6Crop Milestones")

        val customTargetLevel = storage?.get(crop) ?: 0
        val overflowDisplay = overflowConfig.cropMilestoneDisplay
        val allowOverflow = overflowDisplay || (customTargetLevel != 0)
        val currentTier = crop.getMilestoneTier() ?: return errorMessage
        var nextTier = if (config.bestShowMaxedNeeded.get() && currentTier <= 46) 46 else currentTier + 1
        val nextRealTier = nextTier
        val useCustomGoal = customTargetLevel != 0 && customTargetLevel > currentTier
        nextTier = if (useCustomGoal) customTargetLevel else nextTier

        lineMap[MilestoneTextEntry.MILESTONE_TIER] = Renderable.horizontal {
            addItemStack(crop.icon)
            if (crop.isMaxMilestone() == true && !overflowDisplay) {
                addString("§7" + crop.cropName + " §eMAXED")
            } else {
                addString("§7" + crop.cropName + " §8$currentTier➜§3$nextTier")
            }
        }

        val allowOverflowOrCustom = overflowDisplay || useCustomGoal
        val cropsForNextTier = crop.getTierAmount(currentTier + 1) ?: return errorMessage
        val (have, need) = if (config.bestShowMaxedNeeded.get() && !overflowDisplay) {
            Pair(counter, cropsForNextTier)
        } else {
            val have = if (useCustomGoal) counter else crop.getProgressToNextTier() ?: return errorMessage
            val need = if (useCustomGoal) {
                cropsForNextTier
            } else {
                crop.getTierAmount(currentTier + 1) ?: return errorMessage
            }
            Pair(have, need)
        }

        lineMap[MilestoneTextEntry.NUMBER_OUT_OF_TOTAL] = if (crop.isMaxMilestone() == true && !overflowDisplay) {
            val haveFormat = counter.addSeparators()
            Renderable.text("§7Counter: §e$haveFormat")
        } else {
            val haveFormat = have.addSeparators()
            val needFormat = need.addSeparators()
            Renderable.text("§e$haveFormat§8/§e$needFormat")
        }

        val farmingFortune = FarmingFortuneDisplay.getCurrentFarmingFortune()
        val speed = GardenCropSpeed.averageBlocksPerSecond
        val farmingFortuneSpeed = ((100.0 + farmingFortune) * crop.baseDrops * speed / 100).roundTo(1).toInt()

        if (farmingFortuneSpeed > 0) {
            crop.setSpeed(farmingFortuneSpeed)
            if (crop.isMaxMilestone() == false || overflowDisplay) {
                val missing = need - have
                val missingTime = (missing / farmingFortuneSpeed).seconds
                val millis = missingTime.inWholeMilliseconds
                GardenBestCropTime.timeTillNextCrop[crop] = millis.milliseconds
                tryWarn(missingTime, "§b${crop.cropName} $nextTier in %t", crop)
                val biggestUnit = config.highestTimeFormat.get().timeUnit
                val duration = missingTime.format(biggestUnit)
                val speedText = "§7In §b$duration"
                lineMap[MilestoneTextEntry.TIME] = Renderable.text(speedText)
                GardenApi.itemInHand?.let {
                    if (GardenApi.readCounter(it) == null) {
                        lineMap[MilestoneTextEntry.TIME] = Renderable.text("$speedText §7Inaccurate!")
                    }
                }
            }

            val secondFormat = (farmingFortuneSpeed).addSeparators()
            lineMap[MilestoneTextEntry.CROPS_PER_SECOND] = Renderable.text("§7Crops/Second§8: §e$secondFormat")

            val minuteFormat = (farmingFortuneSpeed * 60).addSeparators()
            lineMap[MilestoneTextEntry.CROPS_PER_MINUTE] = Renderable.text("§7Crops/Minute§8: §e$minuteFormat")

            val hourFormat = (farmingFortuneSpeed * 60 * 60).addSeparators()
            lineMap[MilestoneTextEntry.CROPS_PER_HOUR] = Renderable.text("§7Crops/Hour§8: §e$hourFormat")

            val formatBps = speed.roundTo(config.blocksBrokenPrecision).addSeparators()
            lineMap[MilestoneTextEntry.BLOCKS_PER_SECOND] = Renderable.text("§7Blocks/Second§8: §e$formatBps")
        }

        val percentageFormat = (have.toDouble() / need.toDouble()).formatPercentage()
        lineMap[MilestoneTextEntry.PERCENTAGE] = if (crop.isMaxMilestone() == true && !overflowDisplay) {
            Renderable.text("§7Percentage: §e100%")
        } else {
            Renderable.text("§7Percentage: §e$percentageFormat")
        }

        if (overflowConfig.chat) {
            if (currentTier > 46 && currentTier == previousNext &&
                nextRealTier == currentTier + 1 && lastWarnedLevel != currentTier
            ) {
                GardenCropMilestones.onOverflowLevelUp(crop, currentTier - 1, nextRealTier - 1)
                lastWarnedLevel = currentTier
            }
        }

        if (GardenApi.mushroomCowPet && crop != CropType.MUSHROOM) {
            addMushroomCowData()
        }

        previousNext = nextRealTier

        return formatDisplay(lineMap)
    }

    private fun tryWarn(timeLeft: Duration, title: String, crop: CropType) {
        val isConfigEnabled = config.warnClose
        val isCropBreakEnabled = (GardenCropSpeed.lastBrokenTime.passedSince() < 500.milliseconds)
        val isTimeLeftValid = timeLeft <= 6.seconds

        if (!isConfigEnabled || !isCropBreakEnabled || !isTimeLeftValid) {
            countdownTitleContext?.stop()
            countdownTitleContext = null
            return
        }

        lastTitleWarnedLevel = crop.getMilestoneTier().takeIf { it != lastTitleWarnedLevel } ?: return
        if (needsInventory || countdownTitleContext != null) return

        countdownTitleContext = TitleManager.sendTitle(
            title,
            duration = timeLeft,
            addType = TitleManager.TitleAddType.FORCE_FIRST,
            countDownDisplayType = TitleManager.CountdownTitleDisplayType.WHOLE_SECONDS,
            onInterval = SoundUtils::playBeepSound,
        )
    }

    // TODO Dropdown Menu
    private fun formatDisplay(lineMap: MutableMap<MilestoneTextEntry, Renderable>): List<Renderable> {
        val newList = mutableListOf<Renderable>()
        if (InventoryUtils.inInventory() || InventoryUtils.inContainer()) {
            newList.add(
                Renderable.clickable(
                    "§7[§a${currentCrop ?: "Default"}§7]",
                    tips = listOf("Click for next crop"),
                    onLeftClick = {
                        selectNextCrop()
                        update()
                        DisplayCropChange(currentCrop).post()
                    }
                )
            )
        }

        newList.addAll(config.text.mapNotNull { lineMap[it] })

        if (needsInventory) {
            newList.addString("§cOpen §e/cropmilestones §cto update!")
        }

        return newList
    }

    private fun addMushroomCowData() {
        val mushroom = CropType.MUSHROOM
        val allowOverflow = overflowConfig.cropMilestoneDisplay
        if (mushroom.isMaxMilestone() == true && !allowOverflow) {
            mushroomCowPerkDisplay = listOf(
                Renderable.text("§6Mooshroom Cow Perk"),
                Renderable.text("§eMushroom crop is maxed!"),
            )
            return
        }

        val lineMap = HashMap<MushroomTextEntry, Renderable>()

        val currentTier = mushroom.getMilestoneTier() ?: return
        val nextTier = currentTier + 1

        val have = mushroom.getProgressToNextTier() ?: return
        val need = mushroom.getTierAmount(nextTier) ?: return

        val haveFormat = have.addSeparators()
        val needFormat = need.addSeparators()

        val missing = need - have

        lineMap[MushroomTextEntry.TITLE] = Renderable.text("§6Mooshroom Cow Perk")
        lineMap[MushroomTextEntry.MUSHROOM_TIER] = Renderable.horizontal {
            addItemStack(mushroom.icon)
            addString("§7Mushroom Milestone $nextTier")
        }

        lineMap[MushroomTextEntry.NUMBER_OUT_OF_TOTAL] = Renderable.text("§e$haveFormat§8/§e$needFormat")

        val speed = GardenCropSpeed.averageBlocksPerSecond
        if (speed != 0.0) {
            val blocksPerSecond = speed * (GardenApi.getCurrentlyFarmedCrop()?.multiplier ?: 1)

            val missingTime = (missing / blocksPerSecond).seconds
            val biggestUnit = config.highestTimeFormat.get().timeUnit
            val duration = missingTime.format(biggestUnit)
            lineMap[MushroomTextEntry.TIME] = Renderable.text("§7In §b$duration")
        }

        val percentageFormat = (have.toDouble() / need.toDouble()).formatPercentage()
        lineMap[MushroomTextEntry.PERCENTAGE] = Renderable.text("§7Percentage: §e$percentageFormat")

        if (currentTier > 46 && currentTier == previousMushNext && nextTier == currentTier + 1 && lastMushWarnedLevel != currentTier) {
            GardenCropMilestones.onOverflowLevelUp(mushroom, currentTier - 1, nextTier - 1)
            lastMushWarnedLevel = currentTier
        }

        previousMushNext = nextTier
        mushroomCowPerkDisplay = config.mushroomPetPerk.text.mapNotNull { lineMap[it] }
    }

    private fun selectNextCrop() {
        currentCrop = if (currentCrop == null) CropType.entries.first()
        else currentCrop?.let { sb ->
            CropType.entries.filter { it.ordinal > sb.ordinal }.minByOrNull { it.ordinal }
        }
    }

    private fun isEnabled() = GardenApi.inGarden() && config.progress

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.cropMilestoneProgress", "garden.cropMilestones.progress")
        event.move(3, "garden.cropMilestoneWarnClose", "garden.cropMilestones.warnClose")
        event.move(3, "garden.cropMilestoneHighestTimeFormat", "garden.cropMilestones.highestTimeFormat")
        event.move(3, "garden.cropMilestoneBestShowMaxedNeeded", "garden.cropMilestones.bestShowMaxedNeeded")
        event.move(3, "garden.cropMilestoneText", "garden.cropMilestones.text")
        event.move(3, "garden.blocksBrokenPrecision", "garden.cropMilestones.blocksBrokenPrecision")
        event.move(3, "garden.cropMilestoneProgressDisplayPos", "garden.cropMilestones.progressDisplayPos")
        event.move(3, "garden.cropMilestoneBestDisplay", "garden.cropMilestones.next.bestDisplay")
        event.move(3, "garden.cropMilestoneBestAlwaysOn", "garden.cropMilestones.next.bestAlwaysOn")
        event.move(3, "garden.cropMilestoneNextDisplayPos", "garden.cropMilestones.next.displayPos")
        event.move(3, "garden.cropMilestoneMushroomPetPerkEnabled", "garden.cropMilestones.mushroomPetPerk.enabled")
        event.move(3, "garden.cropMilestoneMushroomPetPerkText", "garden.cropMilestones.mushroomPetPerk.text")
        event.move(3, "garden.cropMilestoneMushroomPetPerkPos", "garden.cropMilestones.mushroomPetPerk.pos")
    }
}
