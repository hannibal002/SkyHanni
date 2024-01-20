package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DojoRankDisplay {

    private val config get() = SkyHanniMod.feature.crimsonIsle
    private var display = emptyList<String>()
    private val patternGroup = RepoPattern.group("inventory.dojo.rankdisplay")
    private val testNamePattern by patternGroup.pattern("name", "(?<color>§\\w)Test of (?<name>.*)")
    private val testRankPattern by patternGroup.pattern("rank", "(?:§\\w)+Your Rank: (?<rank>§\\w.) §8\\((?<score>\\d+)\\)")

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.dojoRankDisplayPosition.renderStrings(display, posLabel = "Dojo Rank Display")
    }

    private fun drawDisplay(items: Collection<ItemStack>) = buildList {
        var totalScore = 0
        for (stack in items) {
            val name = stack.displayName ?: continue
            testNamePattern.matchMatcher(name) {
                val testColor = group("color")
                val testName = group("name")
                for (line in stack.getLore()) {
                    testRankPattern.matchMatcher(line) {
                        val rank = group("rank")
                        val score = group("score").toInt()
                        val color = if (score in 0 .. 99) "§c" else "§a"
                        totalScore += score
                        add("$testColor$testName§f: $rank §7($color${score.addSeparators()}§7)")
                    }
                }
            }
        }

        // TODO: use repo
        val beltPoints = listOf(
            "§fWhite Belt" to 0,
            "§eYellow Belt" to 1000,
            "§aGreen Belt" to 2000,
            "§9Blue Belt" to 4000,
            "§6Brown Belt" to 6000,
            "§8Black Belt" to 7000
        )
        val currentBelt = beltPoints.lastOrNull { totalScore >= it.second } ?: beltPoints.first()
        val currentIndex = beltPoints.indexOf(currentBelt)
        val nextBelt = beltPoints.getOrNull(currentIndex + 1) ?: beltPoints.first()
        val pointsNeededForNextBelt = 0.coerceAtLeast(nextBelt.second.minus(totalScore))

        add("§7Total Score: §6${totalScore.addSeparators()} §7(§8${currentBelt.first}§7)")

        if (pointsNeededForNextBelt != 0)
            add("§7Points needed for ${nextBelt.first}§f: §6${pointsNeededForNextBelt.addSeparators()}")
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Challenges") return
        display = drawDisplay(event.inventoryItems.values)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = emptyList()
    }

    private fun isEnabled() =
        IslandType.CRIMSON_ISLE.isInIsland() && LorenzUtils.skyBlockArea == "Dojo" && config.showDojoRankDisplay
}