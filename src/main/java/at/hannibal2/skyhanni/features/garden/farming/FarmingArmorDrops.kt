package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class FarmingArmorDrops {
    private var display = listOf<String>()
    private val drops = mutableMapOf<ArmorDropType, Int>()
    private var hasArmor = false
    private var tick = 0
    private val armorPattern = Pattern.compile("(FERMENTO|CROPIE|SQUASH|MELON)_(LEGGINGS|CHESTPLATE|BOOTS|HELMET)")
    private val config get() = SkyHanniMod.feature.garden

    enum class ArmorDropType(val dropName: String, val chatMessage: String) {
        CROPIE("§9Cropie", "§6§lRARE CROP! §r§f§r§9Cropie §r§b(Armor Set Bonus)"),
        SQUASH("§5Squash", "§6§lRARE CROP! §r§f§r§5Squash §r§b(Armor Set Bonus)"),
        FERMENTO("§6Fermento", "§6§lRARE CROP! §r§f§r§6Fermento §r§b(Armor Set Bonus)"),
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        for (dropType in ArmorDropType.values()) {
            if (dropType.chatMessage == event.message) {
                addDrop(dropType)
                if (config.farmingArmorDropsEnabled && config.farmingArmorDropsHideChat) {
                    event.blockedReason = "farming_armor_drops"
                }
            }
        }
    }

    private fun addDrop(drop: ArmorDropType) {
        val old = drops[drop] ?: 0
        drops[drop] = old + 1
        saveConfig()
        update()
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<String> {
        val help = mutableListOf<String>()
        help.add("§7RNG Drops for Farming Armor:")
        for ((drop, amount) in drops.sortedDesc()) {
            val dropName = drop.dropName
            help.add(" §7- §e${amount.addSeparators()}x $dropName")
        }

        return help
    }

    private fun saveConfig() {
        val map = SkyHanniMod.feature.hidden.gardenFarmingArmorDrops
        map.clear()
        for ((drop, amount) in drops) {
            map[drop.toString()] = amount
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for ((rawName, amount) in SkyHanniMod.feature.hidden.gardenFarmingArmorDrops) {
            drops[ArmorDropType.valueOf(rawName)] = amount
        }
        update()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.farmingArmorDropsEnabled) return
        if (!hasArmor) return

        config.farmingArmorDropsPos.renderStrings(display, posLabel = "Farming Armor Drops")
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!GardenAPI.inGarden()) return
        if (!config.farmingArmorDropsEnabled) return

        tick++
        if (tick % 30 == 0) {
            checkArmor()
        }
    }

    private fun checkArmor() {
        val armorPieces = InventoryUtils.getArmor()
            .mapNotNull { it?.getInternalName() }
            .count { armorPattern.matcher(it).matches() }
        hasArmor = armorPieces > 1
    }
}