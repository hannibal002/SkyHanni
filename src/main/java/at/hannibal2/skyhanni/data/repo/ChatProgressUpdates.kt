package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.hover
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * This class allows to log actions and their duration of long, async tasks in chat.
 * Ideally for repo reload.
 */
class ChatProgressUpdates {
    private var startOfFirst: SimpleTimeMark? = null
    private var title: String? = null

    private var currentlyRunning = false
    private var chatId: Int? = null

    private val previousSteps = mutableListOf<String>()

    private var startOfCurrent: SimpleTimeMark? = null
    private var currentStep: String? = null
    private var innerProgress = ""

    private var delayedSending: DelayedSending? = null

    private var innerProgressMax = 0
    private val innerProgressCount = AtomicInteger(0)

    class DelayedSending(val text: String, val hover: String) {
        fun send(chatId: Int) {
            val hover = hover.asComponent()
            val nextSend = TextHelper.text(text) {
                this.hover = hover
            }
            nextSend.send(chatId)
        }
    }

    init {
        updates.add(this)
    }

    @SkyHanniModule
    companion object {

        private val updates = mutableListOf<ChatProgressUpdates>()

        @HandleEvent(onlyOnSkyblock = true)
        fun onTick(event: SkyHanniTickEvent) {
            if (!SkyBlockUtils.debug) return
            if (event.isMod(2)) {
                for (update in updates) {
                    update.testDelayedSending()
                    if (update.currentlyRunning) {
                        update.update()
                    }
                }
            }
        }
    }

    fun innerProgressStart(max: Int) {
        if (max > 0) {
            innerProgress(0, max)
        } else {
            update("inner progress with max=$max!")
        }
        innerProgressMax = max
        innerProgressCount.set(0)
    }

    fun innerProgressStep() {
        innerProgress(innerProgressCount.incrementAndGet(), innerProgressMax)
    }

    private fun innerProgress(min: Int, max: Int) {
        val percentage = ((min.toDouble() / max.toDouble()) * 100).roundTo(2)
        this.innerProgress = "($percentage% ${min.addSeparators()}/${max.addSeparators()}) "
    }

    fun start(nextStep: String) {
        statusUpdate(nextStep, Phase.START)
    }

    fun update(nextStep: String) {
        statusUpdate(nextStep, Phase.UPDATE)
    }

    fun end(nextStep: String) {
        statusUpdate(nextStep, Phase.END)
    }

    private fun statusUpdate(nextStep: String, phase: Phase) {
        if (phase == Phase.START) {
            if (currentlyRunning) {
                ErrorManager.skyHanniError(
                    "trying to start an already running chat",
                    "next step" to nextStep,
                    "last step" to currentStep?.lastOrNull(),
                )
            }
            currentlyRunning = true
            startOfFirst = SimpleTimeMark.now()
            chatId = ChatUtils.getUniqueMessageId()
            title = nextStep
        }
        if (phase == Phase.UPDATE) {
            if (!currentlyRunning) {
                ErrorManager.skyHanniError(
                    "trying to update an not running chat",
                    "next step" to nextStep,
                )
            }
        }

        currentStep?.let {
            val format = startOfCurrent?.format() ?: error("start of current is null")
            previousSteps.add("§8- §f$it $innerProgress$format")
        }
        innerProgress = ""

        val time = SimpleTimeMark.now().toLocalDateTime()
        currentStep = nextStep
        startOfCurrent = SimpleTimeMark.now()
        println("$time: $nextStep")

        if (phase == Phase.END) {
            if (!currentlyRunning) {
                ErrorManager.skyHanniError(
                    "trying to end an not running chat",
                    "next step" to nextStep,
                    "last step" to currentStep?.lastOrNull(),
                )
            }
            currentlyRunning = false
            update()
            currentStep = null
            startOfCurrent = null
            previousSteps.clear()
        } else {
            update()
        }
    }

    private fun SimpleTimeMark.format(): String {
        val duration = passedSince()
        val color = when {
            duration < 100.milliseconds -> "§7"
            duration < 5.seconds -> "§b"
            duration < 1.minutes -> "§c"
            else -> "§4"
        }

        val format = duration.format(showMilliSeconds = true)
        return "$color$format§f"
    }

    private fun update() {
        val title = title ?: error("currentStep is null")
        val currentStep = currentStep ?: error("currentStep is null")
        val chatId = chatId ?: error("chatId is null: $currentStep")
        val totalTime = startOfFirst?.format() ?: error("startOfFirst is null: $currentStep")

        val hover = mutableListOf<String>()
        hover.add("§e$title")
        hover.add("§8SkyHanni Debug Log")
        hover.add("")
        hover.addAll(previousSteps)
        val currentTime = startOfCurrent?.format() ?: error("startOfCurrent is null")
        val currentLine = "§8- §f$currentStep $innerProgress$currentTime"
        hover.add(currentLine)
        hover.add("")

        val text = if (currentlyRunning) {
            hover.add("§7Running for: $totalTime")
            currentLine
        } else {
            hover.add("§aDone after: $totalTime")
            "$currentStep $totalTime"
        }

        val delayedSending = DelayedSending("§e[Debug-Log] §f$text §7(hover for more info)", hover.joinToString("\n"))
        if (SkyBlockUtils.debug) {
            delayedSending.send(chatId)
        } else {
            this.delayedSending = delayedSending
        }
    }

    private fun testDelayedSending() {
        val chatId = chatId ?: error("chatId is null: $currentStep")
        delayedSending?.let {
            if (MinecraftCompat.localPlayerOrNull != null) {
                it.send(chatId)
                delayedSending = null
            }
        }
    }

    private enum class Phase {
        START,
        UPDATE,
        END,
    }
}
