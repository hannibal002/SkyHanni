package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class GardenAPI {

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!inGarden()) return
        if (tick++ % 5 != 0) return

        val crop = loadCropInHand()
        if (cropInHand != crop) {
            cropInHand = crop
            GardenToolChangeEvent().postAndCatch()
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

    private fun loadCropInHand(): String? {
        val heldItem = Minecraft.getMinecraft().thePlayer.heldItem ?: return null
        if (readCounter(heldItem) == -1) return null
        return getCropTypeFromItem(heldItem)
    }

    companion object {
        // TODO use everywhere instead of IslandType.GARDEN
        fun inGarden() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN

        var cropInHand: String? = null
        val cropsPerSecond: MutableMap<String, Int> get() = SkyHanniMod.feature.hidden.gardenCropsPerSecond

        fun getCropTypeFromItem(heldItem: ItemStack): String? {
            val name = heldItem.name ?: return null
            return getCropTypeFromItem(name)
        }

        fun getCropTypeFromItem(itemName: String): String? {
            for ((crop, _) in GardenCropMilestones.cropCounter) {
                if (itemName.contains(crop)) {
                    return crop
                }
            }
            if (itemName.contains("Coco Chopper")) {
                return "Cocoa Beans"
            }
            if (itemName.contains("Fungi Cutter")) {
                return "Mushroom"
            }
            return null
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
            if (itemName.endsWith(" Mushroom")) {
                return cropsPerSecond["Mushroom"]
            }
            return cropsPerSecond[itemName]
        }

    }
}