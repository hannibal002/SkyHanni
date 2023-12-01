package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkymallDisplay {
    private val config get() = SkyHanniMod.feature.mining
    var currentPerk = "nothing"

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.skymallDisplay) return
        if (!LorenzUtils.inSkyBlock) return
        when (currentPerk) {
            "nothing" -> config.skymallDisplayPosition.renderString("§cNo perks found! Open §b/hotm §cto refresh", posLabel = "Sky Mall Display")
            "unknown" -> {
                config.skymallDisplayPosition.renderString("§cUnknown perk!", posLabel = "Sky Mall Display")
            }
            else -> config.skymallDisplayPosition.renderString("§eSky Mall:§f $currentPerk", posLabel = "Sky Mall Display")
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message.removeColor().startsWith("New buff:")) {
            currentPerk = event.message
                .replace("New buff", "")
                .replace(": ", "")
                .replace(".", "")
        }
    }

    @SubscribeEvent
    fun onHotmOpen(event: InventoryUpdatedEvent) {
        if (!config.skymallDisplay) return
        if (!LorenzUtils.inSkyBlock) return
        if (event.inventoryName != "Heart of the Mountain") return
        for((slot, item) in event.inventoryItems) {
            if (item.name == "§aSky Mall") {
                currentPerk = when (item.getLore()[16]) {
                    "§8 ■ §7Gain §a+100 §6⸕ Mining Speed§7." -> "§fGain §a+100 §6⸕ Mining Speed"
                    "§8 ■ §7Gain §a+50 §6☘ Mining Fortune§7." -> "§fGain §a+50 §6☘ Mining Fortune"
                    "§8 ■ §7Gain §a+15% §7more Powder while" -> "§fGain §a+15% §fmore Powder while mining"
                    "§8 ■ §7Reduce Pickaxe Ability cooldown" -> "§fReduce Pickaxe Ability cooldown by §a20%"
                    "§8 ■ §7§a10x §7chance to find Golden" -> "§f§a10x §fchance to find Golden and Diamond Goblins"
                    "§8 ■ §7Gain §a5x §9Titanium §7drops." -> "§fGain §a5x §9Titanium §fdrops"
                    else -> {
                        // todo copy item nbt so people can paste it in support
                        LorenzUtils.chat("unknown event: ${item.getLore()[16]}")
                        "unknown"
                    }
                }
            }
        }

    }
}
