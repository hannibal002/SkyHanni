package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CityProjectMaterialHelper {
    private val config get() = SkyHanniMod.feature.misc.cityProject
    private var display = listOf<List<Any>>()
    private var inInventory = false

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showMaterials) return

        val lore = event.inventoryItems[4]?.getLore() ?: return
        if (lore.isEmpty()) return
        if (lore[0] != "§8City Project") return
        inInventory = true

        // internal name -> amount
        val materials = mutableMapOf<String, Int>()
        for ((_, item) in event.inventoryItems) {
            val itemName = item.name ?: continue
            if (itemName != "§eContribute this component!") continue
            fetchMaterials(item, materials)
        }

        println("materials: $materials")

        display = buildList(materials)
    }

    private fun buildList(materials: MutableMap<String, Int>) = buildList<List<Any>> {
        addAsSingletonList("§7City Project Materials")

        if (materials.isEmpty()) {
            addAsSingletonList("§cNo Materials to contribute.")
            return@buildList
        }

        for ((internalName, amount) in materials) {
            val stack = NEUItems.getItemStack(internalName)
            val name = stack.name ?: continue
            val list = mutableListOf<Any>()
            list.add(" §7- ")
            list.add(stack)

            list.add(Renderable.optionalLink("$name §ex${amount.addSeparators()}", {
                if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
                    LorenzUtils.setTextIntoSign("$amount")
                } else if (!NEUItems.neuHasFocus() && !LorenzUtils.noTradeMode) {
                    LorenzUtils.sendCommandToServer("bz ${name.removeColor()}")
                    OSUtils.copyToClipboard("$amount")
                }
            }) { inInventory && !NEUItems.neuHasFocus() })

            val price = NEUItems.getPrice(internalName) * amount
            val format = NumberUtil.format(price)
            list.add(" §7(§6$format§7)")
            add(list)
        }
    }

    private fun fetchMaterials(item: ItemStack, materials: MutableMap<String, Int>) {
        var next = false
        for (line in item.getLore()) {
            if (line == "§7Cost") {
                next = true
                continue
            }
            if (!next) continue
            if (line == "") break
            if (line.contains("Bits")) break

            println(" ")
            println("line: '$line'")
            val (name, amount) = ItemUtils.readItemAmount(line)
            if (name != null) {
                val internalName = NEUItems.getInternalName(name)
                println("internalName: $internalName")
                println("amount: $amount")
                val old = materials.getOrPut(internalName) { 0 }
                materials[internalName] = old + amount
            }
        }
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showMaterials) return
        if (!inInventory) return

        config.pos.renderStringsAndItems(display, posLabel = "City Project Materials")
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showReady) return
        if (!inInventory) return


        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            val stack = slot.stack ?: continue
            val lore = stack.getLore()
            if (lore.isEmpty()) continue
            val last = lore.last()
            if (last == "§eClick to contribute!") {
                slot highlight LorenzColor.YELLOW
            }
        }
    }

}
