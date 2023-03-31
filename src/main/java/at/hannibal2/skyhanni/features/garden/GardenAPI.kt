package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.*
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
        tick++
        if (tick % 10 == 0) {
            // We ignore random hypixel moments
            Minecraft.getMinecraft().currentScreen ?: return
            checkItemInHand()
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (inGarden()) {
            mushroomCowPet = event.tabList.any { it.startsWith(" Strength: §r§c❁") }
        }
    }

    private fun checkItemInHand() {
        val toolItem = Minecraft.getMinecraft().thePlayer.heldItem
        val crop = getCropTypeFromItem(toolItem)
        val newTool = getToolInHand(toolItem, crop)
        if (toolInHand != newTool) {
            toolInHand = newTool
            cropInHand = crop
            GardenToolChangeEvent(crop, toolItem).postAndCatch()
        }
    }

    private fun getToolInHand(toolItem: ItemStack?, crop: CropType?): String? {
        if (crop != null) return crop.cropName

        val internalName = toolItem?.getInternalName() ?: return null
        return if (internalName.startsWith("DAEDALUS_AXE")) "Other Tool" else null
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onProfileJoin(event: ProfileJoinEvent) {
        if (cropsPerSecond.isEmpty()) {
            for (cropType in CropType.values()) {
                cropType.setSpeed(-1)
            }
        }
    }

    companion object {
        var toolInHand: String? = null
        private val cropsPerSecond: MutableMap<CropType, Int> get() = SkyHanniMod.feature.hidden.gardenCropsPerSecond
        var cropInHand: CropType? = null
        var mushroomCowPet = false

        fun inGarden() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN

        fun getCropTypeFromItem(item: ItemStack?): CropType? {
            val internalName = item?.getInternalName() ?: return null
            return CropType.values().firstOrNull { internalName.startsWith(it.toolName) }
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

        fun CropType.getSpeed(): Int {
            val speed = cropsPerSecond[this]
            if (speed != null) return speed

            val message = "Set speed for $this to -1!"
            println(message)
            LorenzUtils.debug(message)
            setSpeed(-1)
            return -1
        }

        fun CropType.setSpeed(speed: Int) {
            cropsPerSecond[this] = speed
        }

        fun itemNameToCropName(itemName: String): CropType? {
            if (itemName == "Red Mushroom" || itemName == "Brown Mushroom") {
                return CropType.MUSHROOM
            }
            return CropType.getByName(itemName)
        }

        fun addGardenCropToList(crop: CropType, list: MutableList<Any>) {
            try {
                list.add(crop.icon)
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }

        fun isSpeedDataEmpty() = cropsPerSecond.values.sum() < 0
    }
}