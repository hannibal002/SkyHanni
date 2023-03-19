package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class GardenAPI {
    var tick = 0

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!inGarden()) return
        if (event.packet !is C09PacketHeldItemChange) return
        checkItemInHand()
    }

    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        if (!inGarden()) return
        checkItemInHand()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!inGarden()) return
        if (tick++ % 10 != 0) return

        // We ignore random hypixel moments
        Minecraft.getMinecraft().currentScreen ?: return
        checkItemInHand()
    }

    private fun checkItemInHand() {
        val heldItem = Minecraft.getMinecraft().thePlayer.heldItem
        val crop = loadCropInHand(heldItem)
        if (cropInHand != crop) {
            cropInHand = crop
            GardenToolChangeEvent(crop, heldItem).postAndCatch()
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onProfileJoin(event: ProfileJoinEvent) {
        if (cropsPerSecond.isEmpty()) {
            // TODO use enum
            for (key in GardenCropMilestones.cropCounter.keys) {
                cropsPerSecond[key] = -1
            }
        }
    }

    private fun loadCropInHand(heldItem: ItemStack?): String? {
        if (heldItem == null) return null
        return getCropTypeFromItem(heldItem, true)
    }

    companion object {
        fun inGarden() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN

        var cropInHand: String? = null
        val cropsPerSecond: MutableMap<String, Int> get() = SkyHanniMod.feature.hidden.gardenCropsPerSecond

        fun getCropTypeFromItem(item: ItemStack, withDaedalus: Boolean = false): String? {
            val internalName = item.getInternalName()

            return when {
                internalName.startsWith("THEORETICAL_HOE_WHEAT") -> "Wheat"
                internalName.startsWith("THEORETICAL_HOE_CARROT") -> "Carrot"
                internalName.startsWith("THEORETICAL_HOE_POTATO") -> "Potato"
                internalName.startsWith("PUMPKIN_DICER") -> "Pumpkin"
                internalName.startsWith("THEORETICAL_HOE_CANE") -> "Sugar Cane"
                internalName.startsWith("MELON_DICER") -> "Melon"
                internalName == "CACTUS_KNIFE" -> "Cactus"
                internalName == "COCO_CHOPPER" -> "Cocoa Beans"
                internalName == "FUNGI_CUTTER" -> "Mushroom"
                internalName.startsWith("THEORETICAL_HOE_WARTS") -> "Nether Wart"

                internalName.startsWith("DAEDALUS_AXE") && withDaedalus -> "Daedalus Axe"

                else -> null
            }
        }

        fun readCounter(itemStack: ItemStack): Int {
            if (itemStack.hasTagCompound()) {
                val tag = itemStack.tagCompound
                if (tag.hasKey("ExtraAttributes", 10)) {
                    val ea = tag.getCompoundTag("ExtraAttributes")
                    if (ea.hasKey("mined_crops", 99)) {
                        return ea.getInteger("mined_crops")
                    }

                    // only using cultivating when no crops counter is there
                    if (ea.hasKey("farmed_cultivating", 99)) {
                        return ea.getInteger("farmed_cultivating")
                    }
                }
            }
            return -1
        }

        fun getCropsPerSecond(itemName: String): Int? {
            return cropsPerSecond[itemNameToCropName(itemName)]
        }

        fun itemNameToCropName(itemName: String): String {
            if (itemName == "Red Mushroom" || itemName == "Brown Mushroom") {
                return "Mushroom"
            }
            return itemName
        }

        private fun getItemStackForCrop(crop: String): ItemStack {
            val internalName = NEUItems.getInternalName(if (crop == "Mushroom") "Red Mushroom" else crop)
            return NEUItems.getItemStack(internalName)
        }

        fun addGardenCropToList(crop: String, list: MutableList<Any>) {
            try {
                list.add(getItemStackForCrop(crop))
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
    }
}