package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CruxTalismanDisplay {

    private val config get() = SkyHanniMod.feature.rift.cruxTalisman
    private val partialName = "CRUX_TALISMAN"
    private var display = listOf<List<Any>>()
    private val displayLine = mutableListOf<Crux>()
    private val bonusesLine = mutableListOf<String>()
    private val progressPattern = ".*(?<tier>§[0-9a-z][IV1-4-]+)\\s+(?<name>§[0-9a-z]\\w+)§[0-9a-z]:\\s*(?<progress>§[0-9a-z](?:§[0-9a-z])?MAXED|(?:§[0-9a-z]\\d+§[0-9a-z]\\/§[0-9a-z]\\d+)).*".toPattern()
    private var maxed = false
    private var percent = 0.0

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderStringsAndItems(
                display,
                posLabel = "Crux Talisman Display"
        )
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        return buildList {
            var i = 0
            var p = 0
            for (crux in displayLine)
                if (crux.maxed)
                    i++
            if (i == 6)
                maxed = true

            if(!config.compactWhenMaxed && maxed) maxed = false

            if (displayLine.isNotEmpty()) {
                addAsSingletonList("§7Crux Talisman Progress: ${if (maxed) "§a§lMAXED!" else "§a$percent%"}")
                if (!maxed) {
                    displayLine.forEach {
                        if (config.compactWhenMaxed) {
                            if (!it.maxed) {
                                p += it.progress.removeColor().split("/")[0].toInt()
                                addAsSingletonList("  ${it.tier} ${it.name}: ${it.progress}")
                            } else {
                                addAsSingletonList("  ${it.tier} ${it.name}: ${it.progress}")
                                p += 100
                            }
                        } else {
                            addAsSingletonList("  ${it.tier} ${it.name}: ${it.progress}")
                            p += if (it.progress.contains("MAXED"))
                                100
                            else
                                it.progress.removeColor().split("/")[0].toInt()
                        }

                    }
                }
            }
            percent = ((p.toDouble() / 600) * 100).roundToPrecision(1)
            if (bonusesLine.isNotEmpty()) {
                addAsSingletonList("§7Bonuses:")
                bonusesLine.forEach { addAsSingletonList("  $it") }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(20)) return
        displayLine.clear()
        bonusesLine.clear()
        maxed = false
        val inventoryStack = InventoryUtils.getItemsInOwnInventory()
        inventoryStack.filter { it.getInternalName().contains(partialName) }.forEach { stack ->
            var bonusFound = false
            stack.getLore().forEach line@{ line ->
                progressPattern.matchMatcher(line) {
                    val tier = group("tier").replace("-", "0")
                    val name = group("name")
                    val progress = group("progress")
                    val crux = Crux(name, tier, progress, progress.contains("MAXED"))
                    displayLine.add(crux)
                }
                if (line.startsWith("§7Total Bonuses")) {
                    bonusFound = true
                    return@line
                }
                if (bonusFound) {
                    if (line.isEmpty()) {
                        bonusFound = false
                        return@line
                    }
                    bonusesLine.add(line)
                }
            }
        }
        update()
    }

    data class Crux(val name: String, val tier: String, val progress: String, val maxed: Boolean)

    fun isEnabled() = RiftAPI.inRift() && config.enabled && InventoryUtils.getItemsInOwnInventory().any { it.getInternalName().startsWith(partialName) }
}