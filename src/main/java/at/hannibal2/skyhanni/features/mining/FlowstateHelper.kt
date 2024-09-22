package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FlowstateHelper {
    private val config get() = SkyHanniMod.feature.mining
    private var streakEndTimer = SimpleTimeMark.farPast()
    private var blockBreakStreak = 0

    private var display: List<Renderable>? = null
    private var displayDirty = false

    private var flowstateCache: Int? = null

    private val pickobulusPattern by RepoPattern.pattern(
        "mining.pickobulus.blockdestroy",
        "§7Your §r§aPickobulus §r§7destroyed §r§e(?<amount>\\d+) §r§7blocks!"
    )

    enum class FlowstateElements(val label: String, var renderable: Renderable) {
        TITLE("§d§lFlowstate Helper", Renderable.string("§d§lFlowstate Helper")),
        TIMER("Time Remaining: §b6.71", Renderable.string("")),
        STREAK("Streak: §7234", Renderable.string("")),
        SPEED("§6+600⸕", Renderable.string("")),
        ;

        override fun toString() = label

        companion object {
            @JvmField
            val defaultOption = listOf(
                TITLE, TIMER, STREAK, SPEED
            )
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockMined(event: OreMinedEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (flowstateCache == null) return

        streakEndTimer = SimpleTimeMark.now().plus(10.seconds)
        blockBreakStreak += event.extraBlocks.values.sum()
        createDisplay()
        displayDirty = true
        attemptClearDisplay()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) { //TODO: Remove once #2540 is merged
        if (!MiningAPI.inCustomMiningIsland()) return
        if (flowstateCache == null) return

        pickobulusPattern.matchMatcher(event.message) {
            streakEndTimer = SimpleTimeMark.now().plus(10.seconds)
            blockBreakStreak += group("amount").toInt()
            createDisplay()
            attemptClearDisplay()
            displayDirty = true
        }
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
        createDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (!config.flowstateHelper) return
        if (flowstateCache == null) return

        if (display == null || streakEndTimer.isInFuture()) createDisplay()
        display?.let {
            config.flowstateHelperPosition.renderRenderables(
                it,
                1,
                "Flowstate Helper",
                true
            )
        }
    }

    private fun createDisplay() {
        if (displayDirty) {
            displayDirty = false
            createDisplayTimer()
            createDisplayBlock()
            createDisplaySpeed()
        } else {
            createDisplayTimer()
        }
        display = config.flowstateHelperAppearance.map { it.renderable }
    }

    private fun createDisplayTimer(): Renderable {
        var timeRemaining = streakEndTimer.minus(SimpleTimeMark.now()).inPartialSeconds
        if (timeRemaining < 0.0) timeRemaining = 0.0

        FlowstateElements.TIMER.renderable = Renderable.string("Time Remaining: §b$timeRemaining")
        return FlowstateElements.TIMER.renderable
    }

    private fun createDisplayBlock(): Renderable {
        val textColor = if (blockBreakStreak < 200) "§7" else "§f"
        FlowstateElements.STREAK.renderable = Renderable.string("Streak: $textColor$blockBreakStreak")
        return FlowstateElements.STREAK.renderable
    }

    private fun createDisplaySpeed(): Renderable {
        FlowstateElements.SPEED.renderable = Renderable.string("§6+${getSpeedBonus()}⸕")
        return FlowstateElements.SPEED.renderable
    }

    private fun getSpeedBonus(): Int {
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

    private fun hasFlowstate(): Int? {
        val enchantList = InventoryUtils.getItemInHand()?.getEnchantments() ?: return null
        if ("ultimate_flowstate" !in enchantList) return null
        flowstateCache = enchantList.getValue("ultimate_flowstate")
        return flowstateCache
    }
}
