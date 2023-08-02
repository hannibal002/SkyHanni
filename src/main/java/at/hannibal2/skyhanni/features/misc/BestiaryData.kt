package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object BestiaryData {

    private val config get() = SkyHanniMod.feature.misc.bestiarySlotHighlightConfig
    private var display = emptyList<List<Any>>()
    private val mobList = mutableListOf<BestiaryMob>()
    private val stackList = mutableMapOf<Int, ItemStack>()
    private val progressPattern = "(?<current>[0-9kKmMbB,.]+)/(?<needed>[0-9kKmMbB,.]+$)".toPattern()
    private val titlePattern = "^(?:\\(\\d+/\\d+\\) )?(Bestiary|.+) ➜ (.+)$".toPattern()
    private var lastclicked = 0L
    private var inInventory = false
    private var indexes = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    )

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (inInventory) {
            config.position.renderStringsAndItems(
                display,
                extraSpace = -1,
                itemScale = 1.3,
                posLabel = "Bestiary Data"
            )
        }
    }

    @SubscribeEvent
    fun onRender(event: GuiContainerEvent.BackgroundDrawnEvent) {
        val inventoryName = InventoryUtils.openInventoryName()
        if (inventoryName.contains("Bestiary ➜") || inventoryName == "Bestiary") {
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                val stack = slot.stack
                val lore = stack.getLore()
                if (lore.any { it == "§7Overall Progress: §b100% §7(§c§lMAX!§7)" || it == "§7Families Completed: §a100§6% §7(§c§lMAX!§7)" }) {
                    slot highlight LorenzColor.GREEN
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val inventoryName = event.inventoryName
        if (isBestiaryGui(event.inventoryItems[4], inventoryName)) {
            stackList.putAll(event.inventoryItems)
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        mobList.clear()
        stackList.clear()
        inInventory = false
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun init() {
        mobList.clear()
        for ((index, stack) in stackList) {
            if (stack.displayName == " ") continue
            if (indexes.contains(index)) {
                inInventory = true
                val name = " [IVX0-9]+$".toPattern().matcher(stack.displayName).replaceFirst("")
                val level = " ([IVX0-9]+$)".toRegex().find(stack.displayName)?.groupValues?.get(1) ?: "0"
                var totalKillToMax: Long = 0
                var currentTotalKill: Long = 0
                var totalKillToTier: Long = 0
                var currentKillToTier: Long = 0
                var actualRealTotalKill: Long = 0
                for ((lineIndex, line) in stack.getLore().withIndex()) {
                    val loreLine = line.removeColor()
                    if (loreLine.startsWith("Kills: ")) {
                        actualRealTotalKill = "([0-9,.]+)".toRegex().find(loreLine)?.groupValues?.get(1)?.formatNumber()
                            ?: 0
                    }
                    if (loreLine.startsWith("                    ")) {
                        val previousLine = stack.getLore()[lineIndex - 1]
                        val progress = loreLine.substring(loreLine.lastIndexOf(' ') + 1)
                        if (previousLine.contains("Progress to Tier")) {
                            progressPattern.matchMatcher(progress) {
                                totalKillToTier = group("needed").formatNumber()
                                currentKillToTier = group("current").formatNumber()
                            }
                        } else if (previousLine.contains("Overall Progress")) {
                            progressPattern.matchMatcher(progress) {
                                totalKillToMax = group("needed").formatNumber()
                                currentTotalKill = group("current").formatNumber()
                            }
                        }
                    }
                }
                mobList.add(BestiaryMob(name, level, totalKillToMax, currentTotalKill, totalKillToTier, currentKillToTier, actualRealTotalKill))
            }
        }
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        init()
        val sortedMobList = when (config.sortingType) {
            0 -> mobList.sortedBy {
                when (config.displayType) {
                    0 -> it.totalKills
                    1 -> it.currentKillToNextLevel
                    2 -> it.killNeededToMax()
                    3 -> it.killNeededToNextLevel()
                    else -> it.totalKills
                }
            }.toMutableList()

            1 -> mobList.sortedByDescending {
                when (config.displayType) {
                    0 -> it.totalKills
                    1 -> it.currentKillToNextLevel
                    2 -> it.killNeededToMax()
                    3 -> it.killNeededToNextLevel()
                    else -> it.totalKills
                }
            }.toMutableList()
            else -> mobList.sortedBy {
                when (config.displayType) {
                    0 -> it.totalKills
                    1 -> it.killNeededForNextLevel
                    else -> it.totalKills
                }
            }.toMutableList()
        }

        if (sortedMobList.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Bestiary Data")
            for (mob in sortedMobList) {
                val isUnlocked = mob.totalKills != 0.toLong()
                val isMaxed = mob.percentToMax() == 100.0
                if (isUnlocked) {
                    if (isMaxed && config.hideMaxed) continue
                    newDisplay.add(buildList {
                        val displayType = config.displayType
                        var text = ""
                        //add("  §7- ")
                        text += " §7- "
                        text += "${mob.name} "
                        if (displayType == 0 || displayType == 1) {
                            val currentKill = when (displayType) {
                                0 -> mob.totalKills
                                1 -> mob.currentKillToNextLevel
                                else -> 0
                            }
                            val killNeeded = when (displayType) {
                                0 -> mob.killToMax
                                1 -> mob.killNeededForNextLevel
                                else -> 0
                            }
                            text += if (isMaxed) {
                                "§c§lMAXED!"
                            } else {
                                val curr = if (currentKill > killNeeded) killNeeded else currentKill
                                //"§7(§b${currentKill.formatNumber()}§7/§b${killNeeded.formatNumber()}§7) §a${((curr.toDouble() / killNeeded) * 100).roundToPrecision(2)}§6%"
                                "§7(§b${currentKill.formatNumber()}§7/§b${killNeeded.formatNumber()}§7) §a${((curr.toDouble() / killNeeded) * 100).roundToPrecision(2)}§6% ${if (displayType == 1) "§ato level ${mob.getNextLevel()}" else ""}"
                            }


                            /* add(if (isMaxed) "§c§lMAXED!" else {
                                 val curr = if (currentKill > killNeeded) killNeeded else currentKill
                                 "§7(§b${currentKill.formatNumber()}§7/§b${killNeeded.formatNumber()}§7) §a${((curr.toDouble() / killNeeded) * 100).roundToPrecision(2)}§6%"
                                 text += ""
                             })*/

                        } else if (displayType == 2 || displayType == 3) {
                            val l = when (displayType) {
                                2 -> if (mob.killNeededToMax() > 0) "§b${mob.killNeededToMax()} kills needed to max level" else "§c§lMAXED!"
                                3 -> if (mob.killNeededToNextLevel() > 0) "§b${mob.killNeededToNextLevel()} kills needed to level ${mob.getNextLevel()}" else "§c§lMAXED!"
                                else -> "..."
                            }
                            text += l
                        }
                        val rendered = Renderable.hoverTips(text,
                            listOf(
                                "§6Name: §b${mob.name}",
                                "§6Level: §b${mob.level}",
                                "§6Total Kills: §b${mob.actualRealTotalKill.addSeparators()}",
                                "§6Kills needed to max: §b${mob.killNeededToMax().addSeparators()}",
                                "§6Kills needed to next lvl: §b${mob.killNeededToNextLevel().addSeparators()}",
                                "§6Current kill to next level: §b${mob.currentKillToNextLevel.addSeparators()}",
                                "§6Kill needed for next level: §b${mob.killNeededForNextLevel.addSeparators()}",
                                "§6Current kill to max: §b${mob.killToMax.addSeparators()}",
                                "§6Percent to max: §b${mob.percentToMax().addSeparators()}",
                                "§6Percent to tier: §b${mob.percentToTier().addSeparators()}",
                                "",
                                "§7More infos thing"), false) {
                            true
                        }
                        add(rendered)
                    })
                } else {
                    newDisplay.add(buildList {
                        add(" §7- ")
                        add("${mob.name}: §cNot unlocked!")
                    })
                }
            }
            newDisplay.addButton(
                prefix = "§7Number Format: ",
                getName = FormatType.entries[config.numberFormat].type,
                onChange = {
                    config.numberFormat = (config.numberFormat + 1) % 2
                    update()
                })

            newDisplay.addButton(
                prefix = "§7Sorting Type: ",
                getName = SortingType.entries[config.sortingType].type,
                onChange = {
                    config.sortingType = (config.sortingType + 1) % 2
                    update()
                })

            newDisplay.addButtonWithTips(
                prefix = "§7Display Type: ",
                getName = DisplayType.entries[config.displayType].type,
                tips = DisplayType.entries[config.displayType].tips,
                onChange = {
                    config.displayType = (config.displayType + 1) % 4
                    update()
                })
        }
        newDisplay.addButton(
            prefix = "§7Hide Maxed: ",
            getName = HideMaxed.entries[config.hideMaxed.toInt()].b,
            onChange = {
                config.hideMaxed = ((config.hideMaxed.toInt() + 1) % 2).toBoolean()
                update()
            })

        return newDisplay
    }

    private fun isBestiaryGui(stack: ItemStack?, name: String): Boolean {
        if (stack == null) return false
        val bestiaryGuiTitleMatcher = titlePattern.matcher(name)
        if (bestiaryGuiTitleMatcher.matches()) {
            if ("Bestiary" != bestiaryGuiTitleMatcher.group(1)) {
                var loreContainsFamiliesFound = false
                for (line in stack.getLore()) {
                    if (line.removeColor().startsWith("Families Found")) {
                        loreContainsFamiliesFound = true
                        break
                    }
                }
                if (!loreContainsFamiliesFound) {
                    return false
                }
            }
            return true
        } else if (name == "Search Results") {
            val loreList = stack.getLore()
            if (loreList.size >= 2 && loreList[0].startsWith("§7Query: §a")
                && loreList[1].startsWith("§7Results: §a")) {
                return true
            }
        }
        return false
    }

    enum class FormatType(val type: String) {
        SHORT("Short"),
        LONG("Long")
    }

    enum class SortingType(val type: String) {
        LOWEST("Lowest"),
        HIGHEST("Highest"),
    }

    enum class DisplayType(val type: String, val tips: List<String>) {
        MAX("To Maxed", listOf("§6Show progress to max the bestiary.","§bFormat: - Ghost (100/200) 50.00%")),
        TIER("To Next Tier", listOf("§6Show progress to the next level.","§bFormat: - Ghost (100/200) 50.00% to level X")),
        FEWEST_MAX("Kills to max", listOf("§6Show needed kills to max the bestiary.","§bFormat: - Ghost 100 kills needed to max")),
        FEWEST_TIER("Kills to next tier", listOf("§6Show needed kills to reach the next level.","§bFormat: - Ghost 10 kills needed for level X"))
    }

    enum class HideMaxed(val b: String) {
        NO("Show"),
        YES("Hide")
    }

    private fun Long.formatNumber(): String = when (config.numberFormat) {
        0 -> NumberUtil.format(this)
        1 -> this.addSeparators()
        else -> "0"
    }

    private fun Int.toBoolean() = this != 0
    private fun Boolean.toInt() = if (!this) 0 else 1

    data class BestiaryMob(var name: String,
                           var level: String,
                           var killToMax: Long,
                           var totalKills: Long,
                           var killNeededForNextLevel: Long,
                           var currentKillToNextLevel: Long,
                           var actualRealTotalKill: Long) {

        fun killNeededToMax(): Long {
            return 0L.coerceAtLeast(killToMax - totalKills)
        }

        fun killNeededToNextLevel(): Long {
            return 0L.coerceAtLeast(killNeededForNextLevel - currentKillToNextLevel)
        }

        fun percentToMax(): Double {
            return 100.0.coerceAtMost((totalKills.toDouble() / killToMax) * 100).roundToPrecision(2)
        }

        fun percentToTier(): Double {
            return 100.0.coerceAtMost((currentKillToNextLevel.toDouble() / killNeededForNextLevel) * 100).roundToPrecision(2)
        }

        fun getNextLevel(): String {
            return level.getNextLevel()
        }
    }

    private fun MutableList<List<Any>>.addButton(
        prefix: String,
        getName: String,
        onChange: () -> Unit,
    ) {
        add(buildList {
            add(prefix)
            add("§a[")
            add(Renderable.link("§e$getName") {
                if ((System.currentTimeMillis() - lastclicked) > 100) { //funny thing happen if i don't do that
                    onChange()
                    SoundUtils.playClickSound()
                    lastclicked = System.currentTimeMillis()
                }
            })
            add("§a]")
        })
    }

    private fun MutableList<List<Any>>.addButtonWithTips(
        prefix: String,
        getName: String,
        tips: List<String>,
        onChange: () -> Unit,
    ) {
        add(buildList {
            add(prefix)
            add("§a[")
            add(Renderable.clickAndHover("§e$getName", tips, false) {
                if ((System.currentTimeMillis() - lastclicked) > 100) { //funny thing happen if i don't do that
                    onChange()
                    SoundUtils.playClickSound()
                    lastclicked = System.currentTimeMillis()
                }
            })
            add("§a]")
        })
    }

    fun Any.getNextLevel(): String {
        return when (this) {
            is Int -> {
                (this + 1).toString()
            }

            is String -> {
                if (this == "0") {
                    "I"
                } else {
                    val intValue = Utils.parseRomanNumeral(this)
                    if (intValue != null) {
                        (intValue + 1).toRoman()
                    } else {
                        "Invalid Roman numeral"
                    }
                }
            }

            else -> {
                "Unsupported type: ${this::class.simpleName}"
            }
        }
    }
}