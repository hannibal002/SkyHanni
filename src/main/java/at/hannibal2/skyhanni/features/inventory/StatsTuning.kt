package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.MaxwellApi
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.RenderInventoryItemTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.StringUtils.createCommaSeparatedList
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@HanniModule
object StatsTuning {

    private val config get() = HanniMod.feature.inventory.statsTuning

    /**
     * REGEX-TEST: §7Stat has: §e3 points
     */
    private val statPointsPattern by RepoPattern.pattern(
        "inventory.statstuning.points",
        "§7Stat has: §e(?<amount>\\d+) points?",
    )

    @HandleEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        val inventoryName = event.inventoryName

        val stack = event.stack

        if (config.templateStats && inventoryName == "Stats Tuning") if (templateStats(stack, event)) return
        if (config.selectedStats && MaxwellApi.isThaumaturgyInventory(inventoryName) && renderTunings(
                stack,
                event,
            )
        ) return
        if (config.points && inventoryName == "Stats Tuning") points(stack, event)
    }

    private fun templateStats(stack: ItemStack, event: RenderInventoryItemTipEvent): Boolean {
        if (stack.displayName != "§aLoad") return false

        var grab = false
        val list = mutableListOf<String>()
        for (line in stack.getLore()) {
            if (line == "§7You are loading:") {
                grab = true
                continue
            }
            if (!grab) continue

            if (line == "") {
                grab = false
                continue
            }
            val text = line.split(":")[0]
            list.add(text)
        }
        if (list.isEmpty()) return false

        event.stackTip = list.joinToString(" + ")
        event.offsetX = 20
        event.offsetY = -5
        event.alignLeft = false
        return true
    }

    private fun renderTunings(stack: ItemStack, event: RenderInventoryItemTipEvent): Boolean {
        if (stack.displayName != "§aStats Tuning") return false
        val tunings = MaxwellApi.tunings ?: return false

        event.stackTip = tunings
            .map { tuning ->
                with(tuning) {
                    "$color$value$icon"
                }
            }
            .createCommaSeparatedList("§7")
        event.offsetX = 3
        event.offsetY = -5
        event.alignLeft = false
        return true
    }

    private fun points(stack: ItemStack, event: RenderInventoryItemTipEvent) {
        statPointsPattern.firstMatcher(stack.getLore()) {
            val points = group("amount")
            event.stackTip = points
        }
    }

    @HandleEvent(priority = HandleEvent.LOW, onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        val chestName = InventoryUtils.openInventoryName()
        if (!config.selectedTemplate || chestName != "Stats Tuning") return
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val stack = slot.stack
            val lore = stack.getLore()

            if (lore.any { it == "§aCurrently selected!" }) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "inventory.statsTuningSelectedStats", "inventory.statsTuning.selectedStats")
        event.move(3, "inventory.statsTuningSelectedTemplate", "inventory.statsTuning.selectedTemplate")
        event.move(3, "inventory.statsTuningTemplateStats", "inventory.statsTuning.templateStats")
        event.move(3, "inventory.statsTuningPoints", "inventory.statsTuning.points")
    }
}
