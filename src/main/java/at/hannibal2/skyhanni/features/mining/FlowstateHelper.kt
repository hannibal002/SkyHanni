package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FlowstateHelper {
    private val config get() = SkyHanniMod.feature.mining
    private var timeSinceBreak = SimpleTimeMark.farPast()
    private var display: List<Renderable>? = null

    private var blockBreakStreak = 0

    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockMined(event: OreMinedEvent) {
        if (!MiningAPI.inCustomMiningIsland()) return
        if (hasFlowstate() == null) return

        timeSinceBreak = SimpleTimeMark.now().plus(10.seconds)
        blockBreakStreak += event.extraBlocks.values.sum()
        event.extraBlocks.forEach { (block, amount) ->
            ChatUtils.debug("Mined $amount ${block.name}")
        }
        createDisplay()

        DelayedRun.runDelayed(10.seconds) {
            if (timeSinceBreak.isInFuture()) return@runDelayed
            blockBreakStreak = 0
            createDisplay()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.flowstateHelper) return

        if (display == null || timeSinceBreak.isInFuture()) createDisplay()
        display?.let {
            config.flowstateHelperPosition.renderRenderables(
                listOf(Renderable.verticalContainer(it, 1)),
                posLabel = "Flowstate Helper",
                addToGuiManager = true
            )
        }
    }

    private fun createDisplay() {
        val helperDisplay: MutableList<Renderable> = mutableListOf()

        helperDisplay.add(Renderable.string("§d§lFlowstate Helper"))
        helperDisplay.add(createDisplayTimer())
        helperDisplay.add(createDisplayBlock())
        helperDisplay.add(Renderable.string("§6+${getSpeedBonus()}⸕"))

        display = helperDisplay.toList()
    }

    private fun createDisplayTimer(): Renderable {
        var timeRemaining = timeSinceBreak.minus(SimpleTimeMark.now()).inPartialSeconds
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
        return Renderable.string("Streak: $textColor$blockBreakStreak")
    }

    private fun getSpeedBonus(): Int {
        val flowstateLevel = hasFlowstate() ?: 0
        if (blockBreakStreak >= 200) return 200 * flowstateLevel
        return blockBreakStreak * flowstateLevel
    }

    private fun hasFlowstate(): Int? {
        val enchantList = ItemUtils.getHeldItem()?.getEnchantments() ?: return null
        if ("ultimate_flowstate" !in enchantList) return null
        return enchantList.getValue("ultimate_flowstate")
    }
}
