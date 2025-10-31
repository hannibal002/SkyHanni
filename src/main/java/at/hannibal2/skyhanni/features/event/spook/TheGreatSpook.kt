package at.hannibal2.hanni.features.event.spook

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.jsonobjects.repo.EventsJson
import at.hannibal2.hanni.data.model.SkyblockStat
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ConditionalUtils.afterChange
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.NeuCalculator
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.TimeUnit
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.collection.CircularList
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.primitives.text
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HanniModule
object TheGreatSpook {
    private val config get() = HanniMod.feature.event.spook

    private var isGreatSpookActive = false
    private var greatSpookTimeRange: ClosedRange<SimpleTimeMark>? = null
    private var greatSpookEndTime = SimpleTimeMark.farPast()

    private var displayMobCooldown: Renderable? = null
    private var displayGreatSpookEnd: Renderable? = null

    private var timeUntilNextMob = SimpleTimeMark.farPast()

    private val publicSpeakingSolutions = CircularList(
        "I looove Hanni!",
        "Do you know Hanni? A cool mod for SkyBlock!",
        "Today is a good day to kill Spooky monsters.",
    )

    private val patternGroup = RepoPattern.group("event.greatspook")

    /**
     * REGEX-TEST: §d§lQUICK MATHS! §r§7Solve: §r§e(10*2)+12*5
     */
    private val mathFearMessagePattern by patternGroup.pattern(
        "chat.math",
        "§d§lQUICK MATHS! §r§7Solve: §r§e(?<math>.*)",
    )

    /**
     * REGEX-TEST: §4[FEAR] Public Speaking Demon§r§f: Speak PlasticEating!
     */
    private val speakingFearMessagePattern by patternGroup.pattern(
        "chat.speaking",
        "§4\\[FEAR] Public Speaking Demon§r§f: (?:Speak|Say something interesting) (?<name>.*)!",
    )

    /**
     * REGEX-TEST: §5§lFEAR. §r§eA §r§dPrimal Fear §r§ehas been summoned!
     */
    private val primalFearSpawnPattern by patternGroup.pattern(
        "mob.spawn",
        "§5§lFEAR\\. §r§eA §r§dPrimal Fear §r§ehas been summoned!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isGreatSpookActive) return

        val fear = SkyblockStat.FEAR.lastKnownValue ?: 0.0
        val mobCooldown = timeUntilNextMob.minus((3 * fear).seconds)
        val mobCooldownString = if (mobCooldown.isInFuture()) {
            "§5Next fear in: §b${
                mobCooldown.timeUntil().format(
                    biggestUnit = TimeUnit.MINUTE,
                    showMilliSeconds = false,
                    showSmallerUnits = false,
                )
            }"
        } else {
            "§5§lPrimal Fear Ready!"
        }
        displayMobCooldown = Renderable.text(mobCooldownString)

        if (config.primalFearNotification && mobCooldown.isInPast()) {
            SoundUtils.playPlingSound()
        }

        val greatSpookEnd = greatSpookTimeRange?.endInclusive ?: return
        val timeLeftString = if (greatSpookEnd.isInFuture()) {
            "§5Great Spook time left: §b${
                greatSpookEnd.timeUntil().format(
                    biggestUnit = TimeUnit.DAY,
                    maxUnits = 2,
                )
            }"
        } else {
            "§5§lThe Great Spook has ended!"
        }
        displayGreatSpookEnd = Renderable.text(timeLeftString)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val config = HanniMod.feature.dev.debug.forceGreatSpook
        config.afterChange {
            if (config.get()) {
                isGreatSpookActive = true
                greatSpookEndTime = SimpleTimeMark.farFuture()
            } else {
                val timeRange = greatSpookTimeRange
                if (timeRange == null) {
                    isGreatSpookActive = false
                    greatSpookEndTime = SimpleTimeMark.farPast()
                    return@afterChange
                }
                isGreatSpookActive = SimpleTimeMark.now() in timeRange
                greatSpookEndTime = timeRange.endInclusive
            }
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        val currentTime = SimpleTimeMark.now()
        val timeRange = greatSpookTimeRange ?: run {
            isGreatSpookActive = false
            return
        }

        isGreatSpookActive = currentTime in timeRange
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isGreatSpookActive) return

        if (config.primalFearTimer) {
            displayMobCooldown.let {
                config.positionTimer.renderRenderable(it, posLabel = "Primal Fear Timer")
            }
        }
        if (config.fearStatDisplay) {
            SkyblockStat.FEAR.displayValue?.let {
                config.positionFear.renderString(it, posLabel = "Fear Stat Display")
            }
        }
        if (config.greatSpookTimeLeft) {
            displayGreatSpookEnd.let {
                config.positionTimeLeft.renderRenderable(it, posLabel = "Great Spook Time Left")
            }
        }
    }

    private fun mathSolver(query: String?) {
        val answer = query?.let { NeuCalculator.calculateOrNull(it)?.toInt() } ?: run {
            ChatUtils.userError("Failed to solve $query!")
            return
        }
        ChatUtils.clickToActionOrDisable(
            "The answer is: §b$answer§e.",
            config.primalFearSolver::math,
            actionName = "send the answer",
            action = {
                HypixelCommands.allChat(answer.toString())
            },
        )
    }

    private fun publicSpeakingSolver() {
        val solution = publicSpeakingSolutions.next()
        ChatUtils.clickToActionOrDisable(
            "Solving Public Speaking puzzle for you.",
            config.primalFearSolver::publicSpeaking,
            actionName = "send a random string",
            action = {
                HypixelCommands.allChat(solution)
            },
            oneTimeClick = true,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        if (!isGreatSpookActive) return

        if (primalFearSpawnPattern.matches(event.message)) {
            timeUntilNextMob = SimpleTimeMark.now().plus(6.minutes)
            if (SkyblockStat.FEAR.lastKnownValue == null && (config.primalFearNotification || config.primalFearTimer)) {
                ChatUtils.userError(
                    "Fear stat not found! Please enable the Stats widget and enable the Fear stat for the best results.",
                    replaceSameMessage = true,
                )
            }
            return
        }

        if (config.primalFearSolver.math) {
            mathFearMessagePattern.matchMatcher(event.message) {
                DelayedRun.runNextTick {
                    mathSolver(group("math"))
                }
            }
        }

        if (config.primalFearSolver.publicSpeaking) {
            speakingFearMessagePattern.matchMatcher(event.message) {
                DelayedRun.runNextTick {
                    publicSpeakingSolver()
                }
            }
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EventsJson>("Events").greatSpook

        val startTime = data["start_time"] ?: SimpleTimeMark.farPast()
        val endTime = data["end_time"] ?: SimpleTimeMark.farPast()

        greatSpookTimeRange = startTime..endTime
        greatSpookEndTime = if (HanniMod.feature.dev.debug.forceGreatSpook.get()) SimpleTimeMark.farFuture() else endTime
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Great Spook")

        event.addIrrelevant {
            add("isActive: $isGreatSpookActive")
            add("activeTimeRange: $greatSpookTimeRange")
            add("eventEndTime: $greatSpookEndTime")
            add("timeUntilNextMob: $timeUntilNextMob")
        }
    }
}
