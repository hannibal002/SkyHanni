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

    enum class GuiType {
        CLOSED,
        CATEGORY_OF_CATEGORIES,
        CATEGORY_OF_MOBS,
        MOB_VARIANTS,
    }

    sealed class BestiaryGuiState(val type: GuiType) {
        object Closed : BestiaryGuiState(GuiType.CLOSED)

        abstract class Open(
            type: GuiType,
            val overallProgressEnabled: Boolean,
        ) : BestiaryGuiState(type)

        class Categories(
            overallProgressEnabled: Boolean,
            val categories: Map<Int, Category>,
        ) : Open(GuiType.CATEGORY_OF_CATEGORIES, overallProgressEnabled)

        class Mobs(
            overallProgressEnabled: Boolean,
            val parentCategory: Category?,
            val mobs: Map<Int, BestiaryMob>,
        ) : Open(GuiType.CATEGORY_OF_MOBS, overallProgressEnabled)

        class Variants(
            overallProgressEnabled: Boolean,
            val parentCategory: Category?,
            val parentFamily: BestiaryMob?,
            val variants: Map<Int, BestiaryMobVariant>,
        ) : Open(GuiType.MOB_VARIANTS, overallProgressEnabled)
    }

    val indexes = listOf(
        10..16,
        19..25,
        28..34,
        37..43,
    ).flatten()

    const val OVERALL_PROGRESS_SLOT = 52

    // Single source of truth for the active inventory state
    var currentState: BestiaryGuiState = BestiaryGuiState.Closed
        private set

    private var pendingCategory: Category? = null
    private var pendingFamily: BestiaryMob? = null

    val inInventory: Boolean get() = currentState is BestiaryGuiState.Open
    val isCategoryOfCategories: Boolean get() = currentState.type == GuiType.CATEGORY_OF_CATEGORIES
    val isCategoryOfMobs: Boolean get() = currentState.type == GuiType.CATEGORY_OF_MOBS
    val isMobVariants: Boolean get() = currentState.type == GuiType.MOB_VARIANTS

    val overallProgressEnabled: Boolean
        get() = (currentState as? BestiaryGuiState.Open)?.overallProgressEnabled ?: false

    val currentCategory: Category?
        get() = when (val state = currentState) {
            is BestiaryGuiState.Mobs -> state.parentCategory
            is BestiaryGuiState.Variants -> state.parentCategory
            else -> null
        }

    val currentFamily: BestiaryMob?
        get() = (currentState as? BestiaryGuiState.Variants)?.parentFamily

    val mobList: List<BestiaryMob>
        get() = (currentState as? BestiaryGuiState.Mobs)?.mobs?.values?.toList().orEmpty()

    val catList: List<Category>
        get() = (currentState as? BestiaryGuiState.Categories)?.categories?.values?.toList().orEmpty()

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        val items = event.inventoryItems
        val stack = items[4] ?: return

        if (!isBestiaryGui(stack, inventoryName) && !inventoryCategoryOfCategoryPattern.matches(inventoryName)) return

        val isOverallProgress = isOverallProgressEnabled(items)
        val cleanLore = stack.getCleanLore()
        val hasFamilies = cleanLore.any { familyFoundPattern.matches(it) || familyCompletedPattern.matches(it) }
        val hasMobFamily = parseStackName(stack.hoverName) != null

        // Capture previous references before we transition states
        val oldCategory = currentCategory
        val oldFamily = currentFamily

        currentState = if (inventoryCategoryOfCategoryPattern.matches(inventoryName)) {
            val map = parseCategoryOfCategories(inventoryName, items)
            BestiaryGuiState.Categories(isOverallProgress, map)
        } else if (hasFamilies || inventorySearchResultsPattern.matches(inventoryName)) {
            val map = parseCategoryOfMobs(items)
            BestiaryGuiState.Mobs(isOverallProgress, pendingCategory ?: oldCategory, map)
        } else if (hasMobFamily) {
            val map = parseMobVariants(items)
            BestiaryGuiState.Variants(isOverallProgress, oldCategory, pendingFamily ?: oldFamily, map)
        } else {
            BestiaryGuiState.Closed
        }

        // Clean up pending states after applying them
        if (isCategoryOfMobs || isMobVariants) {
            pendingCategory = null
        }
        if (isMobVariants) {
            pendingFamily = null
        }
    }

    @HandleEvent
    private fun onInventoryClose() {
        currentState = BestiaryGuiState.Closed
        pendingCategory = null
        pendingFamily = null
    }

    @HandleEvent
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        when (val state = currentState) {
            is BestiaryGuiState.Categories -> {
                state.categories[event.slotId]?.let { pendingCategory = it }
            }
            is BestiaryGuiState.Mobs -> {
                state.mobs[event.slotId]?.let { pendingFamily = it }
            }
            else -> {}
        }
    }

    private fun parseCategoryFromStack(
        name: Component,
        fullName: String,
        stack: SafeItemStack,
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

        return Category(name, fullName, familiesFound, totalFamilies, familiesCompleted)
    }

    private fun parseCategoryOfCategories(inventoryName: String, items: Map<Int, SafeItemStack>): Map<Int, Category> {
        val map = mutableMapOf<Int, Category>()
        for ((index, stack) in items) {
            if (!indexes.contains(index)) continue
            val cleanName = stack.cleanName
            if (cleanName.isBlank()) continue

            val name = stack.hoverName
            val fullName = getFullNameForItem(inventoryName, cleanName)
            val category = parseCategoryFromStack(name, fullName, stack)

            if (category.totalFamilies > 0L) {
                map[index] = category
            }
        }
        return map
    }

    private fun parseCategoryOfMobs(items: Map<Int, SafeItemStack>): Map<Int, BestiaryMob> {
        val map = mutableMapOf<Int, BestiaryMob>()
        for ((index, stack) in items) {
            if (!indexes.contains(index)) continue
            val cleanName = stack.cleanName
            if (cleanName.isBlank()) continue

            val mob = parseMobFromStack(stack)
            if (mob != null) {
                map[index] = mob
            }
        }
        return map
    }

    private fun parseMobVariants(items: Map<Int, SafeItemStack>): Map<Int, BestiaryMobVariant> {
        val map = mutableMapOf<Int, BestiaryMobVariant>()
        for ((index, stack) in items) {
            if (!indexes.contains(index)) continue
            val cleanName = stack.cleanName
            if (cleanName.isBlank()) continue

            val variant = getMobVariant(stack)
            if (variant != null) {
                map[index] = variant
            }
        }
        return map
    }

    private fun getMobVariant(stack: SafeItemStack): BestiaryMobVariant? {
        val (name, levelOrTier) = mobVariantPattern.matchStyledMatcher(stack.hoverName.intoSpan()) {
            val name = group("name")?.intoComponent() ?: return@matchStyledMatcher null
            val levelOrTier = group("level")?.getText()?.formatInt() ?: return@matchStyledMatcher null
            name to levelOrTier
        } ?: return null

        return BestiaryMobVariant(name = name, level = levelOrTier)
    }

    private fun parseStackName(component: Component): Pair<Component, String>? {
        var level = "0"
        val name = mobLevelPattern.matchStyledMatcher(component) {
            level = group("level")?.getText() ?: "0"
            group("name")
        }?.intoComponent() ?: return null
        return name to level
    }

    private fun parseMobFromStack(stack: SafeItemStack): BestiaryMob? {
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
    )

    data class BestiaryMob(
        var name: Component,
        var level: Int,
        var killToMax: Long,
        var totalKills: Long,
        var killNeededForNextLevel: Long,
        var currentKillToNextLevel: Long,
        var actualRealTotalKill: Long,
    ) {
        val cleanName: String get() = name.string.removeColor()
        val romanLevel: String get() = takeUnless { level == 0 }?.let { level.toRoman() } ?: "0"

        fun killNeededToMax(): Long = 0L.coerceAtLeast(killToMax - actualRealTotalKill)
        fun killNeededToNextLevel(): Long = 0L.coerceAtLeast(killNeededForNextLevel - currentKillToNextLevel)

        fun percentToMax() = if (killToMax == 0L) 0.0 else actualRealTotalKill.toDouble() / killToMax
        fun percentToMaxFormatted() = percentToMax().formatPercentage()

        fun percentToTier() = if (killNeededForNextLevel == 0L) 1.0 else currentKillToNextLevel.toDouble() / killNeededForNextLevel
        fun percentToTierFormatted() = percentToTier().formatPercentage()
    }

    // TODO: Add more data, like kills, mob types, etc.
    data class BestiaryMobVariant(
        val name: Component,
        val level: Int,
    ) {
        val cleanName: String get() = name.string.removeColor().removeSuffix(" (Master)")
    }
}
