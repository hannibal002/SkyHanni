package at.hannibal2.skyhanni.features.misc.limbo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class LimboPlaytime {
    private lateinit var modifiedList: MutableList<String>
    private var setMinutes = false
    private val patternGroup = RepoPattern.group("misc.limbo.tooltip")
    private val minutesPattern by patternGroup.pattern(
        "minutes",
        "§5§o§a([\\d.,]+) minutes.+\$"
    )
    private val hoursPattern by patternGroup.pattern(
        "hours",
        "§5§o§b([\\d.,]+) hours.+\$"
    )

    private var wholeMinutes: Long = 0
    private var hoursString: String = ""

    private val config get() = SkyHanniMod.feature.misc

    private val item = "ENDER_PEARL".asInternalName().getItemStack().item
    private val itemName = "§aLimbo"
    private var limboItem = Utils.createItemStack(item, itemName)
    private var lastCreateCooldown = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory !is ContainerLocalMenu) return
        if (event.inventory.displayName.unformattedText != "Detailed /playtime") return
        if (event.slotNumber != 43) return
        if (config.limboPlaytime == 0) return

        if (lastCreateCooldown.passedSince() > 3.seconds) {
            lastCreateCooldown = SimpleTimeMark.now()
            limboItem = if (wholeMinutes >= 60) Utils.createItemStack(item, itemName, "§7Playtime: §a${wholeMinutes.addSeparators()} minutes", "§7Or: §b$hoursString hours")
            else Utils.createItemStack(item, itemName, "§7Playtime: §a$wholeMinutes minutes")
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
        val hoursList = lore.filter { hoursPattern.matches(it) }.toMutableList()
        val minutesList = lore.filter { minutesPattern.matches(it) }.toMutableList()

        addLimbo(hoursList, minutesList)
        remakeList(event.toolTip, minutesList, hoursList)
    }

    @SubscribeEvent
    fun onRenderGUI(event: InventoryOpenEvent) {
        if (event.inventoryName != "Detailed /playtime") return
        if (config.limboPlaytime < 60) return
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

    private fun addLimbo(hoursList: MutableList<String>, minutesList: MutableList<String>) {
        if (wholeMinutes >= 60) {
            val hours = config.limboPlaytime.seconds.inWholeHours
            val minutes = (config.limboPlaytime.seconds.inWholeMinutes-(hours*60).toFloat()/6).toInt()
            modifiedList = hoursList
            if (minutes == 0) modifiedList.add("§5§o§b$hours hours §7on Limbo")
            else modifiedList.add("§5§o§b$hoursString hours §7on Limbo")
            modifiedList = modifiedList.sortedByDescending {
                val matcher = hoursPattern.matcher(it)
                if (matcher.find()) {
                    matcher.group(1).replace(",", "").toDoubleOrNull() ?: 0.0
                } else 0.0
            }.toMutableList()
            setMinutes = false
        }
        else {
            val minutes = config.limboPlaytime.seconds.inWholeMinutes
            modifiedList = minutesList
            modifiedList.add("§a$minutes minutes §7on Limbo")
            modifiedList = modifiedList.sortedByDescending {
                it.substringAfter("§a").substringBefore(" minutes").toDoubleOrNull()
            }.toMutableList()
            setMinutes = true
        }
    }

    private fun remakeList(toolTip: MutableList<String>, minutesList: MutableList<String>, hoursList: MutableList<String>) {
        val firstLine = toolTip.first()
        val totalPlaytime = toolTip.last()
        toolTip.clear()
        toolTip.add(firstLine)
        if (!setMinutes) {
            toolTip.addAll(modifiedList)
            toolTip.addAll(minutesList)
        } else {
            toolTip.addAll(hoursList)
            toolTip.addAll(modifiedList)
        }
        toolTip.add(totalPlaytime)
    }
}
