package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.CropMilestonesConfig.MilestoneTextEntry
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.MushroomPetPerkConfig.MushroomTextEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.CropCollectionApi
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getCurrentMilestoneTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getMaxTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getMaxedMilestoneAmount
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getMilestoneCounter
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.inaccurateMilestone
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.isMaxMilestone
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneNextTierAmount
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneProgressToNextTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.percentToNextMilestone
import at.hannibal2.skyhanni.data.garden.cropmilestones.CustomGoals.getCustomGoal
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.garden.farming.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getCurrentlyFarmedCrop
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.setSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
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
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// TODO config load updates for display and mooshroom cow
@SkyHanniModule
object GardenCropMilestoneDisplay {
    private var progressDisplay = emptyList<Renderable>()
    private var mushroomCowPerkDisplay = emptyList<Renderable>()
    private val config get() = GardenApi.config.cropMilestones
    private val overflowConfig get() = config.overflow

    private var countdownTitleContext: TitleContext? = null
    private var lastTitleWarnedLevel = -1
    private var inventoryOpen = false

    private var displayCrop: CropType? = null

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
            config.showMaxTier,
            config.highestTimeFormat,
            config.overflow.cropMilestoneDisplay
        ) {
            GardenBestCropTime.updateTimeTillNextCrop()
            update()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (GardenApi.hideExtraGuis()) return

        val currentlyOpen = InventoryUtils.inAnyInventory()

        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }
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
        update()
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onGardenJoin(event: IslandChangeEvent) {
        update()
    }

    @HandleEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        update()
    }

    @HandleEvent
    fun onToolChange(event: GardenToolChangeEvent) {
        update()
    }

    fun update() {
        progressDisplay = emptyList()
        mushroomCowPerkDisplay = emptyList()
        GardenBestCropTime.display = null
        val crop =
            displayCrop ?: getDefaultCrop()

        crop?.let {
            progressDisplay = drawProgressDisplay(it)
        }

        if (config.next.bestDisplay && config.next.bestAlwaysOn.get() || displayCrop != null) {
            GardenBestCropTime.display = GardenBestCropTime.drawBestDisplay(displayCrop)
        }
    }

    private fun getDefaultCrop(): CropType? {
        return if (config.showWithoutTool) {
            CropCollectionApi.lastGainedCrop ?: getCurrentlyFarmedCrop()
        } else {
            getCurrentlyFarmedCrop()
        }
    }

    @Suppress("DestructuringDeclarationWithTooManyEntries")
    private fun drawProgressDisplay(crop: CropType): List<Renderable> {
        val lineMap = mutableMapOf<MilestoneTextEntry, Renderable>()
        val (
            counter,
            useMaxTier,
            overflowDisplay,
            currentTier,
            nextTier,
            useCustomGoal
        ) = getMilestoneInfo(crop) ?: run {
            return formatDisplay(lineMap)
        }
        val (have, need) = getHaveNeed(crop, counter, useCustomGoal, useMaxTier) ?: run {
            return formatDisplay(lineMap)
        }

        lineMap[MilestoneTextEntry.TITLE] = Renderable.text("§6Crop Milestones")
        lineMap[MilestoneTextEntry.MILESTONE_TIER] = tiersRenderable(crop, currentTier, nextTier, overflowDisplay)
        lineMap[MilestoneTextEntry.NUMBER_OUT_OF_TOTAL] = haveNeedRenderable(crop, counter, have, need, overflowDisplay)
        lineMap[MilestoneTextEntry.PERCENTAGE] = percentRenderable(crop, overflowDisplay)

        // TODO move everything underneath this to new display
        // This was already fairly inaccurate and still is, not touching this until moving to new display
        val farmingFortune = FarmingFortuneDisplay.getCurrentFarmingFortune()
        val speed = GardenCropSpeed.averageBlocksPerSecond
        val farmingFortuneSpeed = ((100.0 + farmingFortune) * crop.baseDrops * speed / 100).roundTo(1).toInt()

        if (farmingFortuneSpeed > 0 && crop == getCurrentlyFarmedCrop()) {
            crop.setSpeed(farmingFortuneSpeed)
            if (!crop.isMaxMilestone() || overflowDisplay) {
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

        return formatDisplay(lineMap)
    }

    private fun getMilestoneInfo(crop: CropType): MilestoneInfo? {
        val counter = crop.getMilestoneCounter() ?: return null
        val customGoal = crop.getCustomGoal()?.tier ?: 0
        val overflowDisplay = overflowConfig.cropMilestoneDisplay.get()
        val currentTier = crop.getCurrentMilestoneTier() ?: return null
        val maxTier = getMaxTier()
        val useMaxTier = if (currentTier >= maxTier) false else config.showMaxTier.get()

        var nextTier = if (useMaxTier) maxTier else currentTier + 1
        val useCustomGoal = customGoal > 0 && customGoal > nextTier
        nextTier = if (useCustomGoal) customGoal else nextTier

        return MilestoneInfo(
            counter,
            useMaxTier,
            overflowDisplay,
            currentTier,
            nextTier,
            useCustomGoal
        )
    }

    private fun haveNeedRenderable(
        crop: CropType,
        counter: Long,
        have: Long,
        need: Long,
        overflowDisplay: Boolean
    ) = if (crop.isMaxMilestone() && !overflowDisplay) {
        val haveFormat = counter.addSeparators()
        Renderable.text("§7Counter: §e$haveFormat")
    } else {
        val haveFormat = have.addSeparators()
        val needFormat = need.addSeparators()
        Renderable.text("§e$haveFormat§8/§e$needFormat")
    }

    private fun percentRenderable(crop: CropType, overflowDisplay: Boolean): Renderable {
        val percentageFormat = crop.percentToNextMilestone()?.formatPercentage() ?: return Renderable.empty()
        return if (crop.isMaxMilestone() && !overflowDisplay) {
            Renderable.text("§7Percentage: §e100%")
        } else {
            Renderable.text("§7Percentage: §e$percentageFormat")
        }
    }

    private fun tiersRenderable(crop: CropType, currentTier: Int, nextTier: Int, showOverflow: Boolean): Renderable {
        return Renderable.horizontal {
            addItemStack(crop.icon)
            if (crop.isMaxMilestone() && !showOverflow) {
                addString("§7" + crop.cropName + " §eMAXED")
            } else {
                addString("§7" + crop.cropName + " §8$currentTier➜§3$nextTier")
            }
        }
    }

    private fun getHaveNeed(crop: CropType, counter: Long, useCustomGoal: Boolean, showMaxTier: Boolean): Pair<Long, Long>? {
        val have = if (useCustomGoal || showMaxTier) counter else crop.milestoneProgressToNextTier() ?: return null
        val need = when {
            useCustomGoal -> crop.getCustomGoal()?.cropAmount ?: 0
            showMaxTier -> crop.getMaxedMilestoneAmount()
            else -> crop.milestoneNextTierAmount() ?: return null
        }
        return Pair(have, need)
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
        if (inaccurateMilestone || countdownTitleContext != null) return

        countdownTitleContext = TitleManager.sendTitle(
            title,
            duration = timeLeft,
            addType = TitleManager.TitleAddType.FORCE_FIRST,
            countDownDisplayType = TitleManager.CountdownTitleDisplayType.WHOLE_SECONDS,
            onInterval = SoundUtils::playBeepSound,
        )
    }

    private fun formatDisplay(lineMap: MutableMap<MilestoneTextEntry, Renderable>): List<Renderable> {
        val newList = mutableListOf<Renderable>()
        if (inventoryOpen) newList.buildCropSwitcher() else newList.addVerticalSpacer()
        if (CropMilestonesApi.missingMilestoneRepoData) {
            newList.add(Renderable.text("§cMissing Milestone Repo Data!"))
            val inaccurateList = listOf(
                MilestoneTextEntry.MILESTONE_TIER,
                MilestoneTextEntry.PERCENTAGE,
                MilestoneTextEntry.NUMBER_OUT_OF_TOTAL,
                MilestoneTextEntry.TIME
            )

            for (key in inaccurateList) {
                lineMap.remove(key)
            }
        }

        newList.addAll(config.text.mapNotNull { lineMap[it] })

        if (inaccurateMilestone) {
            newList.addString("§cOpen §e/cropmilestones §cto update!")
        }
        return newList
    }

    private fun getDisplayCrop() = displayCrop

    private fun MutableList<Renderable>.buildCropSwitcher() {
        this.addRenderableNullableButton(
            label = "Crop Type",
            current = getDisplayCrop(),
            nullLabel = "Default",
            onChange = { new ->
                displayCrop = new
                update()
            },
            universe = CropType.entries,
            enableUniverseScroll = false // would infinitely scroll while hovered
        )
    }

    @Suppress("DestructuringDeclarationWithTooManyEntries")
    private fun addMushroomCowData() {
        val mushroom = CropType.MUSHROOM
        val lineMap = HashMap<MushroomTextEntry, Renderable>()

        val (
            counter,
            useMaxTier,
            overflowDisplay,
            currentTier,
            nextTier,
            useCustomGoal
        ) = getMilestoneInfo(mushroom) ?: run {
            mushroomCowPerkDisplay = listOf(Renderable.text("§cOpen §e/cropmilestones §cto update!"))
            return
        }
        val (have, need) = getHaveNeed(mushroom, counter, useCustomGoal, useMaxTier) ?: run {
            mushroomCowPerkDisplay = listOf(Renderable.text("§cOpen §e/cropmilestones §cto update!"))
            return
        }

        lineMap[MushroomTextEntry.TITLE] = Renderable.text("§6Mooshroom Cow Perk")
        lineMap[MushroomTextEntry.MUSHROOM_TIER] = tiersRenderable(mushroom, currentTier, nextTier, overflowDisplay)
        lineMap[MushroomTextEntry.NUMBER_OUT_OF_TOTAL] = haveNeedRenderable(mushroom, counter, have, need, overflowDisplay)
        lineMap[MushroomTextEntry.PERCENTAGE] = percentRenderable(mushroom, overflowDisplay)

        val missing = need - have

        val speed = GardenCropSpeed.averageBlocksPerSecond

        if (speed != 0.0 && !(mushroom.isMaxMilestone() && !overflowDisplay)) {
            val blocksPerSecond = speed * (getCurrentlyFarmedCrop()?.multiplier ?: 1)

            val missingTime = (missing / blocksPerSecond).seconds
            val biggestUnit = config.highestTimeFormat.get().timeUnit
            val duration = missingTime.format(biggestUnit)
            lineMap[MushroomTextEntry.TIME] = Renderable.text("§7In §b$duration")
        }

        mushroomCowPerkDisplay = config.mushroomPetPerk.text.mapNotNull { lineMap[it] }
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

    data class MilestoneInfo(
        val counter: Long,
        val useMaxTier: Boolean,
        val overflowDisplay: Boolean,
        val currentTier: Int,
        val nextTier: Int,
        val useCustomGoal: Boolean
    )
}
