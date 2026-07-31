package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items

@SkyHanniModule
object BestiaryApi {

    private val patternGroup = RepoPattern.group("combat.bestiary.data")

    /**
     * REGEX-TEST: Progress to Tier 14: 26%
     * REGEX-TEST: Progress to Tier XV: 57.1%
     */
    private val tierProgressPattern by patternGroup.pattern(
        "tierprogress.colorless",
        "Progress to Tier [\\dIVXC]+: [\\d.]+%",
    )

    /**
     * REGEX-TEST: Overall Progress: 55.2%
     * REGEX-TEST: Overall Progress: 100% (MAX!)
     */
    private val overallProgressPattern by patternGroup.pattern(
        "overallprogress.colorless",
        "Overall Progress: [\\d.]+%(?: \\(MAX!\\))?",
    )

    /**
     * REGEX-TEST: 9/10
     * REGEX-TEST: 6/6
     */
    private val progressPattern by patternGroup.pattern(
        "progress.colorless",
        "(?<current>[0-9kKmMbB,.]+)/(?<needed>[0-9kKmMbB,.]+\$)",
    )

    /**
     * REGEX-TEST: (1/2) Bestiary ➜ The Catacombs
     * REGEX-TEST: Bestiary ➜ Dwarven Mines
     * REGEX-TEST: Bestiary ➜ Spider's Den
     * REGEX-TEST: Fishing ➜ Spooky
     */
    private val inventoryTitlePattern by patternGroup.pattern(
        "title.colorless",
        "^(?:\\(\\d+/\\d+\\) )?(?<parent>[^➜]+) ➜ (?<category>.+)\$",
    )

    /**
     * REGEX-TEST: Search Results
     */
    private val inventorySearchResultsPattern by patternGroup.pattern(
        "title.search-results.colorless",
        "^Search Results$",
    )

    /**
     * REGEX-TEST: Bestiary
     * REGEX-TEST: Bestiary ➜ Fishing
     * REGEX-TEST: Bestiary ➜ Critter Safari
     */
    private val inventoryCategoryOfCategoryPattern by patternGroup.list(
        "category-of-category.colorless",
        "^Bestiary$",
        "^Bestiary ➜ Fishing$",
        "^Bestiary ➜ Critter Safari$",
    )

    /**
     * REGEX-TEST: Cave Spider
     * REGEX-TEST: Cave Spider IV
     * REGEX-TEST: Cave Spider 5
     * REGEX-TEST: Blobfish XIV
     */
    private val mobLevelPattern by patternGroup.pattern(
        "mob.level.colorless",
        "^(?<name>.+?)(?: (?<level>[IVX0-9]+))?\$",
    )

    /**
     * REGEX-TEST: Kills: 1,234
     * REGEX-TEST: Kills: 9,876,543
     */
    private val killsLinePattern by patternGroup.pattern(
        "kills.line.colorless",
        "Kills: (?<kills>[0-9,.]+)",
    )

    /**
     * REGEX-TEST: [Lv40] Endermite
     * REGEX-TEST: [Lv37] Endermite
     * REGEX-TEST: [Lv50] Nest Enderman
     */
    private val mobVariantPattern by patternGroup.pattern(
        "mob.variant.colorless",
        """^\[Lv\d+] (?<name>.+)""",
    )

    /**
     * WRAPPED-REGEX-TEST: "                    9/10"
     */
    private val progressBarLinePattern by patternGroup.pattern(
        "progress.bar.line",
        " {20}.*",
    )

    /**
     * REGEX-TEST: You haven't unlocked this Family yet!
     */
    private val notUnlockedFamilyPattern by patternGroup.pattern(
        "progress.not-unlocked-family.colorless",
        "You haven't unlocked this Family yet!",
    )

    /**
     * REGEX-TEST: Overall Progress: SHOWN
     */
    private val overallProgressShownPattern by patternGroup.pattern(
        "progress.overall-shown",
        "Overall Progress: SHOWN",
    )

