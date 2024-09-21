package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
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

    private var titleRenderable = Renderable.string("")
    private var streakRenderable = Renderable.string("")
    private var speedRenderable = Renderable.string("")

    private val pickobulusPattern by RepoPattern.pattern(
        "mining.pickobulus.blockdestroy",
        "§7Your §r§aPickobulus §r§7destroyed §r§e(?<amount>\\d+) §r§7blocks!"
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockMined(event: OreMinedEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (hasFlowstate() == null) return

        streakEndTimer = SimpleTimeMark.now().plus(10.seconds)
        blockBreakStreak += event.extraBlocks.values.sum()
        createDisplay()
        displayDirty = true
        attemptClearDisplay()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (hasFlowstate() == null) return

        pickobulusPattern.matchMatcher(event.message) {
            streakEndTimer = SimpleTimeMark.now().plus(10.seconds)
            blockBreakStreak += group("amount").toInt()
            createDisplay()
            displayDirty = true
            attemptClearDisplay()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (hasFlowstate() == null) return

        attemptClearDisplay()
    }

    private fun attemptClearDisplay() {
        if (streakEndTimer.isInFuture()) return
        blockBreakStreak = 0
        createDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (!config.flowstateHelper) return
        if (hasFlowstate() == null) return

        if (display == null || streakEndTimer.isInFuture()) createDisplay()
        display?.let {
            config.flowstateHelperPosition.renderRenderables(
                listOf(Renderable.verticalContainer(it, 1)),
                posLabel = "Flowstate Helper",
                addToGuiManager = true
            )
        }
    }

    private fun createDisplay() {
        if (displayDirty) {
            displayDirty = false
            display = buildList {
                createDisplayTitle()
                createDisplayTimer()
                createDisplayBlock()
                createDisplaySpeed()
            }
        } else {
            display = buildList {
                titleRenderable
                createDisplayTimer()
                streakRenderable
                speedRenderable
            }
        }
    }

    private fun createDisplayTitle(): Renderable {
        titleRenderable = Renderable.string("§d§lFlowstate Helper")
        return titleRenderable
    }

    private fun createDisplayTimer(): Renderable {
        var timeRemaining = streakEndTimer.minus(SimpleTimeMark.now()).inPartialSeconds
        if (timeRemaining < 0.0) timeRemaining = 0.0

        val timerColor = when (timeRemaining) {
            in 0.0..3.0 -> "§c"
            in 3.0..6.0 -> "§e"
            in 6.0..10.0 -> "§a"
            else -> "§c"
        }
        return Renderable.string("Time Remaining: $timerColor$timeRemaining")
    }

    private fun createDisplayBlock(): Renderable {
        val textColor = if (blockBreakStreak < 200) "§7" else "§f"
        streakRenderable = Renderable.string("Streak: $textColor$blockBreakStreak")
        return streakRenderable
    }

    private fun createDisplaySpeed(): Renderable {
        speedRenderable = Renderable.string("§6+${getSpeedBonus()}⸕")
        return speedRenderable
    }

    private fun getSpeedBonus(): Int {
        val flowstateLevel = hasFlowstate() ?: 0
        if (blockBreakStreak >= 200) return 200 * flowstateLevel
        return blockBreakStreak * flowstateLevel
    }

    @SubscribeEvent
    fun itemInHand(event: ItemInHandChangeEvent) {

    }

    private fun hasFlowstate(): Int? {
        val enchantList = InventoryUtils.getItemInHand()?.getEnchantments() ?: return null
        if ("ultimate_flowstate" !in enchantList) return null
        return enchantList.getValue("ultimate_flowstate")
    }
}
