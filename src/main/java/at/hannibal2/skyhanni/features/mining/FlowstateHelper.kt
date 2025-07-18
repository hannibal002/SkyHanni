package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.UserLuckCalculateEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.blockBreakStreak
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.getSpeedBonus
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.getStreakColor
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.getTimerColor
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.personalBest
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.streakEndTimer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.Text
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.init.Items
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
                val newLuck = calculateFlowstateLuck(blockBreakStreak)
                val oldLuck = calculateFlowstateLuck(personalBest)
                val userLuckSegment = if (personalBest > 500) " §aYou Gained +${newLuck - oldLuck}✴ SkyHanni User Luck" else ""
                ChatUtils.chat(
                    "§d§lNEW FLOWSTATE PERSONAL BEST!§f Streak: $blockBreakStreak." +
                        " You beat your old personal best by ${blockBreakStreak - personalBest} Blocks!" + userLuckSegment,
                )
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

    fun getTimerColor(timeRemaining: Duration): Text {
        if (!config.colorfulTimer) return Text.of("§b")
        return when (timeRemaining) {
            in 0.seconds..2.seconds -> Text.of("§c")
            in 2.seconds..4.seconds -> ExtendedChatColor("#ec7b36", false).asText()
            in 4.seconds..6.seconds -> Text.of("§e")
            in 6.seconds..8.seconds -> Text.of("§a")
            in 8.seconds..10.seconds -> Text.of("§2")
            else -> Text.of("§6")
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


    /**
     * Best below 500 blocks gets no luck
     * Every block between 500-1000 gets 0.005 per block (500*0.005 = 2.5)
     * Every block between 1000-2000 gets 0.0025 per block (1000*0.0025 = 2.5)
     * Every block between 2000-7000 gets 0.001 per block (5000*0.001 = 5)
     * 10 Luck is the max
     */
    private fun calculateFlowstateLuck(best: Int): Float {
        var extraBlocks = best - 500
        if (0 > extraBlocks) return 0f
        if (extraBlocks <= 500) {
            return (0.005f * extraBlocks).roundTo(2)
        }
        extraBlocks -= 500
        if (extraBlocks <= 1000) {
            return (0.0025f * extraBlocks + 2.5f).roundTo(2)
        }
        extraBlocks -= 1000
        if (extraBlocks <= 5000) {
            return (0.001f * extraBlocks + 5f).roundTo(2)
        }
        // 10 is the max luck
        return 10f
    }

    @HandleEvent
    fun onUserLuck(event: UserLuckCalculateEvent) {
        if (personalBest < 500) return
        val luck = calculateFlowstateLuck(personalBest)
        event.addLuck(luck)
        val stack = ItemUtils.createItemStack(
            Items.enchanted_book,
            "§a✴ Flowstate Personal Best",
            arrayOf(
                "§8Enchantment",
                "",
                "§7Value: §a+$luck✴",
                "",
                "§8Gain more by getting a higher flowstate personal best",
                "§8Maxes out at §a+10✴ §8luck",
            ),
        )
        event.addItem(stack)
    }
}

enum class FlowstateElements(val label: String, var renderable: Renderable = Renderable.empty()) {
    TITLE("§d§lFlowstate Helper", Renderable.text("§d§lFlowstate Helper")),
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

                Renderable.text(Text.of("§7Time Remaining: ").append(timeRemaining.formatTime()))
            }

            STREAK -> {
                val textColor = getStreakColor()
                val string = "§7Streak: $textColor$blockBreakStreak"
                Renderable.text(string + if (blockBreakStreak < 200) "§8/200" else "")
            }

            SPEED -> {
                Renderable.text("§6+${getSpeedBonus()}⸕")
            }

            COMPACT -> {
                val timeRemaining = streakEndTimer.timeUntil().coerceAtLeast(0.seconds)

                Renderable.text(
                    Text.of(
                        "§7x${getStreakColor()}$blockBreakStreak " + "§6+${getSpeedBonus()}⸕ ",
                    ).append(
                        timeRemaining.formatTime(),
                    ),
                )
            }

            PERSONAL_BEST -> {
                if (blockBreakStreak <= personalBest) {
                    Renderable.text(
                        "§7Personal Best: §7${getStreakColor()}$blockBreakStreak§8/§d$personalBest",
                    )
                } else {
                    Renderable.text("§d§lNew Personal Best ${getStreakColor()}$blockBreakStreak")
                }
            }

            else -> return
        }
    }

    companion object {
        private val config get() = SkyHanniMod.feature.mining.flowstateHelper

        private fun Duration.formatTime(): Text {
            return getTimerColor(this).append(format(TimeUnit.SECOND, true, maxUnits = 2, showSmallerUnits = true))
        }

        @JvmField
        val defaultOption = listOf(
            TITLE, TIMER, STREAK, SPEED,
        )
    }
}
