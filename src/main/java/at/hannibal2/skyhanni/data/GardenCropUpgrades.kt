package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import io.github.moulberry.notenoughupdates.util.stripControlCodes
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenCropUpgrades {
    private val tierPattern = "§7Current Tier: §[0-9a-e](\\d)§7/§a9".toRegex()
    private val chatUpgradePattern = "\\s*§r§6§lCROP UPGRADE §e§f([\\w ]+)§7 #(\\d)§r".toRegex()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        chatUpgradePattern.matchEntire(event.message)?.groups?.let { matches ->
            val crop = CropType.getByItemName(matches[1]!!.value) ?: return
            val level = matches[2]!!.value.toInt()
            cropUpgrades[crop] = level
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        if (cropUpgrades.isEmpty()) {
            for (crop in CropType.values()) {
                cropUpgrades[crop] = 0
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName != "Crop Upgrades") return
        event.inventoryItems.forEach { (_, item) ->
            val crop = item.name?.stripControlCodes()?.let {
                CropType.getByName(it)
            } ?: return@forEach
            val level = item.getLore().firstNotNullOfOrNull {
                tierPattern.matchEntire(it)?.groups?.get(1)?.value?.toIntOrNull()
            } ?: return@forEach
            crop.setUpgradeLevel(level)
        }
    }

    companion object {
        val cropUpgrades: MutableMap<CropType, Int> get() = SkyHanniMod.feature.hidden.gardenCropUpgrades

        fun CropType.getUpgradeLevel() = cropUpgrades[this]!!

        fun CropType.setUpgradeLevel(level: Int) {
            cropUpgrades[this] = level
        }

    }
}
