package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.OwnInventorItemUpdateEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*

class GardenCropMilestoneDisplay {
    private val display = mutableListOf<List<Any>>()
    private var currentCrop: String? = null
    private val cultivatingData = mutableMapOf<String, Int>()
    private var needsInventory = true

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        SkyHanniMod.feature.garden.cropMilestoneDisplayPos.renderStringsAndItems(display)
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        needsInventory = true
    }

    @SubscribeEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        needsInventory = false
        update()
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventorItemUpdateEvent) {
        val itemStack = event.itemStack
        val counter = readCounter(itemStack)
        if (counter == -1) return
        val crop = getCropTypeFromItem(itemStack) ?: return
        if (cultivatingData.containsKey(crop)) {
            val old = cultivatingData[crop]!!
            val diff = counter - old
            GardenCropMilestones.cropCounter[crop] = GardenCropMilestones.cropCounter[crop]!! + diff
            if (currentCrop == crop) {
                update()
            }
        }
        cultivatingData[crop] = counter
    }

    private fun readCounter(itemStack: ItemStack): Int {
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

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!isEnabled()) return

        if (tick++ % 5 != 0) return

        val cropInHand = getCropInHand()
        if (currentCrop != cropInHand) {
            currentCrop = cropInHand
            update()
        }
    }

    private fun update() {
        display.clear()
        currentCrop?.let {
            val crops = GardenCropMilestones.cropCounter[it]
            if (crops == null) {
                println("cropCounter is null for '$it'")
                return
            }
            display.add(Collections.singletonList("§6Crop Milestones"))

            val list = mutableListOf<Any>()

            try {
                val internalName = NEUItems.getInternalName(if (it == "Mushroom") "Red Mushroom" else it)
                val itemStack = NEUItems.getItemStack(internalName)
                list.add(itemStack)
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
            list.add(it)
            display.add(list)

            val currentTier = GardenCropMilestones.getTierForCrops(crops)

            val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier)
            val nextTier = currentTier + 1
            val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier)

            val have = crops - cropsForCurrentTier
            val need = cropsForNextTier - cropsForCurrentTier

            val haveFormat = LorenzUtils.formatInteger(have)
            val needFormat = LorenzUtils.formatInteger(need)
            display.add(Collections.singletonList("§7Progress to Tier $nextTier§8: §e$haveFormat§8/§e$needFormat"))

            if (needsInventory) {
                display.add(Collections.singletonList("§cOpen §e/cropmilestones §cto update!"))
            }
        }
    }

    private fun getCropInHand(): String? {
        val heldItem = Minecraft.getMinecraft().thePlayer.heldItem ?: return null
        if (readCounter(heldItem) == -1) return null
        return getCropTypeFromItem(heldItem)
    }

    private fun getCropTypeFromItem(heldItem: ItemStack): String? {
        val name = heldItem.name ?: return null
        for ((crop, _) in GardenCropMilestones.cropCounter) {
            if (name.contains(crop)) {
                return crop
            }
        }
        if (name.contains("Coco Chopper")) {
            return "Cocoa Beans"
        }
        if (name.contains("Fungi Cutter")) {
            return "Mushroom"
        }
        return null
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock &&
            SkyHanniMod.feature.garden.cropMilestoneDisplay &&
            LorenzUtils.skyBlockIsland == IslandType.GARDEN
}