package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.blockBreakStreak
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.getSpeedBonus
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.getTimerColor
import at.hannibal2.skyhanni.features.mining.FlowstateHelper.streakEndTimer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FlowstateHelper {
    private val config get() = SkyHanniMod.feature.mining.flowstateHelper
    var streakEndTimer = SimpleTimeMark.farPast()
        private set
    var blockBreakStreak = 0
        private set

    private var display: List<Renderable>? = null
    private var displayDirty = false
    private var displayHibernating = true
    private var timeSinceHibernation = SimpleTimeMark.farPast()

    private var flowstateCache: Int? = null

    private val pickobulusPattern by RepoPattern.pattern(
        "mining.pickobulus.blockdestroy",
        "§7Your §r§aPickobulus §r§7destroyed §r§e(?<amount>\\d+) §r§7blocks!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockMined(event: OreMinedEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (flowstateCache == null) return

        displayHibernating = false
        streakEndTimer = 10.seconds.fromNow()
        blockBreakStreak += event.extraBlocks.values.sum()
        displayDirty = true
        createDisplay()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return

        attemptClearDisplay()
    }

    private fun attemptClearDisplay() {
        if (streakEndTimer.isInFuture()) return
        blockBreakStreak = 0
        displayDirty = true
        if (!displayHibernating) timeSinceHibernation = SimpleTimeMark.now()
        displayHibernating = true
        createDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (!config.enabled) return
        if (flowstateCache == null) return

        if (displayHibernating && config.autoHide > -1 && timeSinceHibernation.passedSince() > config.autoHide.seconds) return
        if (display == null || streakEndTimer.isInFuture()) {
            createDisplay()
        }

        display?.let {
            config.position.renderRenderables(it, extraSpace = 1, "Flowstate Helper")
        }
    }

    private fun createDisplay() {
        if (displayDirty) {
            displayDirty = false
            FlowstateElements.STREAK.create()
            FlowstateElements.SPEED.create()
        }
        FlowstateElements.TIMER.create()
        FlowstateElements.COMPACT.create()
        display = config.appearance.map { it.renderable }
    }

    fun getSpeedBonus(): Int {
        val flowstateLevel = flowstateCache ?: 0
        if (blockBreakStreak >= 200) return 200 * flowstateLevel
        return blockBreakStreak * flowstateLevel
    }

    @SubscribeEvent
    fun onChangeItem(event: ItemInHandChangeEvent) {
        hasFlowstate()
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        streakEndTimer = SimpleTimeMark.farPast()
        attemptClearDisplay()
    }

    fun getTimerColor(timeRemaining: Duration): String {
        if (!config.colorfulTimer) return "§6"
        return when (timeRemaining) {
            in 0.seconds..2.seconds -> "§c"
            in 2.seconds..5.seconds -> "§#§e§c§7§b§3§6§/"
            in 5.seconds..7.seconds -> "§e"
            in 7.seconds..10.seconds -> "§a"
            else -> "§6"
        }
    }

    private fun hasFlowstate() {
        val enchantList = InventoryUtils.getItemInHand()?.getEnchantments() ?: run {
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

enum class FlowstateElements(val label: String, var renderable: Renderable = Renderable.string("")) {
    TITLE("§d§lFlowstate Helper", Renderable.string("§d§lFlowstate Helper")),
    TIMER("Time Remaining: §b9.71"),
    STREAK("Streak: §7234"),
    SPEED("§6+600⸕"),
    COMPACT("§7x40 §6+120⸕ §b(9.71)"),
    ;

    override fun toString() = label

    private val config get() = SkyHanniMod.feature.mining.flowstateHelper

    fun create() {
        if (this !in config.appearance) return

        renderable = when (this) {
            TIMER -> {
                var timeRemaining = streakEndTimer.minus(SimpleTimeMark.now())
                if (timeRemaining < 0.seconds) timeRemaining = 0.seconds

                Renderable.string(
                    "Time Remaining: ${getTimerColor(timeRemaining)}${
                        timeRemaining.format(
                            TimeUnit.SECOND, true, maxUnits = 2, showSmallerUnits = true,
                        )
                    }",
                )
            }

            STREAK -> {
                val textColor = if (blockBreakStreak < 200) "§7" else "§f"
                Renderable.string("Streak: $textColor$blockBreakStreak")
            }

            SPEED -> {
                Renderable.string("§6+${getSpeedBonus()}⸕")
            }

            COMPACT -> {
                var timeRemaining = streakEndTimer.minus(SimpleTimeMark.now())
                if (timeRemaining < 0.seconds) timeRemaining = 0.seconds

                Renderable.string(
                    "§7x$blockBreakStreak " + "§6+${getSpeedBonus()}⸕ " + "${getTimerColor(timeRemaining)}${
                        timeRemaining.format(
                            TimeUnit.SECOND, true, maxUnits = 2, showSmallerUnits = true,
                        )
                    }",
                )
            }

            else -> return
        }
    }

    companion object {
        @JvmField
        val defaultOption = listOf(
            TITLE, TIMER, STREAK, SPEED,
        )
    }
}
