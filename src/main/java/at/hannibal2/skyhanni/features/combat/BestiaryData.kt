package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.combat.BestiaryConfig
import at.hannibal2.skyhanni.config.features.combat.BestiaryConfig.DisplayTypeEntry
import at.hannibal2.skyhanni.config.features.combat.BestiaryConfig.NumberFormatEntry
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addButton
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BestiaryData {

    private val config get() = SkyHanniMod.feature.combat.bestiary
    private var display = emptyList<List<Any>>()
    private val mobList = mutableListOf<BestiaryMob>()
    private val stackList = mutableMapOf<Int, ItemStack>()
    private val catList = mutableListOf<Category>()
    private val progressPattern = "(?<current>[0-9kKmMbB,.]+)/(?<needed>[0-9kKmMbB,.]+$)".toPattern()
    private val titlePattern = "^(?:\\(\\d+/\\d+\\) )?(Bestiary|.+) ➜ (.+)$".toPattern()
    private var inInventory = false
    private var isCategory = false
    private var indexes = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    )

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
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
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                val stack = slot.stack
                val lore = stack.getLore()
                if (lore.any { it == "§7Overall Progress: §b100% §7(§c§lMAX!§7)" || it == "§7Families Completed: §a100%" }) {
                    slot highlight LorenzColor.GREEN
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        val inventoryName = event.inventoryName
        val stack = event.inventoryItems[4] ?: return
        if ((inventoryName == "Bestiary ➜ Fishing" || inventoryName == "Bestiary") || isBestiaryGui(
                stack,
                inventoryName
            )
        ) {
            isCategory = inventoryName == "Bestiary ➜ Fishing" || inventoryName == "Bestiary"
            stackList.putAll(event.inventoryItems)
            inInventory = true
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        mobList.clear()
        stackList.clear()
        inInventory = false
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.bestiaryData", "combat.bestiary")

        event.transform(15, "combat.bestiary.numberFormat") { element ->
            ConfigUtils.migrateIntToEnum(element, NumberFormatEntry::class.java)
        }
        event.transform(15, "combat.bestiary.displayType") { element ->
            ConfigUtils.migrateIntToEnum(element, DisplayTypeEntry::class.java)
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun init() {
        mobList.clear()
        catList.clear()
        if (isCategory) {
            inCategory()
        } else {
            notInCategory()
        }
    }

    private fun inCategory() {
        for ((index, stack) in stackList) {
            if (stack.displayName == " ") continue
            if (!indexes.contains(index)) continue
            inInventory = true
            val name = stack.displayName
            var familiesFound: Long = 0
            var totalFamilies: Long = 0
            var familiesCompleted: Long = 0
            for ((lineIndex, loreLine) in stack.getLore().withIndex()) {
                val line = loreLine.removeColor()
                if (!line.startsWith("                    ")) continue
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
            catList.add(Category(name, familiesFound, totalFamilies, familiesCompleted))
        }
    }

    private fun notInCategory() {
        for ((index, stack) in stackList) {
            if (stack.displayName == " ") continue
            if (!indexes.contains(index)) continue
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
                    actualRealTotalKill =
                        "([0-9,.]+)".toRegex().find(loreLine)?.groupValues?.get(1)?.formatNumber()
                            ?: 0
                }
                if (!loreLine.startsWith("                    ")) continue
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
            mobList.add(
                BestiaryMob(
                    name,
                    level,
                    totalKillToMax,
                    currentTotalKill,
                    totalKillToTier,
                    currentKillToTier,
                    actualRealTotalKill
                )
            )
        }
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        init()

        addCategories(newDisplay)

        if (mobList.isEmpty()) return newDisplay

        addList(newDisplay)

        addButtons(newDisplay)

        return newDisplay
    }

    private fun sortMobList(): MutableList<BestiaryMob> {
        val sortedMobList = when (config.displayType) {
            DisplayTypeEntry.GLOBAL_MAX -> mobList.sortedBy { it.percentToMax() }
            DisplayTypeEntry.GLOBAL_NEXT -> mobList.sortedBy { it.percentToTier() }
            DisplayTypeEntry.LOWEST_TOTAL -> mobList.sortedBy { it.totalKills }
            DisplayTypeEntry.HIGHEST_TOTAL -> mobList.sortedByDescending { it.totalKills }
            DisplayTypeEntry.LOWEST_MAX -> mobList.sortedBy { it.killNeededToMax() }
            DisplayTypeEntry.HIGHEST_MAX -> mobList.sortedByDescending { it.killNeededToMax() }
            DisplayTypeEntry.LOWEST_NEXT -> mobList.sortedBy { it.killNeededToNextLevel() }
            DisplayTypeEntry.HIGHEST_NEXT -> mobList.sortedByDescending { it.killNeededToNextLevel() }
            else -> mobList.sortedBy { it.totalKills }
        }.toMutableList()
        return sortedMobList
    }

    private fun addList(newDisplay: MutableList<List<Any>>) {
        val sortedMobList = sortMobList()

        newDisplay.addAsSingletonList("§7Bestiary Data")
        for (mob in sortedMobList) {
            val isUnlocked = mob.totalKills != 0.toLong()
            val isMaxed = mob.percentToMax() >= 1
            if (!isUnlocked) {
                newDisplay.add(buildList {
                    add(" §7- ")
                    add("${mob.name}: §cNot unlocked!")
                })
                continue
            }
            if (isMaxed && config.hideMaxed) continue
            val text = getMobLine(mob, isMaxed)
            val tips = getMobHover(mob)
            newDisplay.addAsSingletonList(Renderable.hoverTips(text, tips) { true })
        }
    }

    private fun getMobHover(mob: BestiaryMob) = listOf(
        "§6Name: §b${mob.name}",
        "§6Level: §b${mob.level} ${if (!config.replaceRoman) "§7(${mob.level.romanToDecimalIfNecessary()})" else ""}",
        "§6Total Kills: §b${mob.actualRealTotalKill.formatNumber()}",
        "§6Kills needed to max: §b${mob.killNeededToMax().formatNumber()}",
        "§6Kills needed to next lvl: §b${mob.killNeededToNextLevel().formatNumber()}",
        "§6Current kill to next level: §b${mob.currentKillToNextLevel.formatNumber()}",
        "§6Kill needed for next level: §b${mob.killNeededForNextLevel.formatNumber()}",
        "§6Current kill to max: §b${mob.killToMax.formatNumber()}",
        "§6Percent to max: §b${mob.percentToMaxFormatted()}",
        "§6Percent to tier: §b${mob.percentToTierFormatted()}",
        "",
        "§7More info thing"
    )

    private fun getMobLine(
        mob: BestiaryMob,
        isMaxed: Boolean
    ): String {
        val displayType = config.displayType
        var text = ""
        text += " §7- "
        text += "${mob.name} ${mob.level.romanOrInt()} "
        text += if (isMaxed) {
            "§c§lMAXED! §7(§b${mob.actualRealTotalKill.formatNumber()}§7 kills)"
        } else {
            when (displayType) {
                DisplayTypeEntry.GLOBAL_MAX, DisplayTypeEntry.GLOBAL_NEXT -> {
                    val currentKill = when (displayType) {
                        DisplayTypeEntry.GLOBAL_MAX -> mob.totalKills
                        DisplayTypeEntry.GLOBAL_NEXT -> mob.currentKillToNextLevel
                        else -> 0
                    }
                    val killNeeded = when (displayType) {
                        DisplayTypeEntry.GLOBAL_MAX -> mob.killToMax
                        DisplayTypeEntry.GLOBAL_NEXT -> mob.killNeededForNextLevel
                        else -> 0
                    }
                    "§7(§b${currentKill.formatNumber()}§7/§b${killNeeded.formatNumber()}§7) §a${
                        ((currentKill.toDouble() / killNeeded) * 100).roundToPrecision(
                            2
                        )
                    }§6% ${if (displayType == DisplayTypeEntry.GLOBAL_NEXT) "§ato level ${mob.getNextLevel()}" else ""}"
                }

                DisplayTypeEntry.LOWEST_TOTAL, DisplayTypeEntry.HIGHEST_TOTAL -> {

                    "§6${mob.totalKills.formatNumber()} §7total kills"
                }

                DisplayTypeEntry.LOWEST_MAX, DisplayTypeEntry.HIGHEST_MAX -> {
                    "§6${mob.killNeededToMax().formatNumber()} §7kills needed"
                }

                DisplayTypeEntry.LOWEST_NEXT, DisplayTypeEntry.HIGHEST_NEXT -> {
                    "§6${mob.killNeededToNextLevel().formatNumber()} §7kills needed"
                }

                else -> "§cYou are not supposed to see this, please report it to @HiZe on discord!"
            }
        }
        return text
    }

    private fun addButtons(newDisplay: MutableList<List<Any>>) {
        newDisplay.addButton(
            prefix = "§7Number Format: ",
            getName = FormatType.entries[config.numberFormat.ordinal].type, // todo: avoid ordinal
            onChange = {
                // todo: avoid ordinal
                config.numberFormat = BestiaryConfig.NumberFormatEntry.entries[(config.numberFormat.ordinal + 1) % 2]
                update()
            })

        newDisplay.addButton(
            prefix = "§7Display Type: ",
            getName = DisplayType.entries[config.displayType.ordinal].type, // todo: avoid ordinal
            onChange = {
                // todo: avoid ordinal
                config.displayType = DisplayTypeEntry.entries[(config.displayType.ordinal + 1) % 8]
                update()
            })

        newDisplay.addButton(
            prefix = "§7Number Type: ",
            getName = NumberType.entries[if (config.replaceRoman) 0 else 1].type,
            onChange = {
                config.replaceRoman = !config.replaceRoman
                update()
            }
        )
        newDisplay.addButton(
            prefix = "§7Hide Maxed: ",
            getName = HideMaxed.entries[if (config.hideMaxed) 1 else 0].type,
            onChange = {
                config.hideMaxed = !config.hideMaxed
                update()
            }
        )
    }

    private fun addCategories(newDisplay: MutableList<List<Any>>) {
        if (catList.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Category")
            for (cat in catList) {
                newDisplay.add(buildList {
                    add(" §7- ${cat.name}§7: ")
                    val element = when {
                        cat.familiesCompleted == cat.totalFamilies -> "§c§lCompleted!"
                        cat.familiesFound == cat.totalFamilies -> "§b${cat.familiesCompleted}§7/§b${cat.totalFamilies} §7completed"
                        cat.familiesFound < cat.totalFamilies ->
                            "§b${cat.familiesFound}§7/§b${cat.totalFamilies} §7found, §b${cat.familiesCompleted}§7/§b${cat.totalFamilies} §7completed"

                        else -> continue
                    }
                    add(element)
                })
            }
        }
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
                && loreList[1].startsWith("§7Results: §a")
            ) {
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

    enum class DisplayType(val type: String) {
        GLOBAL_MAX("Global display (to max)"),
        GLOBAL_TIER("Global display (to next tier)"),
        LOWEST_TOTAL("Lowest total kills"),
        HIGHEST_TOTAL("Highest total kills"),
        LOWEST_NEEDED_MAX("Lowest kills needed to max"),
        HIGHEST_NEEDED_MAX("Highest kills needed to max"),
        LOWEST_NEEDED_TIER("Lowest kills needed to next tier"),
        HIGHEST_NEEDED_TIER("Highest kills needed to next tier"),
    }

    enum class HideMaxed(val type: String) {
        NO("Show"),
        YES("Hide")
    }

    private fun Long.formatNumber(): String = when (config.numberFormat) {
        BestiaryConfig.NumberFormatEntry.SHORT -> NumberUtil.format(this)
        BestiaryConfig.NumberFormatEntry.LONG -> this.addSeparators()
        else -> "0"
    }

    data class Category(
        val name: String,
        val familiesFound: Long,
        val totalFamilies: Long,
        val familiesCompleted: Long
    )

    data class BestiaryMob(
        var name: String,
        var level: String,
        var killToMax: Long,
        var totalKills: Long,
        var killNeededForNextLevel: Long,
        var currentKillToNextLevel: Long,
        var actualRealTotalKill: Long
    ) {

        fun killNeededToMax(): Long {
            return 0L.coerceAtLeast(killToMax - totalKills)
        }

        fun killNeededToNextLevel(): Long {
            return 0L.coerceAtLeast(killNeededForNextLevel - currentKillToNextLevel)
        }

        fun percentToMax() = totalKills.toDouble() / killToMax

        fun percentToMaxFormatted() = LorenzUtils.formatPercentage(percentToMax())

        fun percentToTier() =
            if (killNeededForNextLevel == 0L) 1.0 else currentKillToNextLevel.toDouble() / killNeededForNextLevel

        fun percentToTierFormatted() = LorenzUtils.formatPercentage(percentToTier())

        fun getNextLevel() = level.getNextLevel()
    }

    private fun String.romanOrInt() = romanToDecimalIfNecessary().let {
        if (config.replaceRoman || it == 0) it.toString() else it.toRoman()
    }

    private fun String.getNextLevel() =
        if (this == "0") {
            "I".romanOrInt()
        } else {
            val intValue = romanToDecimalIfNecessary()
            (intValue + 1).toRoman().romanOrInt()
        }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

}
