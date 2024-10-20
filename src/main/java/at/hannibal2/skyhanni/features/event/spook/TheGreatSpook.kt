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

    private val speakingFearMessagePattern by RepoPattern.pattern(
        "chat.speaking",
        "§4\\[FEAR] Public Speaking Demon§r§f: (Speak|Say something interesting) (?<name>.*)!",
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (isMathSolverEnabled()) {
            mathFearMessagePattern.matchMatcher(event.message) {
                val math = group("math")
                val result = NEUCalculator.calculateOrNull(math)
                DelayedRun.runNextTick {
                    val feature: KMutableProperty0<*> = config.primalFearSolver::math
                    if (result != null) {
                        ChatUtils.clickToActionOrDisable(
                            "The result is: $result",
                            feature,
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

        if (isSpeakingSolverEnabled()) {
            speakingFearMessagePattern.matchMatcher(event.message) {
                DelayedRun.runNextTick {
                    val feature: KMutableProperty0<*> = config.primalFearSolver::publicSpeaking
                    val stringToSend = "I looove SkyHanni! " + StringUtils.generateRandomString(4)
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
    }

    private fun isTimerEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearTimer

    private fun isNotificationEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearNotification
    private fun isFearStatEnabled(): Boolean = LorenzUtils.inSkyBlock && config.fearStatDisplay
    private fun isTimeLeftEnabled(): Boolean = LorenzUtils.inSkyBlock && config.greatSpookTimeLeft

    private fun isMathSolverEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearSolver.math
    private fun isSpeakingSolverEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearSolver.publicSpeaking
}
