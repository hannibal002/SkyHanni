package at.hannibal2.skyhanni.features.misc.limbo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class LimboPlaytime {
    private lateinit var modifiedArray: MutableList<String>
    private var setMinutes = false
    private val minutesRegex by RepoPattern.pattern("limbo.tooltip.minutes", "§5§o§a\\d+(\\.\\d+)? minutes.+\$")
    private val hoursRegex by RepoPattern.pattern("limbo.tooltip.hours", "§5§o§b\\d+(\\.\\d+)? hours.+\$")

    private var wholeMinutes: Long = 0
    private var hoursString: String = ""

    private val config get() = SkyHanniMod.feature.misc

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory !is ContainerLocalMenu) return
        if (event.inventory.displayName.unformattedText != "Detailed /playtime") return
        if (event.slotNumber != 43) return
        if (config.limboPlaytime == 0) return

        val limboItem by lazy {
            val neuItem = NEUItems.getItemStack("ENDER_PEARL")
            if (wholeMinutes >= 60) Utils.createItemStack(neuItem.item, "§aLimbo", "§7Playtime: §a${wholeMinutes.addSeparators()} minutes", "§7Or: §b$hoursString hours")
            else Utils.createItemStack(neuItem.item, "§aLimbo", "§7Playtime: §a$wholeMinutes minutes")
        }
        event.replaceWith(limboItem)
    }

    @SubscribeEvent
    fun onHoverItem(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.slot.inventory.displayName.unformattedText.startsWith("Detailed /playtime")) return
        if (event.slot.slotIndex != 4) return
        if (config.limboPlaytime == 0) return

        val lore = event.toolTip
        val hoursArray = lore.filter { hoursRegex.matches(it) }.toMutableList()
        val minutesArray = lore.filter { minutesRegex.matches(it) }.toMutableList()

        addLimbo(hoursArray, minutesArray)
        remakeArray(event.toolTip, minutesArray, hoursArray)
    }

    @SubscribeEvent
    fun onRenderGUI(event: InventoryOpenEvent) {
        if (event.inventoryName != "Detailed /playtime") return
        if (config.limboPlaytime == 0) return
        val playtime = config.limboPlaytime.seconds
        val wholeHours = playtime.inWholeHours
        wholeMinutes = playtime.inWholeMinutes
        if ((wholeMinutes%60).toInt() == 0) {
            hoursString = "$wholeHours"
        } else {
            val minutes:Float = ((wholeMinutes - wholeHours * 60).toFloat() / 60)
            hoursString = wholeHours.addSeparators()+minutes.round(1).toString().replace("0", "")
        }
    }

    private fun addLimbo(hoursArray: MutableList<String>, minutesArray: MutableList<String>) {
        if (wholeMinutes >= 60) {
            val hours = config.limboPlaytime.seconds.inWholeHours
            val minutes = (config.limboPlaytime.seconds.inWholeMinutes-(hours*60).toFloat()/6).toInt()
            modifiedArray = hoursArray
            if (minutes == 0) modifiedArray.add("§b$hours hours §7on Limbo")
            else modifiedArray.add("§b$hoursString hours §7on Limbo")
            modifiedArray = modifiedArray.sortedByDescending {
                it.substringAfter("§b").substringBefore(" hours").toDoubleOrNull()
            }.toMutableList()
            setMinutes = false
        }
        else {
            val minutes = config.limboPlaytime.seconds.inWholeMinutes
            modifiedArray = minutesArray
            modifiedArray.add("§a$minutes minutes §7on Limbo")
            modifiedArray = modifiedArray.sortedByDescending {
                it.substringAfter("§a").substringBefore(" minutes").toDoubleOrNull()
            }.toMutableList()
            setMinutes = true
        }
    }

    private fun remakeArray(toolTip: MutableList<String>, minutesArray: MutableList<String>, hoursArray: MutableList<String>) {
        val firstLine = toolTip.first()
        val totalPlaytime = toolTip.last()
        toolTip.clear()
        toolTip.add(firstLine)
        if (!setMinutes) {
            toolTip.addAll(modifiedArray)
            toolTip.addAll(minutesArray)
        } else {
            toolTip.addAll(hoursArray)
            toolTip.addAll(modifiedArray)
        }
        toolTip.add(totalPlaytime)
    }
}
