package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ReforgeHelper {

    enum class ReforgeType {
        Sword,
        Ranged,
        Armor,
        Tool,
        Equipment
    }

    class Reforge(val name: String, val type: ReforgeType)

    private val reforges = listOf(
        Reforge("Clean", ReforgeType.Armor),
        Reforge("Firece", ReforgeType.Armor),
        Reforge("Heavy", ReforgeType.Armor),
        Reforge("Light", ReforgeType.Armor),
        Reforge("Mythic", ReforgeType.Armor),
        Reforge("Pure", ReforgeType.Armor),
        Reforge("Smart", ReforgeType.Armor),
        Reforge("Titanic", ReforgeType.Armor),
        Reforge("Wise", ReforgeType.Armor),
    )

    val reforgeMenu by RepoPattern.pattern("menu.reforge", "Reforge Item")

    var isInReforgeMenu = false

    fun isReforgeMenu(chestName: String) = reforgeMenu.matches(chestName)

    val pos: Position = Position(-200, 85, true, true)

    fun enable() = LorenzUtils.inSkyBlock && isInReforgeMenu

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!enable()) return
    }

    @SubscribeEvent
    fun onOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isReforgeMenu(event.inventoryName)) return
        isInReforgeMenu = true
        updateDisplay()
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        if (!enable()) return
        isInReforgeMenu = false
    }

    var display = listOf<String>()

    fun updateDisplay() {
        display = listOf(
            "Reforge Overlay"

        )
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!enable()) return
        pos.renderStrings(display, 0, "Reforge Overlay")
        pos
    }

    @SubscribeEvent
    fun onRepo(event: RepositoryReloadEvent) {

    }
}
