package at.hannibal2.skyhanni.features.misc.limbo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LimboPlaytime {
    private lateinit var modifiedList: MutableList<Component>
    private var setMinutes = false
    private val patternGroup = RepoPattern.group("misc.limbo.tooltip")

    /**
     * REGEX-TEST: 28 minutes
     */
    private val minutesPattern by patternGroup.pattern(
        "minutes.new",
        "(?<minutes>[\\d.,]+) minutes.*$",
    )

    /**
     * REGEX-TEST: 687.7 hours
     * REGEX-TEST: 304.7 hours
     * REGEX-TEST: 61 hours
     * REGEX-TEST: 21.1 hours
     */
    private val hoursPattern by patternGroup.pattern(
        "hours.new",
        "(?<hours>[\\d.,]+) hours.*$",
    )

    private var tooltipPlaytime = mutableListOf<Component>()

    private var wholeMinutes = 0
    private var hoursString: String = ""

    private val storage get() = ProfileStorageData.playerSpecific?.limbo
    private val enabled get() = SkyHanniMod.feature.misc.showLimboTimeInPlaytimeDetailed

    private val itemID = "ENDER_PEARL".toInternalName()
    private const val ITEM_NAME = "§aLimbo"
    private lateinit var limboItem: ItemStack
    private var lastCreateCooldown = SimpleTimeMark.farPast()

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!enabled) return
        if (event.inventory !is SimpleContainer) return
        // TODO replace with InventoryDetector
        if (InventoryUtils.openInventoryName() != "Detailed /playtime") return
        if (event.slot != 43) return
        val playtime = storage?.playtime ?: 0
        if (playtime < 60) return

        if (lastCreateCooldown.passedSince() > 3.seconds) {
            lastCreateCooldown = SimpleTimeMark.now()
            limboItem = ItemUtils.createItemStack(
                itemID.getItemStack().item,
                ITEM_NAME,
                *createItemLore()
            )
        }
        event.replace(limboItem)
    }

    private fun createItemLore(): Array<String> = when {
        wholeMinutes >= 60 -> arrayOf(
            "§7Playtime: §a${wholeMinutes.addSeparators()} minutes",
            "§7Or: §b$hoursString hours",
        )

        wholeMinutes == 1 -> arrayOf("§7Playtime: §a$wholeMinutes minute")

        else -> arrayOf("§7Playtime: §a$wholeMinutes minutes")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onToolTip(event: ToolTipTextEvent) {
        if (!enabled) return
        // TODO replace with InventoryDetector
        if (!InventoryUtils.openInventoryName().startsWith("Detailed /playtime")) return
        if (event.slot?.containerSlot != 4) return
        val playtime = storage?.playtime ?: 0
        if (playtime <= 120) return

        val lore = event.toolTip
        val hoursList = lore.filter { hoursPattern.matches(it) }.toMutableList()
        val minutesList = lore.filter { minutesPattern.matches(it) }.toMutableList()

        addLimbo(hoursList, minutesList)
        remakeList(event.toolTip, minutesList, hoursList)
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName != "Detailed /playtime") return
        val playtime = (storage?.playtime ?: 0).seconds
        if (playtime < 60.seconds) return
        val wholeHours = playtime.inWholeHours
        wholeMinutes = playtime.inWholeMinutes.toInt()
        if ((wholeMinutes % 60) == 0) {
            hoursString = "$wholeHours"
        } else {
            val minutes: Float = ((wholeMinutes - wholeHours * 60).toFloat() / 60).roundTo(1)
            hoursString = wholeHours.addSeparators()
            if (findFloatDecimalPlace(minutes) != 0) {
                val minutesString = minutes.toString()
                hoursString += minutesString.substring(minutesString.indexOf("."))
            }
        }
    }

    private fun addLimbo(hoursList: MutableList<Component>, minutesList: MutableList<Component>) {
        val storedPlaytime = storage?.playtime ?: 0
        if (wholeMinutes >= 60) {
            modifiedList = hoursList
            modifiedList.add("§5§o§b$hoursString hours §7on Limbo")
            modifiedList = modifiedList.sortedByDescending {
                val matcher = hoursPattern.matcher(it.string)
                if (matcher.find()) {
                    matcher.group("hours").replace(",", "").toDoubleOrNull() ?: 0.0
                } else 0.0
            }.toMutableList()
            setMinutes = false
        } else {
            val minutes = storedPlaytime.seconds.inWholeMinutes
            modifiedList = minutesList
            modifiedList.add("§5§o§a$minutes minutes §7on Limbo")
            modifiedList = modifiedList.sortedByDescending {
                val matcher = minutesPattern.matcher(it.string)
                if (matcher.find()) {
                    matcher.group("minutes").toDoubleOrNull() ?: 0.0
                } else 0.0
            }.toMutableList()
            setMinutes = true
        }
    }

    private fun remakeList(
        toolTip: MutableList<Component>,
        minutesList: MutableList<Component>,
        hoursList: MutableList<Component>,
    ) {
        val firstList = mutableListOf<Component>()
        val lastList = mutableListOf<Component>()
        var hasPassed = false
        for (line in toolTip) {
            if (!(hoursPattern.matches(line) || minutesPattern.matches(line)) && !hasPassed) {
                firstList.add(line)
            } else hasPassed = true
        }
        hasPassed = false
        for (line in toolTip) {
            if (!(hoursPattern.matches(line) || minutesPattern.matches(line)) && hasPassed) {
                lastList.add(line)
            } else hasPassed = true
        }
        toolTip.clear()
        toolTip.addAll(firstList)
        if (!setMinutes) {
            toolTip.addAll(modifiedList)
            toolTip.addAll(minutesList)
        } else {
            toolTip.addAll(hoursList)
            toolTip.addAll(modifiedList)
        }
        toolTip.addAll(lastList)

        tooltipPlaytime = toolTip
    }

    private fun findFloatDecimalPlace(input: Float): Int {
        val string = input.toString()
        val dotIndex = string.indexOf(".")
        return (string[dotIndex + 1].toString().toInt())
    }
}
