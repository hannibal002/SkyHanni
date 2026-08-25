package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EnumUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
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
    private val skillNames = EnumUtils.enumJoinToPattern<SkillType> { it.displayName } + "|Runecrafting|Social"


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
        "\\b(?:$skillNames) (?<roman>[IVXLCDM]+)\\b",
    )

    /**
     * REGEX-TEST: Heart of the Mountain X
     * REGEX-TEST: Reach Heart of the Mountain X in the
     */
    private val heartOfTheMountainPattern by inventoryGroup.pattern(
        "heart-of-the-mountain",
        "\\bHeart of the Mountain (?<roman>[IVXLCDM]+)\\b",
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
        heartOfTheMountainPattern,
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
        for (index in toolTip.indices) {
            val replaced = toolTip[index].replaceNumerals(inventoryPatterns) ?: continue
            toolTip[index] = replaced
        }
    }

    private fun Component.replaceNumerals(patterns: List<Pattern>): Component? {
        val result = Component.empty()
        var changed = false
        visit(
            { style: Style?, string: String? ->
                val original = string.orEmpty()
                val new = replaceInText(original, patterns)
                if (new != null) changed = true
                result.append(Component.literal(new ?: original).withStyle(style ?: Style.EMPTY))
                Optional.empty<Component>()
            },
            Style.EMPTY,
        )
        return result.takeIf { changed }
    }

    private fun replaceInText(text: String, patterns: List<Pattern>): String? {
        var result = text
        for (pattern in patterns) {
            result = pattern.replaceIn(result)
        }
        return result.takeIf { it != text }
    }

    private fun Pattern.replaceIn(text: String): String {
        val matcher = matcher(text)
        val builder = StringBuilder()
        var lastEnd = 0
        while (matcher.find()) {
            val start = matcher.start("roman")
            val end = matcher.end("roman")
            val roman = text.substring(start, end)
            // The pattern only locates the numeral, isRoman decides whether it is a valid one
            if (!roman.isRoman()) continue
            builder.append(text, lastEnd, start)
            builder.append(roman.romanToDecimal())
            lastEnd = end
        }
        if (lastEnd == 0) return text
        builder.append(text, lastEnd, text.length)
        return builder.toString()
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