    /**
     * REGEX-TEST: Families Found 9/10
     */
    private val familyFoundPattern by patternGroup.pattern(
        "family.found.colorless",
        "\\s*Families Found.*",
    )

    /**
     * REGEX-TEST: Families Completed 6/10
     */
    private val familyCompletedPattern by patternGroup.pattern(
        "family.completed.colorless",
        "\\s*Families Completed.*",
    )

    val indexes = listOf(
        10..16,
        19..25,
        28..34,
        37..43,
    ).flatten()

    const val OVERALL_PROGRESS_SLOT = 52

    private val stackList = mutableMapOf<Int, SafeItemStack>()
    val mobList = mutableListOf<BestiaryMob>()
    val catList = mutableListOf<Category>()
    val mobVariants = mutableListOf<BestiaryMobVariant>()

    var inInventory = false
        private set
    var overallProgressEnabled = false
        private set
    var currentCategory: Category? = null
        private set

    // Inventory GUI states
    var isCategoryOfCategories = false
        private set
    var isCategoryOfMobs = false
        private set
    var isMobVariants = false
        private set
    var currentFamily: BestiaryMob? = null
        private set

    private var pendingCategory: Category? = null
    private var pendingFamily: BestiaryMob? = null

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        val items = event.inventoryItems
        val stack = items[4] ?: return

        if (!isBestiaryGui(stack, inventoryName) && !inventoryCategoryOfCategoryPattern.matches(inventoryName)) return

        val oldCategory = currentCategory?.copy(slot = null)
        val oldFamily = currentFamily?.copy(slot = null)

        stackList.clear()
        stackList.putAll(items)
        inInventory = true
        overallProgressEnabled = isOverallProgressEnabled(items)
        currentCategory = null

        isCategoryOfCategories = false
        isCategoryOfMobs = false
        isMobVariants = false
        currentFamily = null

        val cleanLore = stack.getCleanLore()
        val hasFamilies = cleanLore.any { familyFoundPattern.matches(it) || familyCompletedPattern.matches(it) }
        val hasMobFamily = parseStackName(stack.hoverName) != null

        if (inventoryCategoryOfCategoryPattern.matches(inventoryName)) {
            isCategoryOfCategories = true
            inCategoryOfCategories(inventoryName)
        } else if (hasFamilies || inventorySearchResultsPattern.matches(inventoryName)) {
            isCategoryOfMobs = true
            inCategoryOfMobs()
        } else if (hasMobFamily) {
            isMobVariants = true
            inMobVariants()
        }

        if (isCategoryOfMobs) {
            currentCategory = pendingCategory ?: oldCategory
            pendingCategory = null
        } else if (isMobVariants) {
            currentCategory = oldCategory
            pendingCategory = null
        } else {
            pendingCategory = null
        }

