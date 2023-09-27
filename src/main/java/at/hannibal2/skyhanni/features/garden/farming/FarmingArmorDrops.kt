package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FarmingArmorDrops {
    private var display = emptyList<String>()
    private val storage get() = GardenAPI.config

    private var hasArmor = false
    private val armorPattern = "(FERMENTO|CROPIE|SQUASH|MELON)_(LEGGINGS|CHESTPLATE|BOOTS|HELMET)".toPattern()
    private val config get() = SkyHanniMod.feature.garden

    enum class ArmorDropType(val dropName: String, val chatMessage: String) {
        CROPIE("§9Cropie", "§6§lRARE CROP! §r§f§r§9Cropie §r§b(Armor Set Bonus)"),
        SQUASH("§5Squash", "§6§lRARE CROP! §r§f§r§5Squash §r§b(Armor Set Bonus)"),
        FERMENTO("§6Fermento", "§6§lRARE CROP! §r§f§r§6Fermento §r§b(Armor Set Bonus)"),
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        hasArmor = false
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        for (dropType in ArmorDropType.entries) {
            if (dropType.chatMessage == event.message) {
                addDrop(dropType)
                if (config.farmingArmorDropsHideChat) {
                    event.blockedReason = "farming_armor_drops"
                }
            }
        }
    }

    private fun addDrop(drop: ArmorDropType) {
        val drops = storage?.farmArmorDrops ?: return
        val old = drops[drop] ?: 0
        drops[drop] = old + 1
        update()
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val drops = storage?.farmArmorDrops ?: return@buildList

        add("§7RNG Drops for Farming Armor:")
        for ((drop, amount) in drops.sortedDesc()) {
            val dropName = drop.dropName
            add(" §7- §e${amount.addSeparators()}x $dropName")
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
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
    fun onTick(event: LorenzTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.farmingArmorDropsEnabled) return

        if (event.isMod(30)) {
            checkArmor()
        }
    }

    private fun checkArmor() {
        val armorPieces = InventoryUtils.getArmor()
            .mapNotNull { it?.getInternalName_old() }
            .count { armorPattern.matcher(it).matches() }
        hasArmor = armorPieces > 1
    }
}