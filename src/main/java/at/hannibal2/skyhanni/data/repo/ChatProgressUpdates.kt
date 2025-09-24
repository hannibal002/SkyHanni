package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * This class allows to log actions and their duration of long, async tasks in chat.
 * Ideally for repo reload.
 */
class ChatProgressUpdates {
    private var startOfFirst: SimpleTimeMark? = null
    private var title: String? = null

    private val currentlyRunning get() = currentStep != null
    private var chatId: Int? = null

    private var previousSteps = mutableListOf<String>()

    private var startOfCurrent: SimpleTimeMark? = null
    private var currentStep: String? = null
    private var innerProgress = ""

    private var delayedSending: DelayedSending? = null

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
                    if (update.currentlyRunning) {
                        update.update()
                    }
                }
            }
        }
    }

    fun innerProgress(min: Int, max: Int) {
        val percentage = ((min.toDouble() / max.toDouble()) * 100).roundTo(2)
        this.innerProgress = "($percentage% ${min.addSeparators()}/${max.addSeparators()}) "
    }

    fun start(nextStep: String) {
        statusUpdate(nextStep, Phase.START)
    }

    fun update(nextStep: String) {
        statusUpdate(nextStep, Phase.MIDDLE)
    }

    fun end(nextStep: String) {
        statusUpdate(nextStep, Phase.END)
    }

    private fun statusUpdate(nextStep: String, phase: Phase) {
        if (phase == Phase.START) {
            if (currentlyRunning) {
                error("trying to start an already running chat: $nextStep")
            }
            startOfFirst = SimpleTimeMark.now()
            chatId = ChatUtils.getUniqueMessageId()
            title = nextStep
        }

        currentStep?.let {
            val format = startOfCurrent?.format() ?: error("start of current is null")
            previousSteps.add("$it $innerProgress$format")
        }
        innerProgress = ""
        if (!SkyBlockUtils.debug) return

        val time = SimpleTimeMark.now().toLocalDateTime()
        println("$time: $nextStep")
        currentStep = nextStep

        if (phase == Phase.END) {
            if (!currentlyRunning) {
                error("trying to end an not running chat: $nextStep")
            }
            update()
            currentStep = null
            startOfCurrent = null
            previousSteps.clear()
        } else {
            startOfCurrent = SimpleTimeMark.now()
            update()
        }
    }

    private fun SimpleTimeMark.format(): String {
        val duration = passedSince()
        val color = when {
            duration < 100.milliseconds -> "§8"
            duration < 10.seconds -> "§b"
            else -> "§c"
        }

        val format = duration.format(showMilliSeconds = true)
        return "$color$format§f"
    }

    private fun update() {
        val title = title ?: error("currentStep is null")
        val currentStep = currentStep ?: error("currentStep is null")
        val chatId = chatId ?: error("chatId is null: $currentStep")
        val totalTime = startOfFirst?.format() ?: error("startOfFirst is null: $currentStep")

        delayedSending?.let {
            if (MinecraftCompat.localPlayerOrNull != null) {
                it.send(chatId)
                delayedSending = null
            }
        }

        val hover = mutableListOf<String>()
        hover.add(title)
        hover.add("")
        hover.addAll(previousSteps)

        val text = if (currentlyRunning) {
            val currentTime = startOfCurrent?.format() ?: error("startOfCurrent is null")
            val currentLine = "$currentStep $innerProgress$currentTime"
            hover.add(currentLine)
            hover.add("")
            hover.add("running for: $totalTime")
            currentLine
        } else {
            hover.add("")
            hover.add("done after: $totalTime")
            "$currentStep $totalTime"
        }

        val delayedSending = DelayedSending(text, hover.joinToString("\n"))
        if (MinecraftCompat.localPlayerOrNull != null) {
            delayedSending.send(chatId)
        } else {
            this.delayedSending = delayedSending
        }
    }

    private enum class Phase {
        START,
        MIDDLE,
        END,
    }
}
