package at.hannibal2.skyhanni.features.event.spook

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUCalculator
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.reflect.KMutableProperty0

@SkyHanniModule
object TheGreatSpook {

    // §r§cPrimal Fears§r§7: §r§6§lREADY!!
    private val config get() = SkyHanniMod.feature.event.spook
    private var displayTimer = ""
    private var displayTimeLeft = ""
    private var notificationSeconds = 0

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (isTimerEnabled() || isNotificationEnabled()) displayTimer = checkTabList(" §r§cPrimal Fears§r§7: ")
        if (isTimeLeftEnabled()) displayTimeLeft = checkTabList(" §r§dEnds In§r§7: ")
        if (isNotificationEnabled()) {
            if (displayTimer.endsWith("READY!!")) {
                if (notificationSeconds > 0) {
                    SoundUtils.playBeepSound()
                    notificationSeconds--
                }
            } else if (displayTimer.isNotEmpty()) {
                notificationSeconds = 5
            }
        }
    }

    private fun checkTabList(matchString: String): String {
        return (TabListData.getTabList().find { it.contains(matchString) }.orEmpty()).trim()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (isTimerEnabled()) config.positionTimer.renderString(displayTimer, posLabel = "Primal Fear Timer")
        if (isFearStatEnabled()) {
            SkyblockStat.FEAR.displayValue?.let {
                config.positionFear.renderString(it, posLabel = "Fear Stat Display")
            }
        }
        if (isTimeLeftEnabled()) config.positionTimeLeft.renderString(displayTimeLeft, posLabel = "Time Left Display")
    }


    /**
     * REGEX-TEST: §d§lQUICK MATHS! §r§7Solve: §r§e(10*2)+12*5
     */
    private val mathFearMessagePattern by RepoPattern.pattern(
        "chat.math",
        "§d§lQUICK MATHS! §r§7Solve: §r§e(?<math>.*)",
    )

    private val mathFearCompletionPattern by RepoPattern.pattern(
        "chat.math.completion",
        "§d§lQUICK MATHS! §r§e(?<name>.*) §r§7answered correctly in §r§a(?<time>\\d+)s§r§e!",
    )

    // §d§lQUICK MATHS! §r§ePlasticEating §r§7answered correctly in §r§a2s§r§e!

    private val speakingFearMessagePattern by RepoPattern.pattern(
        "chat.speaking",
        "§4\\[FEAR] Public Speaking Demon§r§f: (Speak|Say something interesting) (?<name>.*)!",
    )

    private val speakingFearCompletionPattern by RepoPattern.pattern(
        "chat.speaking.completion",
        "§4\\[FEAR] Public Speaking Demon§r§f: Woohoo! Thank you for speaking out loud!",
    )

    // §4[FEAR] Public Speaking Demon§r§f: Woohoo! Thank you for speaking out loud!

    private val fearTimeOutPattern by RepoPattern.pattern(
        "chat.fear.timeout",
        "§5§lFEAR. §r§eYour fear overcame you and ran away!",
    )

    // §5§lFEAR. §r§eYour fear overcame you and ran away!

    var fearComplete: Boolean = false
    var stringToSend: String = ""

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (isMathSolverEnabled()) {
            mathFearMessagePattern.matchMatcher(event.message) {
                fearComplete = false
                val math = group("math")
                val result = NEUCalculator.calculateOrNull(math)?.toInt()
                stringToSend = result.toString()
                DelayedRun.runNextTick {
                    if (result != null) {
                        ChatUtils.clickToActionOrDisable(
                            "The result is: $result",
                            option = config.primalFearSolver::math,
                            actionName = "Send the result",
                            action =
                            {
                                HypixelCommands.allChat(result.toString())
                            },
                        )
                    } else {
                        ChatUtils.chat("An unexpected error occurred while solving the math problem.")
                    }
                }
            }
        }

        if (isMathSolverEnabled()) {
            mathFearCompletionPattern.matchMatcher(event.message) {
                fearComplete = true
                stringToSend = ""
            }
        }

        if (isSpeakingSolverEnabled()) {
            speakingFearMessagePattern.matchMatcher(event.message) {
                fearComplete = false
                DelayedRun.runNextTick {
                    val feature: KMutableProperty0<*> = config.primalFearSolver::publicSpeaking
                    stringToSend = "I looove SkyHanni! " + StringUtils.generateRandomString(4)
                    ChatUtils.clickToActionOrDisable(
                        "Click to send a random string to complete the Primal Fear",
                        feature,
                        "Sends $stringToSend",
                        action =
                        {
                            HypixelCommands.allChat(stringToSend)
                        }
                    )
                }
            }
        }

        if (isSpeakingSolverEnabled()) {
            speakingFearCompletionPattern.matchMatcher(event.message) {
                fearComplete = true
                stringToSend = ""
            }
        }

        fearTimeOutPattern.matchMatcher(event.message) {
            fearComplete = true
            stringToSend = ""
        }
    }

    @SubscribeEvent
    fun onKey(event: LorenzKeyPressEvent) {
        if (!isAnySolverEnabled()) return
        if (event.keyCode != config.primalFearSolver.keyBindSolve) return

        if (!fearComplete) {
            HypixelCommands.allChat(stringToSend)
            fearComplete = true
        }
    }

    private fun isTimerEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearTimer

    private fun isNotificationEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearNotification
    private fun isFearStatEnabled(): Boolean = LorenzUtils.inSkyBlock && config.fearStatDisplay
    private fun isTimeLeftEnabled(): Boolean = LorenzUtils.inSkyBlock && config.greatSpookTimeLeft

    private fun isMathSolverEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearSolver.math
    private fun isSpeakingSolverEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearSolver.publicSpeaking

    private fun isAnySolverEnabled(): Boolean = isMathSolverEnabled() || isSpeakingSolverEnabled()
}
