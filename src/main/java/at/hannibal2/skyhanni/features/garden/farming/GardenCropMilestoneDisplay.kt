package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.CropMilestonesConfig.MilestoneTextEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getMilestoneCounter
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.milestoneProgressToNextTier
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getCurrentMilestoneTier
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getCustomGoal
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getMaxTier
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.isMaxMilestone
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.milestoneNextTierAmount
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.percentToNextMilestone
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.garden.farming.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// TODO update display on overflow config toggle
@SkyHanniModule
object GardenCropMilestoneDisplay {
    private var progressDisplay = emptyList<Renderable>()
    private var mushroomCowPerkDisplay = emptyList<Renderable>()
    private val config get() = GardenApi.config.cropMilestones
    private val overflowConfig get() = config.overflow
    private val storage get() = ProfileStorageData.profileSpecific?.garden?.customGoalMilestone // TODO configFix

    private var countdownTitleContext: TitleContext? = null
    private var lastTitleWarnedLevel = -1
    private var needsInventory = false

    private var lastWarnedLevel = -1
    private var previousNext = 0

    private var lastMushWarnedLevel = -1
    private var previousMushNext = 0

    private var displayCrop: CropType? = null
    private var defaultCrop: CropType? = null

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
            config.showMaxTier,
            config.highestTimeFormat,
            config.overflow.cropMilestoneDisplay
        ) {
            // GardenBestCropTime.updateTimeTillNextCrop()
            update()
        }
    }

    // TODO move best crop time and mooshroom cow displays into separate objects
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (GardenApi.hideExtraGuis()) return

        config.progressDisplayPos.renderRenderables(
            progressDisplay, posLabel = "Crop Milestone Progress",
        )

        /* if (config.mushroomPetPerk.enabled) {
            config.mushroomPetPerk.pos.renderRenderables(
                mushroomCowPerkDisplay, posLabel = "Mushroom Cow Perk",
            )
        }*/

        /* if (config.next.bestDisplay) {
            config.next.displayPos.renderRenderable(GardenBestCropTime.display, posLabel = "Best Crop Time")
        }*/
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
        // needsInventory = false
        // GardenBestCropTime.updateTimeTillNextCrop()
        update()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onOpenInventory(event: InventoryOpenEvent) {
        update()
    }

    fun update() {
        progressDisplay = emptyList()
        // mushroomCowPerkDisplay = emptyList()
        // GardenBestCropTime.display = null
        val crop =
            displayCrop ?: getDefaultCrop()

        crop?.let {
            progressDisplay = drawProgressDisplay(it)
        }

        /* if (config.next.bestDisplay && config.next.bestAlwaysOn.get() || displayCrop != null) {
            GardenBestCropTime.display = GardenBestCropTime.drawBestDisplay(displayCrop)
        }*/
    }

    private fun getDefaultCrop(): CropType? {
        ChatUtils.debug("Get default crop")
        return if (config.showWithoutTool) {
            CropCollectionAPI.lastGainedCrop
        } else {
            GardenApi.getCurrentlyFarmedCrop()
        }
    }

    private fun drawProgressDisplay(crop: CropType): List<Renderable> {
        ChatUtils.debug("Rendering display")
        val counter = crop.getMilestoneCounter()
        val lineMap = mutableMapOf<MilestoneTextEntry, Renderable>()
        val errorMessage = listOf<Renderable>(Renderable.text("§eError: Repo failed to load."))

        lineMap[MilestoneTextEntry.TITLE] = Renderable.text("§6Crop Milestones")

        val customGoal = crop.getCustomGoal()?.tier
        val overflowDisplay = overflowConfig.cropMilestoneDisplay.get()
        val currentTier = crop.getCurrentMilestoneTier() ?: return errorMessage
        val maxTier = getMaxTier() ?: return errorMessage
        var nextTier = if (config.showMaxTier.get() && currentTier <= maxTier) maxTier else currentTier + 1
        val useCustomGoal = customGoal != null && customGoal > 0 && customGoal > nextTier

        nextTier = if (useCustomGoal) customGoal!! else nextTier

        lineMap[MilestoneTextEntry.MILESTONE_TIER] = Renderable.horizontal {
            addItemStack(crop.icon)
            if (crop.isMaxMilestone() == true && !overflowDisplay) {
                addString("§7" + crop.cropName + " §eMAXED")
            } else {
                addString("§7" + crop.cropName + " §8$currentTier➜§3$nextTier")
            }
        }

        val (have, need) = getHaveNeed(overflowDisplay, counter, useCustomGoal, crop)
        lineMap[MilestoneTextEntry.NUMBER_OUT_OF_TOTAL] = if (crop.isMaxMilestone() == true && !overflowDisplay) {
            val haveFormat = counter.addSeparators()
            Renderable.text("§7Counter: §e$haveFormat")
        } else {
            val haveFormat = have.addSeparators()
            val needFormat = need.addSeparators()
            Renderable.text("§e$haveFormat§8/§e$needFormat")
        }

        val percentageFormat = crop.percentToNextMilestone()?.formatPercentage() ?: return errorMessage
        lineMap[MilestoneTextEntry.PERCENTAGE] = if (crop.isMaxMilestone() == true && !overflowDisplay) {
            Renderable.text("§7Percentage: §e100%")
        } else {
            Renderable.text("§7Percentage: §e$percentageFormat")
        }

        /*val farmingFortune = FarmingFortuneDisplay.getCurrentFarmingFortune()
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

        if (GardenApi.mushroomCowPet && crop != CropType.MUSHROOM) {
            addMushroomCowData()
        }

        previousNext = nextRealTier*/

        return formatDisplay(lineMap)
    }

    private fun getHaveNeed(overflowDisplay: Boolean, counter: Long, useCustomGoal: Boolean, crop: CropType): Pair<Long, Long> {
        if (config.showMaxTier.get() && !overflowDisplay) {
            return Pair(counter, crop.milestoneNextTierAmount() ?: 0)
        } else {
            val have = if (useCustomGoal) counter else crop.milestoneProgressToNextTier() ?: 0
            val need = if (useCustomGoal) {
                crop.getCustomGoal()?.cropAmount ?: 0
            } else {
                crop.milestoneNextTierAmount() ?: 0
            }
            return Pair(have, need)
        }
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

        lastTitleWarnedLevel = crop.getCurrentMilestoneTier().takeIf { it != lastTitleWarnedLevel } ?: return
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
                    "§7[§a${displayCrop ?: "Default"}§7]",
                    tips = listOf("Click for next crop"),
                    onLeftClick = {
                        selectNextCrop()
                        update()
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

    // TODO separate display
    /*private fun addMushroomCowData() {
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

        val currentTier = mushroom.getCurrentMilestoneTier() ?: return
        val nextTier = currentTier + 1

        val have = mushroom.milestoneProgressToNextTier() ?: return
        val need = mushroom.milestoneTierAmount(nextTier) ?: return

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
    }*/

    private fun selectNextCrop() {
        displayCrop = if (displayCrop == null) CropType.entries.first()
        else displayCrop?.let { sb ->
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
