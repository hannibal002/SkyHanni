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
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


class BestiaryData {

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
                val name = stack.displayName
                var totalKillToMax: Long = 0
                var currentTotalKill: Long = 0
                var totalKillToTier: Long = 0
                var currentKillToTier: Long = 0

                for ((lineIndex, line) in stack.getLore().withIndex()) {
                    val loreLine = line.removeColor()
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
                mobList.add(BestiaryMob(name, totalKillToMax, currentTotalKill, totalKillToTier, currentKillToTier))
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
                    else -> it.totalKills
                }
            }.toMutableList()

            1 -> mobList.sortedByDescending {
                when (config.displayType) {
                    0 -> it.totalKills
                    1 -> it.currentKillToNextLevel
                    else -> it.totalKills
                }
            }.toMutableList()

            2 -> mobList.sortedBy {
                when (config.displayType) {
                    0 -> it.percentToMax()
                    1 -> it.percentToTier()
                    else -> it.percentToMax()
                }
            }.toMutableList()

            3 -> mobList.sortedByDescending {
                when (config.displayType) {
                    0 -> it.percentToMax()
                    1 -> it.percentToTier()
                    else -> it.percentToMax()
                }
            }.toMutableList()

            4 -> mobList.sortedBy {
                when (config.displayType) {
                    0 -> it.killNeededToMax()
                    1 -> it.killNeededToNextLevel()
                    else -> it.killNeededToMax()
                }
            }.toMutableList()

            5 -> mobList.sortedByDescending {
                when (config.displayType) {
                    0 -> it.killNeededToMax()
                    1 -> it.killNeededToNextLevel()
                    else -> it.killNeededToMax()
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
                        add("  §7- ")
                        add("${mob.name} ")
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
                            add(if (isMaxed) "§c§lMAXED!" else {
                                val curr = if (currentKill > killNeeded) killNeeded else currentKill
                                "§7(§b${currentKill.formatNumber()}§7/§b${killNeeded.formatNumber()}§7) §a${((curr.toDouble() / killNeeded) * 100).roundToPrecision(2)}§6%"
                            })

                        } else if (displayType == 2 || displayType == 3) {
                            val l = when (displayType) {
                                2 -> "§b${mob.killNeededToMax()} kills needed to max"
                                3 -> "§b${mob.killNeededToNextLevel()} kills needed to next tier"
                                else -> "..."
                            }
                            add(l)
                        }
                    })
                } else {
                    newDisplay.add(buildList {
                        add("  §7- ")
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
                    config.sortingType = (config.sortingType + 1) % 4
                    update()
                })

            newDisplay.addButton(
                prefix = "§7Display Type: ",
                getName = DisplayType.entries[config.displayType].type,
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
        LOWEST_KILLS("Lowest Kills"),
        HIGHEST_KILLS("Highest Kills"),
        LOWEST_PERCENT("Lowest %"),
        HIGHEST_PERCENT("Highest %"),
        LOWEST_NEEDED_KILL("Lowest Kills Needed"),
        HIGHEST_NEEDED_KILL("Highest Kills Needed")
    }

    enum class DisplayType(val type: String) {
        MAX("To Maxed"),
        TIER("To Next Tier"),
        FEWEST_MAX("Fewest kills to max"),
        FEWEST_TIER("Fewest kills to next tier")
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
                           var killToMax: Long,
                           var totalKills: Long,
                           var killNeededForNextLevel: Long,
                           var currentKillToNextLevel: Long) {

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
    }

    private fun MutableList<List<Any>>.addButton(
        prefix: String,
        getName: String,
        onChange: () -> Unit,
    ) {
        val newList = mutableListOf<Any>()
        newList.add(prefix)
        newList.add("§a[")
        newList.add(Renderable.link("§e$getName") {
            if ((System.currentTimeMillis() - lastclicked) > 100) {
                onChange()
                SoundUtils.playClickSound()
                lastclicked = System.currentTimeMillis()
            }
        })
        newList.add("§a]")
        add(newList)
    }
}