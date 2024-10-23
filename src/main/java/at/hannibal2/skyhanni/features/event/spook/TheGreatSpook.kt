package at.hannibal2.skyhanni.features.event.spook

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUCalculator
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TheGreatSpook {
    private val config get() = SkyHanniMod.feature.event.spook

    private var isGreatSpookActive = false

    private var displayMobCooldown: Renderable? = null
    private var displayGreatSpookEnd: Renderable? = null

    private var timeUntilNextMob = SimpleTimeMark.farPast()
    private var greatSpookEnd = SimpleTimeMark(1730523600000)

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
        "§4\\[FEAR] Public Speaking Demon§r§f: (Speak|Say something interesting) (?<name>.*)!",
    )
    /**
     * REGEX-TEST: §5§lFEAR. §r§eA §r§dPrimal Fear §r§ehas been summoned!
     */
    private val primalFearSpawnPattern by patternGroup.pattern(
        "mob.spawn",
        "§5§lFEAR\\. §r§eA §r§dPrimal Fear §r§ehas been summoned!",
    )

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        val fear = SkyblockStat.FEAR.lastKnownValue ?: run {
            if (config.primalFearNotification || config.primalFearTimer)
                ChatUtils.userError("Fear stat not found! Please enable the Stats widget and enable the Fear stat for the best results.")
            0.0
        }
        val mobCooldown = timeUntilNextMob.minus((3*fear).seconds)
        val mobCooldownString = if (mobCooldown.isInFuture()) {
            "§5Next fear in: ${mobCooldown.timeUntil().format(
                biggestUnit = TimeUnit.MINUTE,
                showMilliSeconds = false,
                showSmallerUnits = false
            )
            }"
        } else {
            "§5§lPrimal Fear Ready!"
        }
        displayMobCooldown = Renderable.string(mobCooldownString)

        val timeLeftString = if (greatSpookEnd.isInFuture()) {
            "§5Time left: ${greatSpookEnd.timeUntil().format(
                biggestUnit = TimeUnit.DAY,
                showMilliSeconds = false,
                showSmallerUnits = false
            )
            }"
        } else {
            "§5§lThe Great Spook has ended!"
        }
        displayGreatSpookEnd = Renderable.string(timeLeftString)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return

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
                config.positionTimeLeft.renderRenderable(it, posLabel = "Time Left Display")
            }
        }
    }

    private fun mathSolver(query: String?) {
        val answer = query?.let { NEUCalculator.calculateOrNull(it)?.toInt() } ?: run {
            ChatUtils.userError("Failed to solve $query!")
            return
        }
        ChatUtils.clickToActionOrDisable(
            "The answer is: $answer",
            config.primalFearSolver::math,
            actionName = "Send the answer",
            action = {
                HypixelCommands.allChat(answer.toString())
            },
        )
    }

    private fun publicSpeakingSolver() {
        ChatUtils.clickToActionOrDisable(
            "Click to complete the Primal Fear",
            config.primalFearSolver::publicSpeaking,
            actionName = "send a random string.",
            action = {
                HypixelCommands.allChat("I looove SkyHanni! ${StringUtils.generateRandomString(4)}")
            },
        )
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (primalFearSpawnPattern.matches(event.message)) {
            timeUntilNextMob = SimpleTimeMark.now().plus(6.minutes)
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
}