        if (isMobVariants) {
            currentFamily = pendingFamily ?: oldFamily
            pendingFamily = null
        } else {
            pendingFamily = null
        }
    }

    @HandleEvent
    private fun onInventoryClose() {
        stackList.clear()
        mobList.clear()
        catList.clear()
        mobVariants.clear()
        inInventory = false
        isCategoryOfCategories = false
        isCategoryOfMobs = false
        isMobVariants = false
    }

    @HandleEvent
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        when {
            isCategoryOfCategories -> {
                pendingCategory = catList.firstOrNull { it.slot == event.slotId }
            }

            isCategoryOfMobs -> {
                pendingFamily = mobList.firstOrNull { it.slot == event.slotId }
            }
        }
    }

    private fun parseCategoryFromStack(
        name: Component,
        fullName: String,
        stack: SafeItemStack,
        slot: Int?,
    ): Category {
        var familiesFound: Long = 0
        var totalFamilies: Long = 0
        var familiesCompleted: Long = 0

        val cleanLore = stack.getCleanLore()
        for ((lineIndex, line) in cleanLore.withIndex()) {
            if (!line.startsWith("                    ")) continue
            if (lineIndex == 0) continue
            val previousLine = cleanLore[lineIndex - 1]
            val progress = line.substring(line.lastIndexOf(' ') + 1)
            if (familyFoundPattern.matches(previousLine)) {
                progressPattern.matchMatcher(progress) {
                    familiesFound = group("current").formatLong()
                    totalFamilies = group("needed").formatLong()
                }
            } else if (familyCompletedPattern.matches(previousLine)) {
                progressPattern.matchMatcher(progress) {
                    familiesCompleted = group("current").formatLong()
                }
            }
        }

        return Category(
            name,
            fullName,
            familiesFound,
            totalFamilies,
            familiesCompleted,
            slot,
        )
    }

    private fun inCategoryOfCategories(inventoryName: String) {
        for ((index, stack) in stackList) {
            val cleanName = stack.cleanName
            if (cleanName == " " || cleanName.isEmpty()) continue
            if (!indexes.contains(index)) continue
            val name = stack.hoverName
            val fullName = getFullNameForItem(inventoryName, cleanName)
            val category = parseCategoryFromStack(name, fullName, stack, index)
            if (category.totalFamilies == 0L) continue
            catList.add(category)
        }
    }

    private fun inCategoryOfMobs() {
        for ((index, stack) in stackList) {
            val cleanName = stack.cleanName
            if (cleanName == " " || cleanName.isEmpty()) continue
            if (!indexes.contains(index)) continue

            val mob = parseMobFromStack(stack, index)
            if (mob != null) {
                mobList.add(mob)
            }
        }
    }

    private fun inMobVariants() {
        for ((index, stack) in stackList) {
            if (!indexes.contains(index)) continue
            val cleanName = stack.cleanName
            if (cleanName == " " || cleanName.isEmpty()) continue
            val variant = getMobVariant(stack, index)
            if (variant != null) {
                mobVariants.add(variant)
            }
        }
    }

    private fun getMobVariant(stack: SafeItemStack, slot: Int): BestiaryMobVariant? {
        val (name, levelOrTier) = mobVariantPattern.matchStyledMatcher(stack.hoverName.intoSpan()) {
            val name = group("name")?.intoComponent() ?: return@matchStyledMatcher null
            val levelOrTier = group("level")?.getText()?.formatInt() ?: return@matchStyledMatcher null
            name to levelOrTier
        } ?: return null

        return BestiaryMobVariant(
            name = name,
            level = levelOrTier,
            slot = slot
        )
    }

    private fun parseStackName(component: Component): Pair<Component, String>? {
        var level = "0"
        val name = mobLevelPattern.matchStyledMatcher(component) {
            level = group("level")?.getText() ?: "0"
            group("name")
        }?.intoComponent() ?: return null
        return name to level
    }

    private fun parseMobFromStack(
        stack: SafeItemStack,
        slot: Int?,
    ): BestiaryMob? {
        val (name, level) = parseStackName(stack.hoverName) ?: return null

        var totalKillToMax: Long = 0
        var currentTotalKill: Long = 0
        var totalKillToTier: Long = 0
        var currentKillToTier: Long = 0
        var actualRealTotalKill: Long = 0
        var isUnlocked = true

        val cleanLore = stack.getCleanLore()
        for ((lineIndex, line) in cleanLore.withIndex()) {
            if (notUnlockedFamilyPattern.matches(line)) {
                isUnlocked = false
            }

            killsLinePattern.findMatcher(line) {
                actualRealTotalKill = group("kills").formatLong()
            }

            if (!progressBarLinePattern.matches(line)) continue
            if (lineIndex == 0) continue

            val previousLine = cleanLore[lineIndex - 1]
            val progress = line.substring(line.lastIndexOf(' ') + 1)

            if (tierProgressPattern.matches(previousLine)) {
                progressPattern.matchMatcher(progress) {
                    totalKillToTier = group("needed").formatLong()
                    currentKillToTier = group("current").formatLong()
                }
            } else if (overallProgressPattern.matches(previousLine)) {
                progressPattern.matchMatcher(progress) {
                    totalKillToMax = group("needed").formatLong()
                    currentTotalKill = group("current").formatLong()
                }
            }
        }

        if (totalKillToMax == 0L && totalKillToTier == 0L && isUnlocked) return null

        return BestiaryMob(
            name,
            level.romanToDecimalIfNecessary(),
            totalKillToMax,
            currentTotalKill,
            totalKillToTier,
            currentKillToTier,
            actualRealTotalKill,
            slot = slot,
        )
    }

    fun isOverallProgressEnabled(inventoryItems: Map<Int, SafeItemStack>): Boolean {
        val stack = inventoryItems[OVERALL_PROGRESS_SLOT]
        if (stack?.item == Items.ENDER_EYE) {
            return overallProgressShownPattern.anyMatches(stack.getCleanLore())
        }

        indexes.forEach { index ->
            val item = inventoryItems[index] ?: return@forEach
            val cleanLore = item.getCleanLore()
            val hasTierProgress = tierProgressPattern.anyMatches(cleanLore)
            val hasOverallProgress = overallProgressPattern.anyMatches(cleanLore)
            if (hasTierProgress && !hasOverallProgress) return false
        }

        return true
    }

    fun isBestiaryGui(stack: SafeItemStack, name: String): Boolean {
        inventoryTitlePattern.matchMatcher(name) {
            val parent = groupOrNull("parent")
            if (parent != "Bestiary") {
                val cleanLore = stack.getCleanLore()
                val hasFamiliesFound = familyFoundPattern.anyMatches(cleanLore)
                val hasKills = killsLinePattern.anyMatches(cleanLore)

                if (!hasFamiliesFound && !hasKills) return false
            }
            return true
        }

        if (inventorySearchResultsPattern.matches(name)) {
            val cleanLore = stack.getCleanLore()
            return cleanLore.size >= 2 &&
                cleanLore[0].startsWith("Query: ") &&
                cleanLore[1].startsWith("Results: ")
        }

        return false
    }

    fun getFullNameForItem(inventoryName: String, itemName: String): String {
        return inventoryTitlePattern.matchMatcher(inventoryName) {
            val parent = groupOrNull("parent")
            val category = group("category")
            if (parent == "Bestiary" || parent == null) {
                if (category != null) "$category/$itemName" else itemName
            } else {
                "$category/$itemName"
            }
        } ?: itemName
    }

    data class Category(
        val name: Component,
        val fullName: String,
        val familiesFound: Long,
        val totalFamilies: Long,
        val familiesCompleted: Long,
        val slot: Int? = null,
    )

    data class BestiaryMob(
        var name: Component,
        var level: Int,
        var killToMax: Long,
        var totalKills: Long,
        var killNeededForNextLevel: Long,
        var currentKillToNextLevel: Long,
        var actualRealTotalKill: Long,
        val slot: Int? = null,
    ) {

        val cleanName: String
            get() = name.string.removeColor()

        val romanLevel: String
            get() = takeUnless { level == 0 }?.let { level.toRoman() } ?: "0"

        fun killNeededToMax(): Long {
            return 0L.coerceAtLeast(killToMax - actualRealTotalKill)
        }

        fun killNeededToNextLevel(): Long {
            return 0L.coerceAtLeast(killNeededForNextLevel - currentKillToNextLevel)
        }

        fun percentToMax() = if (killToMax == 0L) 0.0 else actualRealTotalKill.toDouble() / killToMax

        fun percentToMaxFormatted() = percentToMax().formatPercentage()

        fun percentToTier() =
            if (killNeededForNextLevel == 0L) 1.0 else currentKillToNextLevel.toDouble() / killNeededForNextLevel

        fun percentToTierFormatted() = percentToTier().formatPercentage()
    }

    // TODO: Add more data, like kills, mob types, etc.
    data class BestiaryMobVariant(
        val name: Component,
        val level: Int,
        val slot: Int? = null,
    ) {

        val cleanName: String
            get() = name.string.removeColor().removeSuffix(" (Master)")
    }
}
