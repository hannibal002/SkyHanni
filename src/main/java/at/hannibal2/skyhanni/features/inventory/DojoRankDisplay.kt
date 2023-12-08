package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DojoRankDisplay {

    private val config get() = SkyHanniMod.feature.inventory
    private var display = emptyList<String>()
    private val testNameRegex = "(?<color>§\\w)Test of (?<name>.*)".toRegex()
    private val testRankRegex = "(?:§\\w)+Your Rank: (?<rank>§\\w.) §8\\((?<score>\\d+)\\)".toPattern()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.dojoRankDisplayPosition.renderStrings(display, posLabel = "Dojo Rank Display")
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Challenges") return
        val newDisplay = mutableListOf<String>()
        var totalScore = 0
        for (stack in event.inventoryItems.values) {
            for (line in stack.getLore()) {
                val name = stack.displayName ?: return
                testRankRegex.matchMatcher(line) {
                    val testColor = testNameRegex.find(name)?.groupValues?.get(1)
                    val testName = testNameRegex.find(name)?.groupValues?.get(2)
                    val rank = group("rank")
                    val score = when (val s = group("score").toInt()) {
                        in 0 .. 999 -> "§c$s"
                        else -> "§a$s"
                    }
                    totalScore += group("score").toInt()
                    newDisplay.add("$testColor$testName§6: $rank §7($score§7)")
                }
            }
        }
        val belt = when (totalScore) {
            in 0 .. 999 -> "§fWhite Belt"
            in 1000 .. 1999 -> "§cYellow Belt"
            in 2000 .. 3999 -> "§aGreen Belt"
            in 4000 .. 5999 -> "§9Blue Belt"
            in 6000 .. 6999 -> "§6Brown Belt"
            else -> "§8Black Belt"
        }
        newDisplay.add("")
        newDisplay.add("§7Total Score: §6$totalScore §7(§8$belt§7)")
        display = newDisplay
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = emptyList()
    }

    private fun isEnabled() =
        config.showDojoRankDisplay && LorenzUtils.skyBlockIsland == IslandType.CRIMSON_ISLE && LorenzUtils.skyBlockArea == "Dojo"

}