package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ResourcePackReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import org.intellij.lang.annotations.Language
import java.util.regex.Pattern
import kotlin.math.roundToInt

@Language("RegExp")
private const val VALUE_PATTERN = "(?<value>[\\d,.]+)(?: .*)?"

private val patternGroup = RepoPattern.group("stats")

enum class SkyblockStat(
    val color: LorenzColor,
    val hypixelIcon: Char,
    displayName: String? = null,
    private val hypixelId: String? = null,
    generatePatterns: Boolean = true,
) {
    DAMAGE(RED, '\uE050', generatePatterns = false), // weapon only

    // <editor-fold desc="Combat Stats">
    HEALTH(RED, '\uE010'), // TODO get from action bar
    DEFENSE(GREEN, '\uE008'), // TODO get from action bar
    TRUE_DEFENSE(WHITE, '\uE027'),
    STRENGTH(RED, '\uE00D'),
    CRIT_CHANCE(DARK_BLUE, '\uE02C', hypixelId = "CRITICAL_CHANCE"),
    CRIT_DAMAGE(DARK_BLUE, '\uE007', hypixelId = "CRITICAL_DAMAGE"),
    BONUS_ATTACK_SPEED(YELLOW, '\uE001', displayName = "Attack Speed", hypixelId = "ATTACK_SPEED"),
    FEROCITY(RED, '\uE00B'),
    SWING_RANGE(YELLOW, '\uE024'),
    INTELLIGENCE(AQUA, '\uE003'), // TODO get from action bar
    ABILITY_DAMAGE(RED, '\uE002', hypixelId = "ABILITY_DAMAGE_PERCENT"),
    HEALTH_REGEN(RED, '\uE011', hypixelId = "HEALTH_REGENERATION"),
    VITALITY(DARK_RED, '\uE028'), // TODO get from action bar
    MENDING(GREEN, '\uE014'),

    // <editor-fold desc="Mining Stats">
    BREAKING_POWER(DARK_GREEN, '\uE005'),
    MINING_SPEED(GOLD, '\uE015'),
    MINING_SPREAD(YELLOW, '\uE016'),
    GEMSTONE_SPREAD(YELLOW, '\uE00F'),
    PRISTINE(DARK_PURPLE, '\uE01C'),
    MINING_FORTUNE(GOLD, '\uE053'),
    ORE_FORTUNE(GOLD, '\uE053'),
    BLOCK_FORTUNE(GOLD, '\uE053'),
    DWARVEN_METAL_FORTUNE(GOLD, '\uE053'),
    GEMSTONE_FORTUNE(GOLD, '\uE053'),
    // </editor-fold>

    // <editor-fold desc="Farming Stats">
    BONUS_PEST_CHANCE(DARK_GREEN, '\uE019'),
    OVERBLOOM(YELLOW, '\uE02B'),
    FARMING_FORTUNE(GOLD, '\uE051'),
    WHEAT_FORTUNE(GOLD, '\uE051'),
    CARROT_FORTUNE(GOLD, '\uE051'),
    POTATO_FORTUNE(GOLD, '\uE051'),
    PUMPKIN_FORTUNE(GOLD, '\uE051'),
    SUGAR_CANE_FORTUNE(GOLD, '\uE051'),
    MELON_FORTUNE(GOLD, '\uE051', displayName = "Melon Slice Fortune"),
    CACTUS_FORTUNE(GOLD, '\uE051'),
    COCOA_BEANS_FORTUNE(GOLD, '\uE051'),
    MUSHROOM_FORTUNE(GOLD, '\uE051'),
    NETHER_STALK_FORTUNE(GOLD, '\uE051', displayName = "Nether Wart Fortune"),
    SUNFLOWER_FORTUNE(GOLD, '\uE051'),
    MOONFLOWER_FORTUNE(GOLD, '\uE051'),
    WILD_ROSE_FORTUNE(GOLD, '\uE051'),
    // </editor-fold>

    // <editor-fold desc="Foraging Stats">
    SWEEP(DARK_GREEN, '\uE023'),
    FORAGING_FORTUNE(GOLD, '\uE054'),
    FIG_FORTUNE(GOLD, '\uE054'),
    MANGROVE_FORTUNE(GOLD, '\uE054'),
    HELIX_FORTUNE(GOLD, '\uE054'),
    // </editor-fold>

    // <editor-fold desc="Fishing Stats">
    FISHING_SPEED(AQUA, '\uE00C'),
    SEA_CREATURE_CHANCE(DARK_AQUA, '\uE021'),
    DOUBLE_HOOK_CHANCE(BLUE, '\uE009'),
    TROPHY_FISH_CHANCE(GOLD, '\uE02A', displayName = "Trophy Chance"),
    TREASURE_CHANCE(GOLD, '\uE025'),

    // </editor-fold>

    // <editor-fold desc="Miscellaneous Stats">
    // TODO get from Minecraft walk speed attribute (500+ Speed works fine now)
    SPEED(WHITE, '\uE022', hypixelId = "WALK_SPEED"),
    MAGIC_FIND(AQUA, '\uE01A'),
    PET_LUCK(LIGHT_PURPLE, '\uE013'),
    HEAT_RESISTANCE(RED, '\uE012'),
    COLD_RESISTANCE(AQUA, '\uE006'),
    RESPIRATION(DARK_AQUA, '\uE01D'),
    PRESSURE_RESISTANCE(BLUE, '\uE01B'),
    FEAR(DARK_PURPLE, '\uE00A'),
    TRACKING(LIGHT_PURPLE, '\uE077'),
    // </editor-fold>

    // <editor-fold desc="Hunting Stats">
    PULL(AQUA, '\uE02D'),
    HUNTER_FORTUNE(LIGHT_PURPLE, '\uE05B'),
    CHARM_CHANCE(AQUA, '❣'),
    // </editor-fold>

    // <editor-fold desc="Wisdom Stats">
    COMBAT_WISDOM(DARK_AQUA, '☯'),
    FARMING_WISDOM(DARK_AQUA, '☯'),
    FISHING_WISDOM(DARK_AQUA, '☯'),
    MINING_WISDOM(DARK_AQUA, '☯'),
    FORAGING_WISDOM(DARK_AQUA, '☯'),
    ENCHANTING_WISDOM(DARK_AQUA, '☯'),
    ALCHEMY_WISDOM(DARK_AQUA, '☯'),
    CARPENTRY_WISDOM(DARK_AQUA, '☯'),
    RUNECRAFTING_WISDOM(DARK_AQUA, '☯'),
    TAMING_WISDOM(DARK_AQUA, '☯'),
    SOCIAL_WISDOM(DARK_AQUA, '☯'),
    HUNTING_WISDOM(DARK_AQUA, '☯'),
    // </editor-fold>

    // <editor-fold desc="Rift Stats">
    RIFT_TIME(GREEN, '\uE020'),
    RIFT_DAMAGE(DARK_PURPLE, '\uE01E'),
    // TODO get from Minecraft walk speed attribute
    RIFT_SPEED(WHITE, '\uE022', displayName = "Speed", hypixelId = "RIFT_WALK_SPEED"),
    RIFT_INTELLIGENCE(AQUA, '\uE003', displayName = "Intelligence"),
    // MAGIC_FIND is just the overworld stat
    MANA_REGEN(AQUA, '\uE004'),
    HEARTS(RED, '\uE01F'),
    // </editor-fold>

    UNKNOWN(GRAY, '?', generatePatterns = false),
    ;

    val displayName: String = displayName ?: toFormattedName()

    var lastKnownValue: Double?
        get() = ProfileStorageData.profileSpecific?.stats?.get(this)
        set(value) {
            ProfileStorageData.profileSpecific?.stats?.set(this, value)
        }

    @Suppress("UNNECESSARY_SAFE_CALL")
    val icon: String
        get() = resourcePackOverrides?.get(name) ?: "${color.getChatColor()}$hypixelIcon"

    var lastSource: StatSourceType = StatSourceType.UNKNOWN

    var lastAssignment: SimpleTimeMark = SimpleTimeMark.farPast()

    private val capitalizedName = name.lowercase().allLettersFirstUppercase()

    val iconWithName
        get() = "$icon $capitalizedName"

    private val keyName = name.lowercase().replace('_', '.')

    val displayValue get() = lastKnownValue?.let { icon + it.roundToInt() }

    val tablistPattern by patternGroup.pattern(
        "tablist.no-color.$keyName",
        if (generatePatterns) " *${this@SkyblockStat.displayName}: $hypixelIcon$VALUE_PATTERN" else "",
    )

    val menuPattern by patternGroup.pattern(
        "menu.no-color.$keyName",
        if (generatePatterns) "\\s*$hypixelIcon ${this@SkyblockStat.displayName} $VALUE_PATTERN" else "",
    )

    fun asString(value: Int) = (if (value > 0) "+" else "") + value.toString() + " " + this.icon

    @SkyHanniModule
    companion object {
        val fontSizeOfLargestIcon by lazy {
            entries.maxOf { Minecraft.getInstance().font.width(it.icon) } + 1
        }

        fun getValueOrNull(string: String): SkyblockStat? = entries.firstOrNull { it.name == string || it.hypixelId == string }

        fun getValueByDisplayNameOrNull(string: String): SkyblockStat? = entries.firstOrNull { it.displayName == string }

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

        fun getIconOrNull(string: String): String? = resourcePackOverrides[string] ?: getValueOrNull(string)?.icon

        fun getIconByDisplayNameOrNull(string: String): String? = getValueByDisplayNameOrNull(string)?.icon

        private var resourcePackOverrides = emptyMap<String, String>()

        @HandleEvent
        fun onResourcePackReload(event: ResourcePackReloadEvent) {
            val packOverrides = event.getJsonResource<Map<String, String>>(SkyHanniMod.id("icon_overrides.json"))

            resourcePackOverrides = packOverrides.orEmpty()
        }

        private const val PLAYER_STATS_SLOT_INDEX = 13

        private fun onSkyblockMenu(event: InventoryFullyOpenedEvent) {
            if (event.inventoryName != "SkyBlock Menu") return
            val list = event.inventoryItems[PLAYER_STATS_SLOT_INDEX]?.getCleanLore() ?: return
            DelayedRun.runNextTick { // Delayed to not impact opening time
                assignEntry(list, StatSourceType.SKYBLOCK_MENU) { it.menuPattern }
            }
        }

        private val statsMenuRelevantSlotIndexes = listOf(14, 15, 16, 23, 24, 25, 32, 33, 34)

        private fun onStatsMenu(event: InventoryFullyOpenedEvent) {
            if (event.inventoryName != "Your Equipment and Stats") return
            val list = statsMenuRelevantSlotIndexes
                .mapNotNull { event.inventoryItems[it]?.getCleanLore() }
                .flatten()
            if (list.isEmpty()) return
            DelayedRun.runNextTick { // Delayed to not impact opening time
                assignEntry(list, StatSourceType.STATS_MENU) { it.menuPattern }
            }
        }

        @HandleEvent
        fun onWidgetUpdate(event: WidgetUpdateEvent) {
            if (!event.isWidget(TabWidget.STATS, TabWidget.DUNGEON_SKILLS_AND_STATS)) return
            val type = if (event.isWidget(TabWidget.DUNGEON_SKILLS_AND_STATS)) StatSourceType.TABLIST_DUNGEON else StatSourceType.TABLIST
            assignEntry(event.lines.map { it.string }, type) { it.tablistPattern }
        }

        private fun assignEntry(lines: List<String>, type: StatSourceType, pattern: (SkyblockStat) -> Pattern) {
            for (line in lines) for (entry in entries) {
                val matchResult = pattern(entry).matchMatcher(line) {
                    groupOrNull("value")?.replace("[,%]".toRegex(), "")?.toDouble()
                } ?: continue
                entry.lastKnownValue = matchResult
                entry.lastSource = type
                entry.lastAssignment = SimpleTimeMark.now()
                break // Exit the inner loop once a match is found
            }
        }

        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(69, "#profile.stats.TRUE_DEFENCE", "#profile.stats.TRUE_DEFENSE")
            event.move(112, "#profile.stats.NETHER_WART_FORTUNE", "#profile.stats.NETHER_STALK_FORTUNE")
            event.remove(113, "#profile.stats.null")
        }
    }
}

class SkyblockStatList : LinkedHashMap<SkyblockStat, Double>(), Map<SkyblockStat, Double> {

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
