package at.hannibal2.skyhanni.data.garden.crop

import at.hannibal2.skyhanni.events.CropUpgradeUpdateEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getByNameOrNull
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenCropUpgrades {
    // TODO USE SH-REPO
    private val tierPattern = "§7Current Tier: §[0-9a-e](\\d)§7/§a9".toRegex()
    private val chatUpgradePattern = " {2}§r§6§lCROP UPGRADE §e§f([\\w ]+)§7 #(\\d)".toRegex()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        chatUpgradePattern.matchEntire(event.message)?.groups?.let { matches ->
            val crop = getByNameOrNull(matches[1]!!.value) ?: return
            val level = matches[2]!!.value.toInt()
            crop.setUpgradeLevel(level)
        }
        CropUpgradeUpdateEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Crop Upgrades") return
        event.inventoryItems.forEach { (_, item) ->
            val crop = item.name?.removeColor()?.let { CropType.getByNameOrNull(it) } ?: return@forEach
            val level = item.getLore()
                .firstNotNullOfOrNull { tierPattern.matchEntire(it)?.groups?.get(1)?.value?.toIntOrNull() } ?: 0
            crop.setUpgradeLevel(level)
        }
        CropUpgradeUpdateEvent().postAndCatch()
    }

    companion object {
        private val cropUpgrades: MutableMap<CropType, Int>? get() = GardenAPI.storage?.cropUpgrades

        fun CropType.getUpgradeLevel() = cropUpgrades?.get(this)

        fun CropType.setUpgradeLevel(level: Int) {
            cropUpgrades?.put(this, level)
        }

    }
}
