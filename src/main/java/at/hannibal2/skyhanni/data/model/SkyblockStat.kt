package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
//#if TODO
import at.hannibal2.skyhanni.data.ProfileStorageData
//#endif
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import org.intellij.lang.annotations.Language
import java.util.EnumMap
import java.util.regex.Pattern
import kotlin.math.roundToInt

@Language("RegExp")
private const val VALUE_PATTERN = "(?<value>[\\d,.]+)(?: .*)?"

// todo 1.21 impl needed
@Suppress("MaxLineLength")
enum class SkyblockStat(
    val icon: String,
    @Language("RegExp") tabListPatternS: String,
    @Language("RegExp") menuPatternS: String,
    private val hypxelId: String? = null,
) {
    DAMAGE("§c❁", "", ""), // Weapon only
    HEALTH("§c❤", " *Health: §r§c❤$VALUE_PATTERN", " *§c❤ Health §f$VALUE_PATTERN"), // TODO get from action bar
    DEFENSE("§a❈", " *Defense: §r§a❈$VALUE_PATTERN", " *§a❈ Defense §f$VALUE_PATTERN"), // TODO get from action bar
    STRENGTH("§c❁", " *Strength: §r§c❁$VALUE_PATTERN", " *§c❁ Strength §f$VALUE_PATTERN"),
    INTELLIGENCE(
        "§b✎",
        " *Intelligence: §r§b✎$VALUE_PATTERN",
        " *§b✎ Intelligence §f$VALUE_PATTERN",
    ), // TODO get from action bar
    CRIT_DAMAGE(
        "§9☠", " *Crit Damage: §r§9☠$VALUE_PATTERN", " *§9☠ Crit Damage §f$VALUE_PATTERN",
        hypxelId = "CRITICAL_DAMAGE",
    ),
    CRIT_CHANCE(
        "§9☣", " *Crit Chance: §r§9☣$VALUE_PATTERN", " *§9☣ Crit Chance §f$VALUE_PATTERN",
        hypxelId = "CRITICAL_CHANCE",
    ),
    FEROCITY("§c⫽", " *Ferocity: §r§c⫽$VALUE_PATTERN", " *§c⫽ Ferocity §f$VALUE_PATTERN"),
    BONUS_ATTACK_SPEED(
        "§e⚔",
        " *Attack Speed: §r§e⚔$VALUE_PATTERN",
        " *§e⚔ Bonus Attack Speed §f$VALUE_PATTERN",
        hypxelId = "ATTACK_SPEED",
    ),
    ABILITY_DAMAGE(
        "§c๑", " *Ability Damage: §r§c๑$VALUE_PATTERN", " *§c๑ Ability Damage §f$VALUE_PATTERN",
        hypxelId = "ABILITY_DAMAGE_PERCENT",
    ),
    HEALTH_REGEN(
        "§c❣",
        " *Health Regen: §r§c❣$VALUE_PATTERN",
        " *§c❣ Health Regen §f$VALUE_PATTERN",
        "HEALTH_REGENERATION",
    ),
    VITALITY("§4♨", " *Vitality: §r§4♨$VALUE_PATTERN", " *§4♨ Vitality §f$VALUE_PATTERN"),
    MENDING("§a☄", " *Mending: §r§a☄$VALUE_PATTERN", " *§a☄ Mending §f$VALUE_PATTERN"),
    TRUE_DEFENSE("§7❂", " *True Defense: §r§f❂$VALUE_PATTERN", " *§f❂ True Defense §f$VALUE_PATTERN"),
    SWING_RANGE("§eⓈ", " *Swing Range: §r§eⓈ$VALUE_PATTERN", " *§eⓈ Swing Range §f$VALUE_PATTERN"),

    // TODO add the way sba did get it (be careful with 500+ Speed)
    SPEED(
        "§f✦", " *Speed: §r§f✦$VALUE_PATTERN", " *§f✦ Speed §f$VALUE_PATTERN",
        hypxelId = "WALK_SPEED",
    ),
    SEA_CREATURE_CHANCE("§3α", " *Sea Creature Chance: §r§3α$VALUE_PATTERN", " *§3α Sea Creature Chance §f$VALUE_PATTERN"),
    MAGIC_FIND("§b✯", " *Magic Find: §r§b✯$VALUE_PATTERN", " *§b✯ Magic Find §f$VALUE_PATTERN"),
    PET_LUCK("§d♣", " *Pet Luck: §r§d♣$VALUE_PATTERN", " *§d♣ Pet Luck §f$VALUE_PATTERN"),
    FISHING_SPEED("§b☂", " *Fishing Speed: §r§b☂$VALUE_PATTERN", " *§b☂ Fishing Speed §f$VALUE_PATTERN"),
    TROPHY_FISH_CHANCE("§b☂", "Trophy Fish Chance: §r§6♔$VALUE_PATTERN", " *§6♔ Trophy Fish Chance §f(?<value>\\d+)%"),
    DOUBLE_HOOK_CHANCE(
        "§9⚓",
        " *Double Hook Chance: §r§9⚓$VALUE_PATTERN",
        " *§9⚓ Double Hook Chance §f(?<value>\\d+(?:\\.\\d+)?)%",
    ),
    TREASURE_CHANCE("§6⛃", " *Treasure Chance: §r§6⛃(?<value>\\d+(?:\\.\\d+)?)", " *§6⛃ Treasure Chance §f(?<value>\\d+(?:\\.\\d+)?)%"),
    BONUS_PEST_CHANCE(
        "§2ൠ",
        " *(?:§r§7§m)?Bonus Pest Chance: (?:§r§2)?ൠ$VALUE_PATTERN",
        " *(?:§7§m|§2)ൠ Bonus Pest Chance (?:§f)?$VALUE_PATTERN",
    ),
    COMBAT_WISDOM("§3☯", " *Combat Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Combat Wisdom §f$VALUE_PATTERN"),
    MINING_WISDOM("§3☯", " *Mining Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Mining Wisdom §f$VALUE_PATTERN"),
    FARMING_WISDOM("§3☯", " *Farming Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Farming Wisdom §f$VALUE_PATTERN"),
    FORAGING_WISDOM("§3☯", " *Foraging Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Foraging Wisdom §f$VALUE_PATTERN"),
    FISHING_WISDOM("§3☯", " *Fishing Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Fishing Wisdom §f$VALUE_PATTERN"),
    ENCHANTING_WISDOM("§3☯", " *Enchanting Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Enchanting Wisdom §f$VALUE_PATTERN"),
    ALCHEMY_WISDOM("§3☯", " *Alchemy Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Alchemy Wisdom §f$VALUE_PATTERN"),
    CARPENTRY_WISDOM("§3☯", " *Carpentry Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Carpentry Wisdom §f$VALUE_PATTERN"),
    RUNECRAFTING_WISDOM("§3☯", " *Runecrafting Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Runecrafting Wisdom §f$VALUE_PATTERN"),
    SOCIAL_WISDOM("§3☯", " *Social Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Social Wisdom §f$VALUE_PATTERN"),
    TAMING_WISDOM("§3☯", " *Taming Wisdom: §r§3☯$VALUE_PATTERN", " *§3☯ Taming Wisdom §f$VALUE_PATTERN"),
    MINING_SPEED("§6⸕", " *Mining Speed: §r§6⸕$VALUE_PATTERN", " *§6⸕ Mining Speed §f$VALUE_PATTERN"),
    BREAKING_POWER("§2Ⓟ", "", " *§2Ⓟ Breaking Power §f$VALUE_PATTERN"),
    PRISTINE("§5✧", " *Pristine: §r§5✧$VALUE_PATTERN", " *§5✧ Pristine §f$VALUE_PATTERN"),
    FORAGING_FORTUNE("§6☘", " *Foraging Fortune: §r§6☘$VALUE_PATTERN", " *§6☘ Foraging Fortune §f$VALUE_PATTERN"),
    FARMING_FORTUNE(
        "§6☘",
        " *(?:§r§7§m)?Farming Fortune: (?:§r§6)?☘$VALUE_PATTERN",
        " *(?:§7§m|§6)☘ Farming Fortune (?:§f)?$VALUE_PATTERN",
    ),
    MINING_FORTUNE("§6☘", " *Mining Fortune: §r§6☘$VALUE_PATTERN", " *§6☘ Mining Fortune §f$VALUE_PATTERN"),
    FEAR("§5☠", " *Fear: §r§5☠$VALUE_PATTERN", " *§5☠ Fear §f$VALUE_PATTERN"),
    COLD_RESISTANCE("§b❄", " *Cold Resistance: §r§b❄$VALUE_PATTERN", ""),
    WHEAT_FORTUNE("§7☘", "", " *§7§m☘ Wheat Fortune $VALUE_PATTERN"),
    CARROT_FORTUNE("§7☘", "", " *§7§m☘ Carrot Fortune $VALUE_PATTERN"),
    POTATO_FORTUNE("§7☘", "", " *§7§m☘ Potato Fortune $VALUE_PATTERN"),
    PUMPKIN_FORTUNE("§7☘", "", " *§7§m☘ Pumpkin Fortune $VALUE_PATTERN"),
    MELON_FORTUNE("§7☘", "", " *§7§m☘ Melon Fortune $VALUE_PATTERN"),
    MUSHROOM_FORTUNE("§7☘", "", " *§7§m☘ Mushroom Fortune $VALUE_PATTERN"),
    CACTUS_FORTUNE("§7☘", "", " *§7§m☘ Cactus Fortune $VALUE_PATTERN"),
    NETHER_WART_FORTUNE("§7☘", "", " *§7§m☘ Nether Wart Fortune $VALUE_PATTERN"),
    COCOA_BEANS_FORTUNE("§7☘", "", " *§7§m☘ Cocoa Beans Fortune $VALUE_PATTERN"),
    SUGAR_CANE_FORTUNE("§7☘", "", " *§7§m☘ Sugar Cane Fortune $VALUE_PATTERN"),

    MINING_SPREAD(
        "§e▚",
        " *(§r§7§m)?Mining Spread: (§r§e)?▚$VALUE_PATTERN",
        " *(§7§m|§e)▚ Mining Spread (§f)?$VALUE_PATTERN",
    ),
    GEMSTONE_SPREAD(
        "§e▚",
        " *(§r§7§m)?Mining Spread: (§r§e)?▚$VALUE_PATTERN",
        " *(§7§m|§e)▚ Gemstone Spread (§f)?$VALUE_PATTERN",
    ),
    ORE_FORTUNE("§6☘", " *Ore Fortune: §r§6☘$VALUE_PATTERN", " *§6☘ Ore Fortune §f103"),
    DWARVEN_METAL_FORTUNE(
        "§6☘",
        " *Dwarven Metal Fortune: §r§6☘$VALUE_PATTERN",
        " *§6☘ Dwarven Metal Fortune §f$VALUE_PATTERN",
    ),
    BLOCK_FORTUNE("§6☘", " *Block Fortune: §r§6☘$VALUE_PATTERN", " *§6☘ Block Fortune §f$VALUE_PATTERN"),
    GEMSTONE_FORTUNE("§6☘", " *Gemstone Fortune: §r§6☘$VALUE_PATTERN", " *§6☘ Gemstone Fortune §f$VALUE_PATTERN"),
    HEAT_RESISTANCE("§c♨", " *Heat Resistance: §r§c♨$VALUE_PATTERN", " *§c♨ Heat Resistance §f$VALUE_PATTERN"),

    UNKNOWN("§c?", "", "")
    ;

    //#if TODO
    var lastKnownValue: Double?
        get() = ProfileStorageData.profileSpecific?.stats?.get(this)
        set(value) {
            ProfileStorageData.profileSpecific?.stats?.set(this, value)
        }
    //#endif

    var lastSource: StatSourceType = StatSourceType.UNKNOWN

    var lastAssignment: SimpleTimeMark = SimpleTimeMark.farPast()

    private val capitalizedName = name.lowercase().allLettersFirstUppercase()

    val iconWithName = "$icon $capitalizedName"

    private val keyName = name.lowercase().replace('_', '.')

    //#if TODO
    val displayValue get() = lastKnownValue?.let { icon + it.roundToInt() }
    //#endif

    val tablistPattern by RepoPattern.pattern("stats.tablist.$keyName", tabListPatternS)
    val menuPattern by RepoPattern.pattern("stats.menu.$keyName", menuPatternS)

    fun asString(value: Int) = (if (value > 0) "+" else "") + value.toString() + " " + this.icon

    @SkyHanniModule
    companion object {

        val fontSizeOfLargestIcon by lazy {
            entries.maxOf { Minecraft.getMinecraft().fontRendererObj.getStringWidth(it.icon) } + 1
        }

        fun getValueOrNull(string: String): SkyblockStat? = entries.firstOrNull { it.name == string || it.hypxelId == string }

        fun getValue(string: String): SkyblockStat = getValueOrNull(string) ?: UNKNOWN

        init {
            entries.forEach {
                it.tablistPattern
                it.menuPattern
            }
        }

        @HandleEvent(onlyOnSkyblock = true)
        fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
            onSkyblockMenu(event)
            onStatsMenu(event)
        }

        private const val PLAYER_STATS_SLOT_INDEX = 13

        private fun onSkyblockMenu(event: InventoryFullyOpenedEvent) {
            if (event.inventoryName != "SkyBlock Menu") return
            val list = event.inventoryItems[PLAYER_STATS_SLOT_INDEX]?.getLore() ?: return
            DelayedRun.runNextTick { // Delayed to not impact opening time
                assignEntry(list, StatSourceType.SKYBLOCK_MENU) { it.menuPattern }
            }
        }

        private val statsMenuRelevantSlotIndexes = listOf(15, 16, 24, 25, 33)

        private fun onStatsMenu(event: InventoryFullyOpenedEvent) {
            if (event.inventoryName != "Your Equipment and Stats") return
            val list = statsMenuRelevantSlotIndexes.mapNotNull { event.inventoryItems[it]?.getLore() }.flatten()
            if (list.isEmpty()) return
            DelayedRun.runNextTick { // Delayed to not impact opening time
                assignEntry(list, StatSourceType.STATS_MENU) { it.menuPattern }
            }
        }

        @HandleEvent
        fun onTabList(event: WidgetUpdateEvent) {
            if (!event.isWidget(TabWidget.STATS, TabWidget.DUNGEON_SKILLS_AND_STATS)) return
            val type = if (event.isWidget(TabWidget.DUNGEON_SKILLS_AND_STATS)) StatSourceType.TABLIST_DUNGEON else StatSourceType.TABLIST
            assignEntry(event.lines, type) { it.tablistPattern }
        }

        private fun assignEntry(lines: List<String>, type: StatSourceType, pattern: (SkyblockStat) -> Pattern) {
            for (line in lines) for (entry in entries) {
                val matchResult = pattern(entry).matchMatcher(line) {
                    groupOrNull("value")?.replace("[,%]".toRegex(), "")?.toDouble()
                } ?: continue
                //#if TODO
                entry.lastKnownValue = matchResult
                //#endif
                entry.lastSource = type
                entry.lastAssignment = SimpleTimeMark.now()
                break // Exit the inner loop once a match is found
            }
        }

        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(69, "#profile.stats.TRUE_DEFENCE", "#profile.stats.TRUE_DEFENSE")
        }
    }
}

class SkyblockStatList : EnumMap<SkyblockStat, Double>(SkyblockStat::class.java), Map<SkyblockStat, Double> {
    operator fun minus(other: SkyblockStatList): SkyblockStatList {
        return SkyblockStatList().apply {
            val keys = this.keys + other.keys
            for (key in keys) {
                this[key] = (this@SkyblockStatList[key] ?: 0.0) - (other[key] ?: 0.0)
            }
        }
    }

    companion object {
        fun mapOf(vararg list: Pair<SkyblockStat, Double>) = SkyblockStatList().apply {
            for ((key, value) in list) {
                this[key] = value
            }
        }
    }
}

enum class StatSourceType {
    UNKNOWN,
    SKYBLOCK_MENU,
    STATS_MENU,
    TABLIST,
    TABLIST_DUNGEON,
}
