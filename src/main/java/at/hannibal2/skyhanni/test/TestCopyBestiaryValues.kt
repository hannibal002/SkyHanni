package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeWordsAtEnd
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose

@SkyHanniModule
object TestCopyBestiaryValues {

    // <editor-fold desc="Brackets">

    private val brackets = mapOf(
        1 to intArrayOf(
            20,
            40,
            60,
            100,
            200,
            400,
            800,
            1400,
            2000,
            3000,
            6000,
            12000,
            20000,
            30000,
            40000,
            50000,
            60000,
            72000,
            86000,
            100000,
            200000,
            400000,
            600000,
            800000,
            1000000,
        ),
        2 to intArrayOf(
            5,
            10,
            15,
            25,
            50,
            100,
            200,
            350,
            500,
            750,
            1500,
            3000,
            5000,
            7500,
            10000,
            12500,
            15000,
            18000,
            21500,
            25000,
            50000,
            100000,
            150000,
            200000,
            250000,
        ),
        3 to intArrayOf(
            4,
            8,
            12,
            16,
            20,
            40,
            80,
            140,
            200,
            300,
            600,
            1200,
            2000,
            3000,
            4000,
            5000,
            6000,
            7200,
            8600,
            10000,
            20000,
            40000,
            60000,
            80000,
            100000,
        ),
        4 to intArrayOf(
            2,
            4,
            6,
            10,
            15,
            20,
            25,
            35,
            50,
            75,
            150,
            300,
            500,
            750,
            1000,
            1350,
            1650,
            2000,
            2500,
            3000,
            5000,
            10000,
            15000,
            20000,
            25000,
        ),
        5 to intArrayOf(1, 2, 3, 5, 7, 10, 15, 20, 25, 30, 60, 120, 200, 300, 400, 500, 600, 720, 860, 1000, 2000, 4000, 6000, 8000, 10000),
        6 to intArrayOf(1, 2, 3, 5, 7, 9, 14, 17, 21, 25, 50, 80, 125, 175, 250, 325, 425, 525, 625, 750, 1500, 3000, 4500, 6000, 7500),
        7 to intArrayOf(1, 2, 3, 5, 7, 9, 11, 14, 17, 20, 30, 40, 55, 75, 100, 150, 200, 275, 375, 500, 1000, 1500, 2000, 2500, 3000),
        8 to intArrayOf(1, 2, 3, 4, 5, 8, 11, 14, 17, 20, 25, 30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200),
    )

    private val critterBrackets = mapOf(
        1 to intArrayOf(1, 5, 10, 20, 35, 50, 65, 85, 105, 125),
        2 to intArrayOf(1, 5, 10, 15, 25, 40, 55, 70, 85, 100),
        3 to intArrayOf(1, 5, 10, 15, 20, 25, 35, 45, 60, 75),
        4 to intArrayOf(1, 3, 6, 10, 15, 20, 25, 30, 40, 50),
        5 to intArrayOf(1, 2, 3, 4, 6, 8, 10, 15, 20, 25),
    )

    // </editor-fold>

    class BestiaryObject { // TODO fix typo

        @Expose
        var name: String = ""

        @Expose
        var skullOwner: String = ""

        @Expose
        var texture: String = ""

        @Expose
        var cap: Int = 0

        @Expose
        var mobs: Array<String> = emptyArray()

        @Expose
        var bracketType: String? = null

        @Expose
        var bracket: Int = 0
    }

    // TODO add regex test
    @Suppress("RepoPatternRegexTestMissing")
    private val bestiaryTypePattern by RepoPattern.pattern(
        "test.bestiary.type",
        "\\[Lv(?<lvl>.*)] (?<text>.*)",
    )

    private fun findBracket(rawCap: Int, capTier: Int, isCritter: Boolean): Int {
        val map = if (isCritter) critterBrackets else brackets
        val index = capTier - 1
        for ((bracketNum, arr) in map) {
            if (index in arr.indices && arr[index] == rawCap) {
                return bracketNum
            }
        }
        ChatUtils.chat("no bracket found for rawCap=$rawCap, capTier=$capTier, isCritter=$isCritter")
        return 0
    }

    @HandleEvent(priority = HandleEvent.LOW)
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!DevApi.config.debug.copyBestiaryData) return
        SkyHanniDebugsAndTests.displayLine = null

        val backItem = event.inventoryItems[3 + 9 * 5 + 3] ?: return
        if (backItem.getLore().none { it.contains("Bestiary Milestone") }) {
            return
        }

        val rankingItem = event.inventoryItems[3 + 9 * 5 + 2] ?: return
        if (rankingItem.getLore().none { it.contains("Ranking") }) {
            return
        }

        val titleItem = event.inventoryItems[4] ?: return
        copy(titleItem, event.inventoryItems)
    }

    private fun copy(titleItem: SafeItemStack, inventoryItems: Map<Int, SafeItemStack>) {
        val titleName = titleItem.hoverName.formattedTextCompatLeadingWhiteLessResets().removeWordsAtEnd(1)

        val obj = BestiaryObject()
        obj.name = titleName
        obj.texture = titleItem.getSkullTexture() ?: "no texture found"
        obj.skullOwner = titleItem.getSkullOwner() ?: "no skullOwner found"

        val lore = titleItem.getLore()
        val overallProgress = lore.find { it.contains("Overall Progress") }
        if (overallProgress == null) {
            println("overallProgress not found!")
            return
        }
        val capLine = lore.nextAfter(overallProgress) ?: return
        val rawCap = capLine.substringAfter("/").removeColor().formatInt()
        obj.cap = rawCap

        val mobs = mutableListOf<String>()
        for (i in 10..43) {
            val stack = inventoryItems[i] ?: continue
            bestiaryTypePattern.matchMatcher(stack.cleanName) {
                val lvl = group("lvl").formatInt()
                var text = group("text").lowercase().replace(" ", "_")

                val master = text.endsWith("(master)")
                val masterText = if (master) "master_" else ""
                if (master) {
                    text = text.split("_").dropLast(1).joinToString("_")
                }
                val result = "$masterText${text}_$lvl"
                mobs.add(result)
            }
        }
        obj.mobs = mobs.toTypedArray()

        if (lore.any { it.contains("Critter") }) {
            obj.bracketType = "CRITTERS"
        }

        val capTier = if (overallProgress.contains("100%")) {
            titleItem.hoverName.string.substringAfterLast(" ").romanToDecimal()
        } else {
            lore.firstOrNull { it.contains("Capped at Tier") }
                ?.substringAfter("Capped at Tier ")
                ?.toInt() ?: 0
        }

        if (capTier == 0) {
            ChatUtils.chat("§cNo capTier found for $titleName, bracket will not be set!")
        } else {
            obj.bracket = findBracket(rawCap, capTier, obj.bracketType == "CRITTERS")
        }

        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        val text = gson.toJson(obj)
        OSUtils.copyToClipboard(text)

        SkyHanniDebugsAndTests.displayLine = "Bestiary for $titleName"
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.copyBestiaryData", "dev.debug.copyBestiaryData")
    }
}
