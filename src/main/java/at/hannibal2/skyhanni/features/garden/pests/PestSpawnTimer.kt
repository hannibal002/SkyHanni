package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.pests.PestTimerConfig.HeldItem
import at.hannibal2.skyhanni.config.features.garden.pests.PestTimerConfig.PestTimerTextEntry
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.hasFarmingToolInHand
import at.hannibal2.skyhanni.features.garden.GardenApi.lastCropBrokenTime
import at.hannibal2.skyhanni.features.garden.GardenApi.pestCooldownEndTime
import at.hannibal2.skyhanni.features.garden.pests.PestApi.hasLassoInHand
import at.hannibal2.skyhanni.features.garden.pests.PestApi.hasVacuumInHand
import at.hannibal2.skyhanni.features.garden.pests.PestApi.lastPestSpawnTime
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.hasGroup
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.TimeUtils.average
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.getTablistEndTime
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestSpawnTimer {

    private val config get() = PestApi.config.pestTimer
    private val patternGroup = RepoPattern.group("garden.pests")
    private val cooldownOverMessageId = ChatUtils.getUniqueMessageId()

    /**
     * WRAPPED-REGEX-TEST: " Cooldown: READY"
     * WRAPPED-REGEX-TEST: " Cooldown: 1m 58s"
     * WRAPPED-REGEX-TEST: " Cooldown: 1m"
     * WRAPPED-REGEX-TEST: " Cooldown: 58s"
     * WRAPPED-REGEX-TEST: " Cooldown: MAX PESTS"
     */

    private val pestCooldownPattern by patternGroup.pattern(
        "cooldowntime-no-color",
        "\\sCooldown: (?<time>\\d{1,2}[ms](?: \\d{1,2}s?)?)?(?<ready>READY)?(?<maxPests>MAX PESTS)?.*",
    )

    private val pestSpawnTimes: MutableList<Duration> = mutableListOf()
    private val averageSpawnTime: Duration get() = pestSpawnTimes.average()
    private var longestCropBrokenTime: Duration = 0.seconds
    private var pestSpawned = false
    private var hasWarned = false
    private var hasReminderShown = false
    private var maxPests = false
    private var ready = false
    private var shouldRender = false
    private var display: List<Renderable> = emptyList()
    private var shouldRepeatWarning = false
    private var countdownTitleContext: TitleContext? = null
    private var lastPlayedSound: SimpleTimeMark = SimpleTimeMark.farPast()

    private fun getCustomCooldownTime(): Duration = with(config) {
        if (Perk.PEST_ERADICATOR.isActive) customCooldownTimeFinnegan
        else customCooldownTime
    }.get().seconds

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PESTS)) return

        pestCooldownPattern.firstMatcher(event.widget.lines.map { it.string }) {
            val time = groupOrNull("time")?.let { getTablistEndTime(it, pestCooldownEndTime) }
            ready = hasGroup("ready")
            maxPests = hasGroup("maxPests")

            if (ready || maxPests) {
                pestCooldownEndTime = SimpleTimeMark.farPast()
                shouldRepeatWarning = false
                return
            }
            if (time == null) return
            pestCooldownEndTime = if (config.customCooldown.get()) {
                lastPestSpawnTime + getCustomCooldownTime()
            } else time

            if (pestSpawned) {
                hasWarned = false
                hasReminderShown = false
                pestSpawned = false
            }
        }
    }

    @HandleEvent(PestSpawnEvent::class)
    fun onPestSpawn() {
        shouldRepeatWarning = false
        val spawnTime = lastPestSpawnTime.passedSince()

        if (!lastPestSpawnTime.isFarPast()) {
            if (longestCropBrokenTime <= config.averagePestSpawnTimeout.seconds) {
                pestSpawnTimes.add(spawnTime)
                ChatUtils.debug("Added pest spawn time ${spawnTime.format()}")
            }
            if (config.pestSpawnChatMessage) ChatUtils.notifyOrDisable(
                "Pests spawned in §b${spawnTime.format()}",
                option = config::pestSpawnChatMessage,
            )
        }

        pestSpawned = true
        longestCropBrokenTime = 0.seconds
        lastPestSpawnTime = SimpleTimeMark.now()
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onGuiRenderOverlay() {
        if (!shouldRender) return
        config.position.renderRenderables(display, posLabel = "Pest Spawn Timer")
    }

    @HandleEvent
    fun onCropBreak(event: CropClickEvent) {
        if (event.clickType != ClickType.LEFT_CLICK) return
        val timeDiff = lastCropBrokenTime.passedSince()

        if (timeDiff > longestCropBrokenTime) {
            longestCropBrokenTime = timeDiff
        }

        lastCropBrokenTime = SimpleTimeMark.now()
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed() {
        if (!isEnabled()) return
        update()
        if (shouldRepeatWarning) {
            countdownTitleContext?.stop()
            countdownTitleContext = null
            if (!pestCooldownEndTime.isInPast()) {
                countdownWarn(pestCooldownEndTime.timeUntil())
            }
        }

        if (hasWarned || !config.cooldownOverWarning) return

        if (pestCooldownEndTime.isFarPast()) return
        if (pestCooldownEndTime.isInPast()) {
            cooldownExpired()
            return
        }
        if (hasReminderShown) return
        if ((pestCooldownEndTime - (config.cooldownWarningTime.seconds + 1.seconds)).isInPast()) {
            cooldownReminder(pestCooldownEndTime)
        } else shouldRepeatWarning = false
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (shouldRepeatWarning) {
            if (WardrobeApi.inWardrobe()) {
                shouldRepeatWarning = false
                countdownTitleContext?.stop()
                countdownTitleContext = null
                return
            }
            repeatSound()
        }
        if (!event.isMod(5)) return
        shouldRender = shouldRender()
    }

    @HandleEvent(IslandJoinEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onIslandJoin() {
        shouldRepeatWarning = false
        longestCropBrokenTime = lastCropBrokenTime.passedSince()
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        config.customCooldown.onToggle {
            setCustomCooldown()
        }
        config.customCooldownTime.afterChange {
            setCustomCooldown()
        }
    }

    private fun setCustomCooldown() {
        if (!config.customCooldown.get()) return
        pestCooldownEndTime = lastPestSpawnTime + getCustomCooldownTime()
    }

    private fun drawDisplay(): List<Renderable> {
        val lineMap = mutableMapOf<PestTimerTextEntry, Renderable>()

        val lastPestSpawned = if (lastPestSpawnTime.isFarPast()) {
            "§cNo pest spawned since joining."
        } else {
            val timeSinceLastPest = lastPestSpawnTime.passedSince().format()
            "§eLast pest spawned: §b$timeSinceLastPest ago"
        }

        lineMap[PestTimerTextEntry.PEST_TIMER] = Renderable.text(lastPestSpawned)

        val pestCooldown = if (!TabWidget.PESTS.isActive) {
            "§cPests Widget not detected! Enable via /widget!"
        } else {
            val cooldownValue = when {
                maxPests -> "§cMax Pests!"
                pestCooldownEndTime.isFarPast() -> "§cUnknown"
                ready || pestCooldownEndTime.isInPast() -> "§aReady!"
                else -> pestCooldownEndTime.timeUntil().format()
            }
            "§ePest Cooldown: §b$cooldownValue"
        }

        lineMap[PestTimerTextEntry.PEST_COOLDOWN] = Renderable.text(pestCooldown)

        val averageSpawn = averageSpawnTime.format()
        if (averageSpawnTime != 0.seconds) {
            lineMap[PestTimerTextEntry.AVERAGE_PEST_SPAWN] = Renderable.text("§eAverage time to spawn: §b$averageSpawn")
        }

        return formatDisplay(lineMap)
    }

    private fun formatDisplay(lineMap: Map<PestTimerTextEntry, Renderable>): List<Renderable> =
        config.pestDisplay.mapNotNull { lineMap[it] }

    private fun update() {
        display = drawDisplay()
    }

    private fun shouldRender(): Boolean {
        if (!isEnabled()) return false

        if (config.onlyWhenHolding.isEmpty()) return true

        return config.onlyWhenHolding.any {
            when (it) {
                HeldItem.FARMING_TOOL -> hasFarmingToolInHand()
                HeldItem.VACUUM -> hasVacuumInHand()
                HeldItem.LASSO -> hasLassoInHand()
            }
        }
    }

    private fun cooldownExpired() {
        shouldRepeatWarning = false
        TitleManager.sendTitle("§cPest Cooldown Has Expired!", duration = 3.seconds)
        ChatUtils.notifyOrDisable(
            "§cPest spawn cooldown has expired!",
            option = config::cooldownOverWarning,
            messageId = cooldownOverMessageId,
        )
        playUserSound()
        hasWarned = true
    }

    private fun cooldownReminder(endTime: SimpleTimeMark) {
        ChatUtils.notifyOrDisable(
            "§cPest spawn cooldown expires in ${endTime.timeUntil().format()}",
            option = config::cooldownOverWarning,
            messageId = cooldownOverMessageId,
        )
        hasReminderShown = true

        if (config.repeatWarning) {
            countdownWarn(endTime.timeUntil())
            shouldRepeatWarning = true
            return
        }

        TitleManager.sendTitle("§cPest Cooldown Expires Soon!", duration = 3.seconds)
        playUserSound()
    }

    private fun isEnabled() = GardenApi.inGarden() && config.enabled

    @JvmStatic
    fun playUserSound() {
        with(config.sound) {
            SoundUtils.createSound(name, pitch).playSound()
        }
    }

    // TODO: Change to countdown title when that works
    private fun countdownWarn(timeLeft: Duration) {
        val text = "§cPest spawn cooldown expires in ${timeLeft.format()}"
        countdownTitleContext = TitleManager.sendTitle(
            text,
            duration = 1.seconds,
            intention = PestTitleIntention.COOLDOWN_COUNTDOWN,
            addType = TitleManager.TitleAddType.FORCE_FIRST,
            // countDownDisplayType = TitleManager.CountdownTitleDisplayType.WHOLE_SECONDS,
        )
        ChatUtils.notifyOrDisable(
            text,
            option = config::cooldownOverWarning,
            messageId = cooldownOverMessageId,
        )
    }

    private fun repeatSound() {
        with(config) {
            if (!enabled || !GardenApi.inGarden()) return
            if (lastPlayedSound.passedSince() >= sound.repeatDuration.ticks) {
                lastPlayedSound = SimpleTimeMark.now()
                playUserSound()
            }
        }
    }

    private enum class PestTitleIntention {
        COOLDOWN_COUNTDOWN
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val userSelections: List<HeldItem> = buildList {
            event.transform(97, "garden.pests.pestTimer.onlyWithFarmingTool") { entry ->
                if (entry.asBoolean) add(HeldItem.FARMING_TOOL)
                entry
            }
            event.transform(97, "garden.pests.pestTimer.onlyWithVacuum") { entry ->
                if (entry.asBoolean) add(HeldItem.VACUUM)
                entry
            }
        }

        if (userSelections.isNotEmpty()) {
            event.add(97, "garden.pests.pestTimer.onlyWhenHolding") {
                ConfigManager.gson.toJsonTree(userSelections)
            }
        }
    }
}
