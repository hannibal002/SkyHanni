package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.darkRectButton
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.emptyText
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * This class allows to log actions and their duration of long, async tasks in chat.
 * Ideally for repo reload.
 */
class ChatProgressUpdates private constructor(val category: ChatProgressCategory) {
    private var startOfFirst: SimpleTimeMark? = null
    private var title: String? = null
    private var currentlyRunning = false
    private val previousSteps = mutableListOf<String>()
    private var startOfCurrent: SimpleTimeMark? = null
    private var currentStep: String? = null
    private var innerProgress = ""
    private var innerProgressMax = 0
    private val innerProgressCount = AtomicInteger(0)
    var currentText: ProgressText? = null

    data class ProgressText(val text: String, val hoverText: String)

    init {
        updates.add(this)
    }

    private fun isEnabled() = category.enabled

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

    private fun start(nextStep: String) {
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
                ErrorManager.logErrorStateWithData(
                    "error properly logging something in SkyHanni",
                    "trying to start an already running chat",
                    "next step" to nextStep,
                    "last step" to currentStep?.lastOrNull(),
                )
            }
            currentlyRunning = true
            startOfFirst = SimpleTimeMark.now()
            title = nextStep
        }
        if (phase == Phase.UPDATE) {
            if (!currentlyRunning) {
                ErrorManager.logErrorStateWithData(
                    "error properly logging something in SkyHanni",
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
                ErrorManager.logErrorStateWithData(
                    "error properly logging something in SkyHanni",
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
        val title = title ?: error("title is null")
        val currentStep = currentStep ?: error("currentStep is null")
        val totalTime = startOfFirst?.format() ?: error("startOfFirst is null: $currentStep")

        val hover = buildList {
            add("§e$title")
            add("")
            addAll(previousSteps)
            val currentTime = startOfCurrent?.format() ?: error("startOfCurrent is null")
            val currentLine = "§8- §f$currentStep $innerProgress$currentTime"
            add(currentLine)
            add("")
            if (currentlyRunning) {
                add("§7Running for: $totalTime")
            } else {
                add("§aDone after: $totalTime")
            }
        }

        val text = if (currentlyRunning) {
            "§8- §f$currentStep $innerProgress${startOfCurrent?.format()}"
        } else {
            "§a✓ §f$currentStep $totalTime"
        }

        currentText = ProgressText(text, hover.joinToString("\n"))
        if (isEnabled()) {
            dirty = true
        }
    }

    private enum class Phase {
        START,
        UPDATE,
        END,
    }

    class ChatProgressCategory(val categoryName: String) {
        val updates = mutableListOf<ChatProgressUpdates>()
        var enabled = false

        fun start(label: String): ChatProgressUpdates {
            val progress = ChatProgressUpdates(this)
            progress.start("$categoryName $label")
            updates.add(progress)
            return progress
        }

        fun toggle() {
            enabled = !enabled
            dirty = true
        }
    }

    @SkyHanniModule
    companion object {
        private var display: Renderable? = null
        private var dirty = false
        private var showCategoryList = false
        private val config get() = SkyHanniMod.feature.dev.devTool
        private val categories = mutableListOf<ChatProgressCategory>()
        private val updates = mutableListOf<ChatProgressUpdates>()

        fun category(categoryName: String): ChatProgressCategory {
            val category = ChatProgressCategory(categoryName)
            categories.add(category)
            return category
        }

        private fun loadAvailableCategories(): List<List<Renderable>> = buildList {
            if (!showCategoryList) return emptyList()
            add(listOf(Renderable.text("§d§lChat Progress Categories")))
            add(listOf(Renderable.emptyText()))

            for (category in categories) {
                val stateColor = if (category.enabled) "§a" else "§c"
                val stateSymbol = if (category.enabled) "✓" else "✗"

                val nameRenderable = Renderable.text("§7${category.categoryName}")
                val stateRenderable = darkRectButton(
                    content = Renderable.text("$stateColor$stateSymbol ${if (category.enabled) "Enabled" else "Disabled"}"),
                    onClick = {
                        category.toggle()
                    },
                    startState = category.enabled,
                    padding = 3,
                )

                add(listOf(nameRenderable, stateRenderable))
            }
        }

        private fun updateDisplay() {
            val availableCategories = loadAvailableCategories()
            val activeCategories = categories.filter { it.enabled }
            if (availableCategories.isEmpty() && activeCategories.isEmpty()) {
                display = null
                return
            }

            display = Renderable.vertical(
                buildList {
                    if (availableCategories.isNotEmpty()) {
                        add(Renderable.table(availableCategories, ySpacing = 2))
                    }
                    if (activeCategories.isNotEmpty()) {
                        add(Renderable.text("§d§lDebug Log"))
                        addAll(getUpdates(activeCategories))
                    }
                },
            )
        }

        private fun getUpdates(activeCategories: List<ChatProgressCategory>): MutableList<Renderable> {
            val updates = mutableListOf<Renderable>()
            for (category in activeCategories) {
                for (update in category.updates) {
                    update.currentText?.let { progress ->
                        val hoverable = Renderable.hoverTips(
                            Renderable.text(progress.text),
                            progress.hoverText.split("\n"),
                        )
                        updates.add(hoverable)
                    }
                }
            }
            return updates
        }

        @HandleEvent(GuiRenderEvent::class)
        fun onRenderOverlay() {
            display?.let {
                config.chatProgressPosition.renderRenderable(it, "Chat Progress Updates")
            }
        }

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shdebugprogress") {
                description = "Toggling chat progress updates"
                category = CommandCategory.DEVELOPER_DEBUG
                simpleCallback {
                    showCategoryList = !showCategoryList
                    ChatUtils.chat("Category list: ${if (showCategoryList) "§ashown" else "§chidden"}")
                    updateDisplay()
                }
            }
        }

        @HandleEvent(onlyOnSkyblock = true)
        fun onTick(event: SkyHanniTickEvent) {
            if (!event.isMod(2)) return

            for (update in updates.filter { it.isEnabled() && it.currentlyRunning }) {
                update.update()
            }

            if (dirty) {
                updateDisplay()
                dirty = false
            }
        }
    }
}
