package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class InfernoMinionFeatures {
    private val config get() = SkyHanniMod.feature.minions
    private val infernoMinionTitlePattern by RepoPattern.pattern(
        "minion.infernominiontitle",
        "Inferno Minion .*"
    )
    private val fuelItemIds = listOf(
        "INFERNO_FUEL_MAGMA_CREAM",
        "INFERNO_FUEL_GLOWSTONE_DUST",
        "INFERNO_FUEL_NETHER_STALK",
        "INFERNO_FUEL_BLAZE_ROD",
        "INFERNO_FUEL_CRUDE_GABAGOOL",
        "INFERNO_HEAVY_MAGMA_CREAM",
        "INFERNO_HEAVY_GLOWSTONE_DUST",
        "INFERNO_HEAVY_NETHER_STALK",
        "INFERNO_HEAVY_BLAZE_ROD",
        "INFERNO_HEAVY_CRUDE_GABAGOOL",
        "INFERNO_HYPERGOLIC_MAGMA_CREAM",
        "INFERNO_HYPERGOLIC_GLOWSTONE_DUST",
        "INFERNO_HYPERGOLIC_NETHER_STALK",
        "INFERNO_HYPERGOLIC_BLAZE_ROD",
        "INFERNO_HYPERGOLIC_CRUDE_GABAGOOL",
    )
    private var isInfernoMinion = false
    @SubscribeEvent
    fun onMinionOpen(event: InventoryOpenEvent) {
        val inventoryName = event.inventoryName
        isInfernoMinion = infernoMinionTitlePattern.matches(inventoryName)
    }

    @SubscribeEvent
    fun onMinionClose(event: InventoryCloseEvent) {
        isInfernoMinion = false
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.infernoFuelBlocker) return
        if (!isInfernoMinion) return

        val containsFuel = fuelItemIds.contains(NEUInternalName.fromItemNameOrNull(event.container.getSlot(19).stack.displayName)?.asString())
        if (event.slot?.slotNumber == 19 && containsFuel) {
            if (KeyboardManager.isModifierKeyDown()) return;
            event.cancel()

            val message = "§c§l[SkyHanni] is blocking you from taking this out! (Hold CTRL to override)"
            val data = event.slot.stack.tagCompound.getCompoundTag("display").getTagList("Lore", 8)
            if (data.getStringTagAt(data.tagCount() - 1).toString() == message) return;
            data.appendTag(NBTTagString(message))
        }
        if (event.slot?.slotNumber == 53) {
            if (KeyboardManager.isModifierKeyDown()) return;
            if (!containsFuel) return
            event.cancel()

            val message = "§c§l[SkyHanni] is blocking you from picking this minion up! (Hold CTRL to override)"
            val data = event.slot.stack.tagCompound.getCompoundTag("display").getTagList("Lore", 8)
            if (data.getStringTagAt(data.tagCount() - 1).toString() == message) return;
            data.appendTag(NBTTagString(message))
        }
    }
}
