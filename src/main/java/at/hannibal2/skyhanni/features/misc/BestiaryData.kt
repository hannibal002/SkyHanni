package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
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
    private val catList = mutableListOf<Category>()
    private val progressPattern = "(?<current>[0-9kKmMbB,.]+)/(?<needed>[0-9kKmMbB,.]+$)".toPattern()
    private val titlePattern = "^(?:\\(\\d+/\\d+\\) )?(Bestiary|.+) ➜ (.+)$".toPattern()
    private var lastclicked = 0L
    private var inInventory = false
    private var isCategory = false
    private var indexes = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    )

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (!isEnabled()) return
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
        if (!isEnabled()) return
        if (inInventory) {
            val inventoryName = InventoryUtils.openInventoryName()
            if (isBestiaryGui(InventoryUtils.getItemsInOpenChest()[4].stack, inventoryName)) {
                for (slot in InventoryUtils.getItemsInOpenChest()) {
                    val stack = slot.stack
                    val lore = stack.getLore()
                    if (lore.any { it == "§7Overall Progress: §b100% §7(§c§lMAX!§7)" || it == "§7Families Completed: §a100§6% §7(§c§lMAX!§7)" }) {
                        slot highlight LorenzColor.GREEN
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        val inventoryName = event.inventoryName
        if ((inventoryName == "Bestiary ➜ Fishing" || inventoryName == "Bestiary") || isBestiaryGui(event.inventoryItems[4], inventoryName)) {
            isCategory = inventoryName == "Bestiary ➜ Fishing" || inventoryName == "Bestiary"
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
        catList.clear()
        if (isCategory) {
            for ((index, stack) in stackList) {
                if (stack.displayName == " ") continue
                if (indexes.contains(index)) {
                    inInventory = true
                    val name = stack.displayName
                    var familiesFound: Long = 0
                    var totalFamilies: Long = 0
                    var familiesCompleted: Long = 0
                    for ((lineIndex, loreLine) in stack.getLore().withIndex()) {
                        val line = loreLine.removeColor()
                        if (line.startsWith("                    ")) {
                            val previousLine = stack.getLore()[lineIndex - 1]
                            val progress = line.substring(line.lastIndexOf(' ') + 1)
                            if (previousLine.contains("Families Found")) {
                                progressPattern.matchMatcher(progress) {
                                    familiesFound = group("current").formatNumber()
                                    totalFamilies = group("needed").formatNumber()
                                }
                            } else if (previousLine.contains("Families Completed")) {
                                progressPattern.matchMatcher(progress) {
                                    familiesCompleted = group("current").formatNumber()
                                }
                            }
                        }
                    }
                    catList.add(Category(name, familiesFound, totalFamilies, familiesCompleted))
                }
            }
        } else {
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
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        init()

        if (catList.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Category")
            for (cat in catList) {
                newDisplay.add(buildList {
                    add(" §7- ${cat.name}§7: ")
                    if (cat.familiesCompleted == cat.totalFamilies){
                        add("§c§lCompleted!")
                    }else if(cat.familiesFound == cat.totalFamilies){
                        add("§b${cat.familiesCompleted}§7/§b${cat.totalFamilies} §7completed")
                    }else if(cat.familiesFound < cat.totalFamilies){
                        add("§b${cat.familiesFound}§7/§b${cat.totalFamilies} §7found, §b${cat.familiesCompleted}§7/§b${cat.totalFamilies} §7completed")
                    }
                })
            }
        }

        if (mobList.isNotEmpty()) {
            val sortedMobList = when (config.displayType) {
                0 -> mobList.sortedBy { it.percentToMax() }
                1 -> mobList.sortedBy { it.percentToTier() }
                2 -> mobList.sortedBy { it.totalKills }
                3 -> mobList.sortedByDescending { it.totalKills }
                4 -> mobList.sortedBy { it.killNeededToMax() }
                5 -> mobList.sortedByDescending { it.killNeededToMax() }
                6 -> mobList.sortedBy { it.killNeededToNextLevel() }
                7 -> mobList.sortedByDescending { it.killNeededToNextLevel() }
                else -> mobList.sortedBy { it.totalKills }
            }.toMutableList()
            newDisplay.addAsSingletonList("§7Bestiary Data")
            for (mob in sortedMobList) {
                val isUnlocked = mob.totalKills != 0.toLong()
                val isMaxed = mob.percentToMax() == 100.0
                if (isUnlocked) {
                    if (isMaxed && config.hideMaxed) continue
                    newDisplay.add(buildList {
                        val displayType = config.displayType
                        var text = ""
                        text += " §7- "
                        text += "${mob.name} ${mob.level.romanOrInt()} "
                        text += if (isMaxed) {
                            "§c§lMAXED! §7(§b${mob.actualRealTotalKill.addSeparators()}§7 kills)"
                        } else {
                            when (displayType) {
                                0, 1 -> {
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
                                    "§7(§b${currentKill.formatNumber()}§7/§b${killNeeded.formatNumber()}§7) §a${((currentKill.toDouble() / killNeeded) * 100).roundToPrecision(2)}§6% ${if (displayType == 1) "§ato level ${mob.getNextLevel()}" else ""}"
                                }

                                2, 3 -> {

                                    "§6${mob.totalKills.formatNumber()} §7total kills"
                                }

                                4, 5 -> {
                                    "§6${mob.killNeededToMax().formatNumber()} §7kills needed"
                                }

                                6, 7 -> {
                                    "§6${mob.killNeededToNextLevel().formatNumber()} §7kills needed"
                                }

                                else -> "§cYou are not supposed to see this, please report it to @HiZe on discord!"
                            }
                        }
                        val rendered = Renderable.hoverTips(text,
                            listOf(
                                "§6Name: §b${mob.name}",
                                "§6Level: §b${mob.level} ${if (!config.replaceRoman) "§7(${Utils.parseRomanNumeral(mob.level)})" else ""}",
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

            newDisplay.addButtonWithTips(
                prefix = "§7Display Type: ",
                getName = Type.entries[config.displayType].type,
                tips = Type.entries[config.displayType].tips,
                onChange = {
                    config.displayType = (config.displayType + 1) % 8
                    update()
                })

            newDisplay.addButton(
                prefix = "§7Number Type: ",
                getName = NumberType.entries[config.replaceRoman.toInt()].type,
                onChange = {
                    config.replaceRoman = ((config.replaceRoman.toInt() + 1) % 2).toBoolean()
                    update()
                }
            )
            newDisplay.addButton(
                prefix = "§7Hide Maxed: ",
                getName = HideMaxed.entries[config.hideMaxed.toInt()].b,
                onChange = {
                    config.hideMaxed = ((config.hideMaxed.toInt() + 1) % 2).toBoolean()
                    update()
                })
        }

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

    enum class NumberType(val type: String) {
        INT("Normal (1, 2, 3)"),
        ROMAN("Roman (I, II, III")
    }

    enum class Type(val type: String, val tips: List<String>) {
        GLOBAL_MAX("Global display (to max)", listOf()),
        GLOBAL_TIER("Global display (to next tier)", listOf()),
        LOWEST_TOTAL("Lowest total kills", listOf()),
        HIGHEST_TOTAL("Highest total kills", listOf()),
        LOWEST_NEEDED_MAX("Lowest kills needed to max", listOf()),
        HIGHEST_NEEDED_MAX("Highest kills needed to max", listOf()),
        LOWEST_NEEDED_TIER("Lowest kills needed to next tier", listOf()),
        HIGHEST_NEEDED_TIER("Highest kills needed to next tier", listOf()),
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

    data class Category(val name: String,
                        val familiesFound: Long,
                        val totalFamilies: Long,
                        val familiesCompleted: Long)

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

    fun String.romanOrInt(): String = if (!config.replaceRoman && isRoman()) Utils.parseRomanNumeral(this).toString() else this

    fun String.isRoman(): Boolean {
        return Utils.parseRomanNumeral(this) != null
    }

    fun Any.getNextLevel(): String {
        return when (this) {
            is Int -> {
                (this + 1).toString().romanOrInt()
            }

            is String -> {
                if (this == "0") {
                    "I".romanOrInt()
                } else {
                    val intValue = Utils.parseRomanNumeral(this)
                    if (intValue != null) {
                        (intValue + 1).toRoman().romanOrInt()
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

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

}