package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EnumUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RegexUtils.replace
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.isRoman
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.Optional
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ReplaceRomanNumerals {

    private val patternGroup = RepoPattern.group("romannumerals")
    private val inventoryGroup = patternGroup.group("inventory")

    // SkillType only holds skills with XP progression, Runecrafting and Social are not part of it
    private val skillNames = "(?:" + EnumUtils.enumJoinToPattern<SkillType> { it.displayName } + "|Runecrafting|Social)"

    /**
     * REGEX-TEST: Catacombs Level XII
     * REGEX-TEST: Reach Catacombs Level XII by
     * REGEX-TEST: Progress to Level XXIV: 100%
     * REGEX-TEST: Hunting Level XX
     */
    private val levelPattern by inventoryGroup.pattern(
        "level",
        "\\bLevel (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * REGEX-TEST: Progress to Tier III: 0%
     * REGEX-TEST: Progress to Tier X: 100%
     * REGEX-TEST: Reach Tier IX in your Chili Pepper
     */
    private val tierPattern by inventoryGroup.pattern(
        "tier",
        "\\bTier (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * REGEX-TEST: Backwater Bayou Chapter VII
     * REGEX-TEST: Complete Chapter VII on the
     * REGEX-TEST: Chapter IV Progress: 66.7%
     */
    private val chapterPattern by inventoryGroup.pattern(
        "chapter",
        "\\bChapter (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * REGEX-TEST: Fishing Skill XXIV
     * WRAPPED-REGEX-TEST: " ✔ Farming Skill XXIV"
     */
    private val skillPattern by inventoryGroup.pattern(
        "skill",
        "\\bSkill (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * REGEX-TEST: Reach Frozen Corpse Milestone VII
     * REGEX-TEST: Progress to Milestone VII: 0%
     * REGEX-TEST: Bestiary Milestone CCC
     */
    private val milestonePattern by inventoryGroup.pattern(
        "milestone",
        "\\bMilestone (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * REGEX-TEST: Master Catacombs Floor VII
     * REGEX-TEST: Complete Master Catacombs Floor VII.
     * REGEX-TEST: Progress to Floor VII: 100%
     */
    private val floorPattern by inventoryGroup.pattern(
        "floor",
        "\\bFloor (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * REGEX-TEST: Chili Pepper Collection IX
     * REGEX-FAIL: View all your Ender Pearl Collection
     */
    private val collectionPattern by inventoryGroup.pattern(
        "collection",
        "\\bCollection (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * Anchored by the prefix and the colon, so the name in between may be any sequence of plain words.
     * REGEX-TEST: Progress to Vinesap V:
     * REGEX-TEST: Progress to Helix Log IX:
     * REGEX-TEST: Progress to Milestone CCC:
     * REGEX-FAIL: Progress: 81.9%
     * REGEX-FAIL: Progress to Amount Consumed: 0%
     */
    private val progressPattern by inventoryGroup.pattern(
        "progress",
        "\\bProgress to (?:\\w+ )+(?<roman>[IVXLCDM]+):",
    )

    /**
     * REGEX-TEST: Fishing L
     * REGEX-TEST: Enchanting LX
     * REGEX-TEST: Hunting XIX
     * REGEX-TEST: Runecrafting XXV
     * REGEX-TEST: Social XVII
     * REGEX-FAIL: Farming Skill XXIV
     * REGEX-FAIL: Hunting Level XX
     * REGEX-FAIL: Combat Collections
     */
    private val skillNamePattern by inventoryGroup.pattern(
        "skill-name",
        "\\b$skillNames (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * REGEX-TEST: Heart of the Mountain X
     * REGEX-TEST: Reach Heart of the Mountain X in the
     * REGEX-TEST: Heart of the Forest III
     * REGEX-TEST: Reach Heart of the Forest III in
     */
    private val heartOfThePattern by inventoryGroup.pattern(
        "heart-of-the",
        "\\bHeart of the \\w+ (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * Anchored by the suffix, the numeral belongs to whatever is named before it.
     * Singular and plural both occur, depending on how many rewards the level grants.
     * REGEX-TEST: Helix Log IX Rewards:
     * REGEX-TEST: Vinesap V Rewards:
     * REGEX-TEST: Level XVIII Reward:
     * REGEX-FAIL: Rewards:
     */
    private val rewardsPattern by inventoryGroup.pattern(
        "rewards",
        "\\b(?<roman>[IVXLCDM]+) Rewards?:",
    )

    /**
     * The tier of an item is repeated in its reward lines, where nothing else anchors it.
     * Every hit is checked against the numeral already read from the display name.
     * REGEX-TEST: Brewer XLIII
     * REGEX-TEST: Charming XX
     */
    private val standaloneNumeralPattern by inventoryGroup.pattern(
        "standalone-numeral",
        "\\b(?<roman>[IVXLCDM]+)\\b",
    )

    private val contextGroup = inventoryGroup.group("context")

    /**
     * REGEX-TEST: 4 tasks
     * REGEX-TEST: 11 tasks
     */
    private val taskListPattern by contextGroup.pattern(
        "task-list",
        "^\\d+ tasks?$",
    )

    /**
     * REGEX-TEST: Feast Perk Shop.
     * REGEX-TEST: Purchase perks from the Safari
     * REGEX-TEST: Purchase Boss Luck I from the
     * REGEX-TEST: Purchase Extra Farming Fortune II
     */
    private val perkShopPattern by contextGroup.pattern(
        "perk-shop",
        "Perk Shop|^Purchase ",
    )

    /**
     * Matched against the inventory title, not against a lore line.
     * REGEX-TEST: Plant Yield Upgrades
     * REGEX-TEST: Growth Speed Upgrades
     * REGEX-TEST: Plot Limit Upgrades
     */
    private val upgradeMenuPattern by contextGroup.pattern(
        "upgrade-menu",
        "^.+ Upgrades$",
    )

    /**
     * The name wraps into the next line on long collections, so the prefix carries the match.
     * REGEX-TEST: Total Collected: 210,194
     * REGEX-TEST: View all your Gunpowder Collection
     * REGEX-TEST: View all your Ruby Veilshroom
     */
    private val collectionItemPattern by contextGroup.pattern(
        "collection-item",
        "Total Collected:|View all your ",
    )

    /**
     * Matched against the inventory title. The singular is what separates the detail menu of a
     * single collection from the overview menus, which end in Collections.
     * REGEX-TEST: Honeycomb Collection
     * REGEX-FAIL: Foraging Collections
     */
    private val collectionMenuPattern by contextGroup.pattern(
        "collection-menu",
        "^.+ Collection$",
    )

    /**
     * Only used inside a recognised context, where a bare name followed by a numeral is safe.
     * REGEX-TEST: Leftovers II
     * REGEX-TEST: Plant Yield V
     * REGEX-TEST: Critter Catcher VII
     * REGEX-TEST: Diana's Favor III
     * WRAPPED-REGEX-TEST: "Leftovers II "
     * REGEX-FAIL: 5 SkyBlock XP
     * REGEX-FAIL: UNLOCKED
     * REGEX-FAIL: You can't afford this upgrade!
     */
    private val namedTierPattern by contextGroup.pattern(
        "named-tier",
        "^(?:[\\w']+ )+(?<roman>[IVXLCDM]+)\\s*$",
    )

    /**
     * REGEX-TEST: increase your Pumpkin tier!
     * REGEX-TEST: to increase your Sunflower tier!
     */
    private val cropMilestonePattern by contextGroup.pattern(
        "crop-milestone",
        "increase your .+ tier!",
    )

    private enum class ToolTipContext(val titleOnly: Boolean) {
        TASK_LIST(titleOnly = false),
        PERK_SHOP(titleOnly = false),
        UPGRADE_MENU(titleOnly = false),
        CROP_MILESTONE(titleOnly = false),

        // Co-op contribution lines carry player names, so only the display name is touched
        COLLECTION(titleOnly = true),
    }

    // Rebuilt on repo reload, since that replaces the Pattern behind each delegate
    private var inventoryPatterns = buildInventoryPatterns()

    private fun buildInventoryPatterns() = listOf(
        levelPattern,
        tierPattern,
        chapterPattern,
        skillPattern,
        milestonePattern,
        floorPattern,
        collectionPattern,
        progressPattern,
        skillNamePattern,
        heartOfThePattern,
        rewardsPattern,
    )

    // Using toRegex here since toPattern doesn't seem to provide the necessary functionality
    private val splitRegex = "((§\\w)|(\\s+)|(\\W))+|(\\w*)".toRegex()
    private val cachedStrings = TimeLimitedCache<String, String>(5.seconds)

    // LOW runs after default priority, so RepoPatternManager has already replaced the patterns
    @HandleEvent(priority = HandleEvent.LOW)
    private fun onRepoReload() {
        cachedStrings.clear()
        inventoryPatterns = buildInventoryPatterns()
    }

    // LOWEST to also cover lines added by other ToolTipTextEvent listeners.
    // The deprecated ToolTipEvent runs later regardless of priority.
    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return

        val toolTip = event.toolTip
        val context = findContext(toolTip)
        val titleOnly = context?.titleOnly ?: false
        val contextPatterns = if (context != null) inventoryPatterns + namedTierPattern else inventoryPatterns

        // Skipped for titleOnly, where nothing below the display name may be touched
        val titleNumeral = if (titleOnly) null
        else toolTip.firstOrNull()?.let { findTitleNumeral(it, contextPatterns) }

        for (index in toolTip.indices) {
            val patterns = if (titleOnly && index != 0) inventoryPatterns else contextPatterns
            val replaced = toolTip[index].replaceNumerals(patterns, if (index == 0) null else titleNumeral) ?: continue
            toolTip[index] = replaced
        }
    }

    /**
     * The first numeral in the display name, read before anything is replaced.
     * Reward lines below repeat it without a keyword of their own.
     */
    private fun findTitleNumeral(title: Component, patterns: List<Pattern>): String? {
        var found: String? = null
        title.visit(
            { _: Style?, string: String? ->
                if (found == null) found = findNumeral(string.orEmpty(), patterns)
                Optional.empty<Component>()
            },
            Style.EMPTY,
        )
        return found
    }

    private fun findNumeral(text: String, patterns: List<Pattern>): String? {
        for (pattern in patterns) {
            pattern.findMatcher(text) {
                val roman = group("roman")
                if (roman.isRoman()) return roman
            }
        }
        return null
    }

    /**
     * A context is a place where a bare name followed by a numeral is known to be a tier,
     * so the numeral can be replaced without a keyword in front of it.
     */
    private fun findContext(toolTip: List<Component>): ToolTipContext? =
        findMenuContext() ?: toolTip.firstNotNullOfOrNull(::findLineContext)

    private fun findMenuContext(): ToolTipContext? {
        val inventoryName = InventoryUtils.openInventoryName()
        return when {
            upgradeMenuPattern.matches(inventoryName) -> ToolTipContext.UPGRADE_MENU
            collectionMenuPattern.matches(inventoryName) -> ToolTipContext.COLLECTION
            else -> null
        }
    }

    private fun findLineContext(line: Component): ToolTipContext? = when {
        taskListPattern.matches(line) -> ToolTipContext.TASK_LIST
        perkShopPattern.find(line) -> ToolTipContext.PERK_SHOP
        cropMilestonePattern.find(line) -> ToolTipContext.CROP_MILESTONE
        collectionItemPattern.find(line) -> ToolTipContext.COLLECTION
        else -> null
    }

    private fun Component.replaceNumerals(patterns: List<Pattern>, titleNumeral: String?): Component? {
        val result = Component.empty()
        var changed = false
        visit(
            { style: Style?, string: String? ->
                val original = string.orEmpty()
                val new = replaceInText(original, patterns, titleNumeral)
                if (new != null) changed = true
                result.append(Component.literal(new ?: original).withStyle(style ?: Style.EMPTY))
                Optional.empty<Component>()
            },
            Style.EMPTY,
        )
        return result.takeIf { changed }
    }

    private fun replaceInText(text: String, patterns: List<Pattern>, titleNumeral: String?): String? {
        var result = text
        for (pattern in patterns) {
            result = pattern.replaceIn(result)
        }
        if (titleNumeral != null) result = replaceTitleNumeral(result, titleNumeral)
        return result.takeIf { it != text }
    }

    private fun replaceTitleNumeral(text: String, titleNumeral: String): String =
        standaloneNumeralPattern.replace(text) {
            if (group("roman") != titleNumeral) return@replace group()
            titleNumeral.romanToDecimal().toString()
        }

    private fun Pattern.replaceIn(text: String): String = replace(text) {
        val roman = group("roman")
        // The pattern only locates the numeral, isRoman decides whether it is a valid one
        if (!roman.isRoman()) return@replace group()
        text.substring(start(), start("roman")) + roman.romanToDecimal() + text.substring(end("roman"), end())
    }

    fun replaceLine(line: String): String {
        if (!isEnabled()) return line

        return cachedStrings.getOrPut(line) {
            line.replace()
        }
    }

    private fun String.replace() = splitRegex.findAll(this).map { it.value }.joinToString("") {
        it.takeIf { it.isValidRomanNumeral() && it.removeFormatting().romanToDecimal() != 2000 }?.coloredRomanToDecimal() ?: it
    }

    private fun String.removeFormatting() = removeColor().replace(",", "")

    private fun String.isValidRomanNumeral() = removeFormatting().let { it.isRoman() && it.isNotEmpty() }

    private fun String.coloredRomanToDecimal() = removeFormatting().let { replace(it, it.romanToDecimal().toString()) }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && SkyHanniMod.feature.misc.replaceRomanNumerals.get()

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Replace Roman Numerals")
        event.addIrrelevant {
            val map = cachedStrings.toMap()
            add("cachedStrings: (${map.size})")
            for ((original, changed) in map) {
                if (original == changed) {
                    add("unchanged: '$original'")
                } else {
                    add("'$original' -> '$changed'")
                }
            }
        }
    }
}
