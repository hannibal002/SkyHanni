package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.inventory.SkyMartCopperPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getCultivatingCounter
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeCounter
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object GardenAPI {
    private val cropsPerSecond: MutableMap<CropType, Int> get() = SkyHanniMod.feature.hidden.gardenCropsPerSecond

    var toolInHand: String? = null
    var cropInHand: CropType? = null
    var mushroomCowPet = false
    var onBarnPlot = false

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
            onBarnPlot = ScoreboardData.sidebarLinesFormatted.contains(" §7⏣ §aThe Garden")

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
        val crop = toolItem?.getCropType()
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
        return if (isOtherTool(internalName)) internalName else null
    }

    private fun isOtherTool(internalName: String): Boolean {
        if (internalName.startsWith("DAEDALUS_AXE")) return true

        if (internalName.startsWith("BASIC_GARDENING_HOE")) return true
        if (internalName.startsWith("ADVANCED_GARDENING_AXE")) return true

        if (internalName.startsWith("BASIC_GARDENING_AXE")) return true
        if (internalName.startsWith("ADVANCED_GARDENING_HOE")) return true

        if (internalName.startsWith("ROOKIE_HOE")) return true

        return false
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onProfileJoin(event: ProfileJoinEvent) {
        if (cropsPerSecond.isEmpty()) {
            for (cropType in CropType.values()) {
                cropType.setSpeed(-1)
            }
        }
    }

    fun inGarden() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN

    fun ItemStack.getCropType(): CropType? {
        val internalName = getInternalName()
        return CropType.values().firstOrNull { internalName.startsWith(it.toolName) }
    }

    fun readCounter(itemStack: ItemStack): Long = itemStack.getHoeCounter() ?: itemStack.getCultivatingCounter() ?: -1L

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

    fun MutableList<Any>.addCropIcon(crop: CropType) {
        try {
            add(crop.icon)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    fun isSpeedDataEmpty() = cropsPerSecond.values.sum() < 0

    fun hideExtraGuis() = ComposterOverlay.inInventory || AnitaMedalProfit.inInventory || SkyMartCopperPrice.inInventory
}