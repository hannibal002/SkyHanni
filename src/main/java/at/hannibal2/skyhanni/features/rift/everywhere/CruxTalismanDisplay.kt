package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.find
import at.hannibal2.skyhanni.utils.StringUtils.findMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object CruxTalismanDisplay {

    private val config get() = RiftAPI.config.cruxTalisman

    private val patternGroup = RepoPattern.group("rift.everywhere.crux.new")
    private val progressPattern by patternGroup.pattern(
        "progress",
        "(?<tier>§[0-9a-z][IV1-4-]+)\\s+(?<name>§[0-9a-z]\\w+)§[0-9a-z]:\\s*(?<progress>§[0-9a-z](?:§[0-9a-z])?MAXED|§[0-9a-z]\\d+§[0-9a-z]/§[0-9a-z]\\d+)"
    )
    private val totalBonusesPattern by patternGroup.pattern(
        "bonuses",
        "^§7Total Bonuses"
    )
    private val maxedPattern by patternGroup.pattern(
        "maxed",
        "(?<progress>\\d+)/\\d+"
    )

    private val partialName = "CRUX_TALISMAN"
    private var display = emptyList<List<Any>>()
    private val displayLine = mutableListOf<Crux>()
    private val bonusesLine = mutableListOf<String>()
    private var maxed = false
    private var percentValue = 0.0

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderStringsAndItems(
            display,
            posLabel = "Crux Talisman Display"
        )
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        var maxedKill = 0
        var percent = 0
        for (crux in displayLine)
            if (crux.maxed)
                maxedKill++
        if (maxedKill == 6)
            maxed = true

        if (!config.compactWhenMaxed && maxed) maxed = false

        if (displayLine.isNotEmpty()) {
            addAsSingletonList("§7Crux Talisman Progress: ${if (maxed) "§a§lMAXED!" else "§a$percentValue%"}")
            if (!maxed) {
                displayLine.forEach {
                    percent += if (config.compactWhenMaxed) {
                        if (!it.maxed) {
                            maxedPattern.findMatcher(it.progress.removeColor()) {
                                group("progress").toInt()
                            } ?: 0
                        } else 100
                    } else {
                        if (it.progress.contains("MAXED"))
                            100
                        else {
                            maxedPattern.findMatcher(it.progress.removeColor()) {
                                group("progress").toInt()
                            } ?: 0
                        }
                    }
                    addAsSingletonList("  ${it.tier} ${it.name}: ${it.progress}")
                }
            }
        }
        percentValue = ((percent.toDouble() / 600) * 100).roundToPrecision(1)
        if (bonusesLine.isNotEmpty() && config.showBonuses.get()) {
            addAsSingletonList("§7Bonuses:")
            bonusesLine.forEach { addAsSingletonList("  $it") }
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(2)) return
        if (!InventoryUtils.getItemsInOwnInventory().any { it.getInternalName().startsWith(partialName) }) return

        displayLine.clear()
        bonusesLine.clear()
        maxed = false
        var bonusFound = false
        val inventoryStack = InventoryUtils.getItemsInOwnInventory()
        for (stack in inventoryStack) {
            line@ for (line in stack.getLore()) {
                progressPattern.findMatcher(line) {
                    val tier = group("tier").replace("-", "0")
                    val name = group("name")
                    val progress = group("progress")
                    val crux = Crux(name, tier, progress, progress.contains("MAXED"))
                    displayLine.add(crux)
                }
                if (totalBonusesPattern.find(line)) {
                    bonusFound = true
                    continue@line
                }
                if (bonusFound) {
                    if (line.isEmpty()) {
                        bonusFound = false
                        continue@line
                    }
                    bonusesLine.add(line)
                }
            }
        }
        update()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.showBonuses) { update() }
    }

    data class Crux(val name: String, val tier: String, val progress: String, val maxed: Boolean)

    fun isEnabled() = RiftAPI.inRift() && config.enabled && LorenzUtils.skyBlockArea != "Mirrorverse"
}
