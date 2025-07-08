package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.blockBreakStreak
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.getSpeedBonus
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.getStreakColor
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.getTimerColor
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.personalBest
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.streakEndTimer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FlowstateHelper {
    private val config get() = SkyHanniMod.feature.mining.flowstateHelper
    var personalBest
        get() = ProfileStorageData.profileSpecific?.mining?.flowstatePersonalBest ?: 0
        private set(value) {
            ProfileStorageData.profileSpecific?.mining?.flowstatePersonalBest = value
        }

    var streakEndTimer = SimpleTimeMark.farPast()
        private set
    var blockBreakStreak = 0
        private set

    private var display: List<Renderable> = emptyList()
    private var displayDirty = false
    private var displayHibernating = true
    private var timeSinceHibernation = SimpleTimeMark.farPast()
    private var timeSinceMax = SimpleTimeMark.farPast()
    private var displayMaxed = false

    private var flowstateCache: Int? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockMined(event: OreMinedEvent) {
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) return
        if (flowstateCache == null) return

        displayHibernating = false
        streakEndTimer = 10.seconds.fromNow()
        blockBreakStreak += event.extraBlocks.values.sum()
        displayDirty = true
        createDisplay()
    }

    @HandleEvent
    fun onTick() {
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) return

        attemptClearDisplay()
    }

    private fun attemptClearDisplay() {
        if (streakEndTimer.isInFuture()) return
        if (blockBreakStreak > personalBest) {
            // no point telling them it's a new personal best if they never got to max speed before
            if (personalBest > 200 && config.personalBestMessage) {
                ChatUtils.chat("§d§lNEW FLOWSTATE PERSONAL BEST!§f Streak: $blockBreakStreak. You beat your old personal best by ${blockBreakStreak - personalBest} Blocks!")
            }
            personalBest = blockBreakStreak
        }
        blockBreakStreak = 0
        timeSinceMax = SimpleTimeMark.farPast()
        displayMaxed = false
        displayDirty = true
        if (!displayHibernating) timeSinceHibernation = SimpleTimeMark.now()
        displayHibernating = true
        createDisplay()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!IslandTypeTags.CUSTOM_MINING.inAny() || !config.enabled) return
        if (flowstateCache == null && !streakEndTimer.isInFuture()) return

        if (shouldAutoHide()) return
        if (display.isEmpty() || streakEndTimer.isInFuture()) {
            createDisplay()
        }

        config.position.renderRenderables(display, extraSpace = 1, "Flowstate Helper")
    }

    private fun shouldAutoHide(): Boolean {
        if (config.autoHide < 0) return false
        val time = 10.seconds - config.autoHide.seconds
        return (streakEndTimer - time).isInPast()
    }

    private fun createDisplay() {
        if (displayDirty) {
            displayDirty = false
            FlowstateElements.STREAK.create()
            FlowstateElements.SPEED.create()
            FlowstateElements.PERSONAL_BEST.create()
        }
        if (!displayHibernating) {
            FlowstateElements.TIMER.create()
            FlowstateElements.COMPACT.create()
        }
        display = config.appearance.map { it.renderable }
    }

    fun getSpeedBonus(): Int {
        val flowstateLevel = flowstateCache ?: 0

        return if (blockBreakStreak >= 200) {
            if (!displayMaxed) {
                displayMaxed = true
                timeSinceMax = SimpleTimeMark.now()
            }
            200 * flowstateLevel
        } else blockBreakStreak * flowstateLevel
    }

    @HandleEvent
    fun onChangeItem(event: ItemInHandChangeEvent) {
        hasFlowstate()
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        streakEndTimer = SimpleTimeMark.farPast()
        attemptClearDisplay()
    }

    fun getTimerColor(timeRemaining: Duration): String {
        if (!config.colorfulTimer) return "§b"
        return when (timeRemaining) {
            in 0.seconds..2.seconds -> "§c"
            in 2.seconds..4.seconds -> "§#§e§c§7§b§3§6§/"
            in 4.seconds..6.seconds -> "§e"
            in 6.seconds..8.seconds -> "§a"
            in 8.seconds..10.seconds -> "§2"
            else -> "§6"
        }
    }

    fun getStreakColor(streak: Int = blockBreakStreak): String = if (streak < 200) "§e" else "§a"

    private fun hasFlowstate() {
        val enchantList = InventoryUtils.getItemInHand()?.getHypixelEnchantments() ?: run {
            flowstateCache = null
            return
        }
        if ("ultimate_flowstate" !in enchantList) {
            flowstateCache = null
            return
        }
        flowstateCache = enchantList.getValue("ultimate_flowstate")
    }
}

enum class FlowstateElements(val label: String, var renderable: Renderable = StringRenderable("")) {
    TITLE("§d§lFlowstate Helper", StringRenderable("§d§lFlowstate Helper")),
    TIMER("§fTime Remaining: §b9.71"),
    STREAK("§7Streak: §f123/200"),
    SPEED("§6+600⸕"),
    COMPACT("§7x40 §6+120⸕ §b(9.71)"),
    PERSONAL_BEST("§7Personal Best: §780§8/§d750"),
    ;

    override fun toString() = label

    fun create() {
        if (this !in config.appearance) return

        renderable = when (this) {
            TIMER -> {
                val timeRemaining = streakEndTimer.timeUntil().coerceAtLeast(0.seconds)

                StringRenderable("§7Time Remaining: ${timeRemaining.formatTime()}")
            }

            STREAK -> {
                val textColor = getStreakColor()
                val string = "§7Streak: $textColor$blockBreakStreak"
                StringRenderable(string + if (blockBreakStreak < 200) "§8/200" else "")
            }

            SPEED -> {
                StringRenderable("§6+${getSpeedBonus()}⸕")
            }

            COMPACT -> {
                val timeRemaining = streakEndTimer.timeUntil().coerceAtLeast(0.seconds)

                StringRenderable(
                    "§7x${getStreakColor()}$blockBreakStreak " +
                        "§6+${getSpeedBonus()}⸕ " +
                        timeRemaining.formatTime(),
                )
            }

            PERSONAL_BEST -> {
                if (blockBreakStreak <= personalBest) {
                    StringRenderable(
                        "§7Personal Best: §7${getStreakColor()}$blockBreakStreak§8/§d$personalBest"
                    )
                } else {
                    StringRenderable("§d§lNew Personal Best ${getStreakColor()}$blockBreakStreak")
                }
            }

            else -> return
        }
    }

    companion object {
        private val config get() = SkyHanniMod.feature.mining.flowstateHelper

        private fun Duration.formatTime(): String {
            return getTimerColor(this) + format(TimeUnit.SECOND, true, maxUnits = 2, showSmallerUnits = true)
        }

        @JvmField
        val defaultOption = listOf(
            TITLE, TIMER, STREAK, SPEED,
        )
    }
}
