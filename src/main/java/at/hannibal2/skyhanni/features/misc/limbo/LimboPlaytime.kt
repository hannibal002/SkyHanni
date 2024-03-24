package at.hannibal2.skyhanni.features.misc.limbo

import at.hannibal2.skyhanni.data.ProfileStorageData
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
import net.minecraft.item.ItemStack
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

    private val storage get() = ProfileStorageData.playerSpecific?.limbo

    private val itemID = "ENDER_PEARL".asInternalName()
    private val itemName = "§aLimbo"
    private lateinit var limboItem: ItemStack
    private var lastCreateCooldown = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory !is ContainerLocalMenu) return
        if (event.inventory.displayName.unformattedText != "Detailed /playtime") return
        if (event.slotNumber != 43) return
        if (storage?.playtime == 0) return

        if (lastCreateCooldown.passedSince() > 3.seconds) {
            lastCreateCooldown = SimpleTimeMark.now()
            limboItem = if (wholeMinutes >= 60) Utils.createItemStack(
                itemID.getItemStack().item,
                itemName,
                "§7Playtime: §a${wholeMinutes.addSeparators()} minutes",
                "§7Or: §b$hoursString hours"
            )
            else Utils.createItemStack(itemID.getItemStack().item, itemName, "§7Playtime: §a$wholeMinutes minutes")
        }
        event.replaceWith(limboItem)
    }

    @SubscribeEvent
    fun onHoverItem(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.slot.inventory.displayName.unformattedText.startsWith("Detailed /playtime")) return
        if (event.slot.slotIndex != 4) return
        val playtime = storage?.playtime ?: 0
        if (playtime <= 60) return

        val lore = event.toolTip
        println("------")
        lore.forEach{ println(it) }
        println("------")
        val hoursList = lore.filter { hoursPattern.matches(it) }.toMutableList()
        val minutesList = lore.filter { minutesPattern.matches(it) }.toMutableList()

        addLimbo(hoursList, minutesList)
        remakeList(event.toolTip, minutesList, hoursList)
    }

    @SubscribeEvent
    fun onRenderGUI(event: InventoryOpenEvent) {
        if (event.inventoryName != "Detailed /playtime") return
        val storedPlaytime = storage?.playtime ?: 0
        if (storedPlaytime < 60) return
        val playtime = storedPlaytime.seconds
        val wholeHours = playtime.inWholeHours
        wholeMinutes = playtime.inWholeMinutes
        if ((wholeMinutes % 60).toInt() == 0) {
            hoursString = "$wholeHours"
        } else {
            val minutes: Float = ((wholeMinutes - wholeHours * 60).toFloat() / 60)
            hoursString = wholeHours.addSeparators() + minutes.round(1).toString().replace("0", "")
        }
    }

    private fun addLimbo(hoursList: MutableList<String>, minutesList: MutableList<String>) {
        val storedPlaytime = storage?.playtime ?: 0
        if (wholeMinutes >= 60) {
            val hours = storedPlaytime.seconds.inWholeHours
            val minutes = (storedPlaytime.seconds.inWholeMinutes - (hours * 60).toFloat() / 6).toInt()
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
        } else {
            val minutes = storedPlaytime.seconds.inWholeMinutes
            modifiedList = minutesList
            modifiedList.add("§5§o§a$minutes minutes §7on Limbo")
            modifiedList = modifiedList.sortedByDescending {
                val matcher = minutesPattern.matcher(it)
                if (matcher.find()) {
                    matcher.group(1).toDoubleOrNull() ?: 0.0
                } else 0.0
            }.toMutableList()
            setMinutes = true
        }
    }

    private fun remakeList(
        toolTip: MutableList<String>,
        minutesList: MutableList<String>,
        hoursList: MutableList<String>
    ) {
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
