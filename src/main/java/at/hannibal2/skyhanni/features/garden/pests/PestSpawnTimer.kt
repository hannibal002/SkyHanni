package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestTimerConfig.PestTimerTextEntry
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager.sendTitle
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.hasFarmingToolInHand
import at.hannibal2.skyhanni.features.garden.GardenApi.lastCropBrokenTime
import at.hannibal2.skyhanni.features.garden.GardenApi.pestCooldownEndTime
import at.hannibal2.skyhanni.features.garden.pests.PestApi.hasVacuumInHand
import at.hannibal2.skyhanni.features.garden.pests.PestApi.lastPestSpawnTime
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.hasGroup
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.average
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestSpawnTimer {

    private val config get() = PestApi.config.pestTimer
    private val patternGroup = RepoPattern.group("garden.pests")

    /**
     * REGEX-TEST:  Cooldown: §r§a§lREADY
     * REGEX-TEST:  Cooldown: §r§e1m 58s
     * REGEX-TEST:  Cooldown: §r§e1m
     * REGEX-TEST:  Cooldown: §r§e58s
     * REGEX-TEST:  Cooldown: §r§c§lMAX PESTS
     */

    private val pestCooldownPattern by patternGroup.pattern(
        "cooldown",
        "\\sCooldown: §r§.(?:§.)?(?:(?<minutes>\\d+)m)? ?(?:(?<seconds>\\d+)s)?(?<ready>READY)?(?<maxPests>MAX PESTS)?.*",
    )

    private val pestSpawnTimes: MutableList<Duration> = mutableListOf()
    private val averageSpawnTime: Duration get() = pestSpawnTimes.average()
    private var longestCropBrokenTime: Duration = 0.seconds
    private var pestSpawned = false
    private var hasWarned = false
    private var maxPests = false
    private var ready = false
    private var shouldRender = false
    private var display: List<Renderable> = emptyList()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PESTS)) return

        pestCooldownPattern.firstMatcher(event.widget.lines) {
            val minutes = groupOrNull("minutes")?.formatInt()
            val seconds = groupOrNull("seconds")?.formatInt()
            ready = hasGroup("ready")
            maxPests = hasGroup("maxPests")

            if (ready || maxPests) return
            if (minutes == null && seconds == null) return

            val tablistCooldownEnd = SimpleTimeMark.now() + (minutes?.minutes ?: 0.seconds) + (seconds?.seconds ?: 0.seconds)

            if (shouldSetCooldown(tablistCooldownEnd, seconds)) {
                // hypixel sometimes rounds down times, we'll assume times are rounded down if seconds are null and add a minute
                pestCooldownEndTime = if (seconds == null) {
                    tablistCooldownEnd + 1.minutes
                } else {
                    tablistCooldownEnd
                }
                if (pestSpawned) {
                    hasWarned = false
                    pestSpawned = false
                }
            }
        }
    }

    @HandleEvent
    fun onPestSpawn(event: PestSpawnEvent) {
        val spawnTime = lastPestSpawnTime.passedSince()

        if (!lastPestSpawnTime.isFarPast()) {
            if (longestCropBrokenTime <= config.averagePestSpawnTimeout.seconds) {
                pestSpawnTimes.add(spawnTime)
                ChatUtils.debug("Added pest spawn time ${spawnTime.format()}")
            }
            if (config.pestSpawnChatMessage) {
                ChatUtils.chat("Pests spawned in §b${spawnTime.format()}")
            }
        }

        pestSpawned = true
        longestCropBrokenTime = 0.seconds
        lastPestSpawnTime = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
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

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        update()
        if (hasWarned || !config.cooldownOverWarning) return

        if (pestCooldownEndTime.isInPast()) {
            cooldownExpired()
            return
        }
        if ((pestCooldownEndTime - ((config.cooldownWarningTime.seconds) + 1.seconds)).isInPast()) {
            cooldownReminder()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        shouldRender = shouldRender()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onIslandChange(event: IslandChangeEvent) {
        longestCropBrokenTime = lastCropBrokenTime.passedSince()
    }

    private fun shouldSetCooldown(tabCooldownEnd: SimpleTimeMark, seconds: Int?): Boolean {
        // tablist can have up to 6 seconds of delay, besides this, there is no scenario where tablist will overestimate cooldown
        if (tabCooldownEnd > ((pestCooldownEndTime) + 6.seconds)) return true
        // tablist sometimes rounds down to nearest min
        if ((tabCooldownEnd + 1.minutes) < (pestCooldownEndTime) && seconds == null) return true
        // tablist shouldn't underestimate if it is displaying seconds
        if ((tabCooldownEnd + 1.seconds) < (pestCooldownEndTime) && seconds != null) return true
        return false
    }

    private fun drawDisplay(): List<Renderable> {
        val lineMap = mutableMapOf<PestTimerTextEntry, Renderable>()

        val lastPestSpawned = if (lastPestSpawnTime.isFarPast()) {
            "§cNo pest spawned since joining."
        } else {
            val timeSinceLastPest = lastPestSpawnTime.passedSince().format()
            "§eLast pest spawned: §b$timeSinceLastPest ago"
        }

        lineMap[PestTimerTextEntry.PEST_TIMER] = Renderable.string(lastPestSpawned)

        val pestCooldown = if (!TabWidget.PESTS.isActive) {
            "§cPests Widget not detected! Enable via /widget!"
        } else {
            val cooldownValue = when {
                maxPests -> "§cMax Pests!"
                ready || pestCooldownEndTime.isInPast() -> "§aReady!"
                pestCooldownEndTime.isFarPast() -> "§cUnknown"
                else -> pestCooldownEndTime.timeUntil().format()
            }
            "§ePest Cooldown: §b$cooldownValue"
        }

        lineMap[PestTimerTextEntry.PEST_COOLDOWN] = Renderable.string(pestCooldown)

        val averageSpawn = averageSpawnTime.format()
        if (averageSpawnTime != 0.seconds) {
            lineMap[PestTimerTextEntry.AVERAGE_PEST_SPAWN] = Renderable.string("§eAverage time to spawn: §b$averageSpawn")
        }

        return formatDisplay(lineMap)
    }

    private fun formatDisplay(lineMap: Map<PestTimerTextEntry, Renderable>): List<Renderable> {
        return config.pestDisplay.mapNotNull { lineMap[it] }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun shouldRender(): Boolean = when {
        config.onlyWithFarmingTool && config.onlyWithVacuum -> hasFarmingToolInHand() || hasVacuumInHand()
        config.onlyWithFarmingTool -> hasFarmingToolInHand()
        config.onlyWithVacuum -> hasVacuumInHand()

        else -> true
    }

    private fun cooldownExpired() {
        sendTitle("§cPest Cooldown Has Expired!", duration = 3.seconds)
        ChatUtils.chat("§cPest spawn cooldown has expired!")
        SoundUtils.playPlingSound()
        hasWarned = true
    }

    private fun cooldownReminder() {
        sendTitle("§cPest Cooldown Expires Soon!", duration = 3.seconds)
        ChatUtils.chat("§cPest spawn cooldown expires in ${pestCooldownEndTime.timeUntil().format()}")
        SoundUtils.playPlingSound()
        hasWarned = true
    }

    private fun isEnabled() = GardenApi.inGarden() && config.enabled
}
