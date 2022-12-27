package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class DungeonLevelColor {

    private val pattern = Pattern.compile(" §.(.*)§f: §e(.*)§b \\(§e(.*)§b\\)")

    @SubscribeEvent
    fun onItemTooltip(event: ItemTooltipEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (!SkyHanniMod.feature.dungeon.partyFinderColoredClassLevel) return

        if (event.toolTip == null) return
        val guiChest = Minecraft.getMinecraft().currentScreen
        if (guiChest !is GuiChest) return
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()
        if (chestName != "Party Finder") return

        val stack = event.itemStack
        var index = 0
        for (line in stack.getLore()) {
            index++
            val matcher = pattern.matcher(line)
            if (!matcher.matches()) continue

            val playerName = matcher.group(1)
            val className = matcher.group(2)
            val level = matcher.group(3).toInt()
            val color = getColor(level)
            event.toolTip[index] = " §b$playerName§f: §e$className $color$level"
        }
    }

    private fun getColor(level: Int): String {
        if (level >= 50) return "§c§l"
        if (level >= 45) return "§c"
        if (level >= 40) return "§d"
        if (level >= 35) return "§b"
        if (level >= 30) return "§5"
        if (level >= 25) return "§9"
        if (level >= 20) return "§a"
        if (level >= 10) return "§f"
        return "§7"
    }
}
