package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ResourcePackReloadEvent
import at.hannibal2.skyhanni.features.inventory.CurrentEquipmentApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.UtilsPatterns
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
    val hypixelIcon: SkyblockIcon,
    displayName: String? = null,
    private val hypixelId: String? = null,
    generatePatterns: Boolean = true,
) {
    DAMAGE(RED, SkyblockIcon.DAMAGE, generatePatterns = false), // weapon only

    // <editor-fold desc="Combat Stats">
    HEALTH(RED, SkyblockIcon.HEALTH), // TODO get from action bar
    DEFENSE(GREEN, SkyblockIcon.DEFENSE), // TODO get from action bar
    TRUE_DEFENSE(WHITE, SkyblockIcon.TRUE_DEFENSE),
    STRENGTH(RED, SkyblockIcon.STRENGTH),
    CRIT_CHANCE(
        DARK_BLUE,
        SkyblockIcon.CRIT_CHANCE,
        hypixelId = "CRITICAL_CHANCE",
    ),
    CRIT_DAMAGE(
        DARK_BLUE,
        SkyblockIcon.CRIT_DAMAGE,
        hypixelId = "CRITICAL_DAMAGE",
    ),
    BONUS_ATTACK_SPEED(
        YELLOW,
        SkyblockIcon.ATTACK_SPEED,
        displayName = "Attack Speed",
        hypixelId = "ATTACK_SPEED",
    ),
    FEROCITY(RED, SkyblockIcon.FEROCITY),
    SWING_RANGE(YELLOW, SkyblockIcon.SWING_RANGE),
    INTELLIGENCE(AQUA, SkyblockIcon.INTELLIGENCE), // TODO get from action bar
    ABILITY_DAMAGE(
        RED,
        SkyblockIcon.ABILITY_DAMAGE,
        hypixelId = "ABILITY_DAMAGE_PERCENT",
    ),
    HEALTH_REGEN(
        RED,
        SkyblockIcon.HEALTH_REGEN,
        hypixelId = "HEALTH_REGENERATION",
    ),
    VITALITY(DARK_RED, SkyblockIcon.VITALITY), // TODO get from action bar
    MENDING(GREEN, SkyblockIcon.MENDING),

    // <editor-fold desc="Mining Stats">
    BREAKING_POWER(DARK_GREEN, SkyblockIcon.BREAKING_POWER),
    MINING_SPEED(GOLD, SkyblockIcon.MINING_SPEED),
    MINING_SPREAD(YELLOW, SkyblockIcon.MINING_SPREAD),
    GEMSTONE_SPREAD(YELLOW, SkyblockIcon.GEMSTONE_SPREAD),
    PRISTINE(DARK_PURPLE, SkyblockIcon.PRISTINE),
    MINING_FORTUNE(GOLD, SkyblockIcon.MINING_FORTUNE),
    ORE_FORTUNE(GOLD, SkyblockIcon.MINING_FORTUNE),
    BLOCK_FORTUNE(GOLD, SkyblockIcon.MINING_FORTUNE),
    DWARVEN_METAL_FORTUNE(GOLD, SkyblockIcon.MINING_FORTUNE),
    GEMSTONE_FORTUNE(GOLD, SkyblockIcon.MINING_FORTUNE),
    // </editor-fold>

    // <editor-fold desc="Farming Stats">
    BONUS_PEST_CHANCE(DARK_GREEN, SkyblockIcon.BONUS_PEST_CHANCE),
    OVERBLOOM(YELLOW, SkyblockIcon.OVERBLOOM),
    FARMING_FORTUNE(GOLD, SkyblockIcon.FARMING_FORTUNE),
    WHEAT_FORTUNE(GOLD, SkyblockIcon.WHEAT_FORTUNE),
    CARROT_FORTUNE(GOLD, SkyblockIcon.CARROT_FORTUNE),
    POTATO_FORTUNE(GOLD, SkyblockIcon.POTATO_FORTUNE),
    PUMPKIN_FORTUNE(GOLD, SkyblockIcon.PUMPKIN_FORTUNE),
    SUGAR_CANE_FORTUNE(GOLD, SkyblockIcon.SUGAR_CANE_FORTUNE),
    MELON_FORTUNE(
        GOLD,
        SkyblockIcon.MELON_FORTUNE,
        displayName = "Melon Slice Fortune",
    ),
    CACTUS_FORTUNE(GOLD, SkyblockIcon.CACTUS_FORTUNE),
    COCOA_BEANS_FORTUNE(GOLD, SkyblockIcon.COCOA_BEANS_FORTUNE),
    MUSHROOM_FORTUNE(GOLD, SkyblockIcon.MUSHROOM_FORTUNE),
    NETHER_STALK_FORTUNE(
        GOLD,
        SkyblockIcon.NETHER_STALK_FORTUNE,
        displayName = "Nether Wart Fortune",
    ),
    SUNFLOWER_FORTUNE(GOLD, SkyblockIcon.SUNFLOWER_FORTUNE),
    MOONFLOWER_FORTUNE(GOLD, SkyblockIcon.MOONFLOWER_FORTUNE),
    WILD_ROSE_FORTUNE(GOLD, SkyblockIcon.WILD_ROSE_FORTUNE),
    // </editor-fold>

    // <editor-fold desc="Foraging Stats">
    SWEEP(DARK_GREEN, SkyblockIcon.SWEEP),
    FORAGING_FORTUNE(GOLD, SkyblockIcon.FORAGING_FORTUNE),
    FIG_FORTUNE(GOLD, SkyblockIcon.FORAGING_FORTUNE),
    MANGROVE_FORTUNE(GOLD, SkyblockIcon.FORAGING_FORTUNE),
    HELIX_FORTUNE(GOLD, SkyblockIcon.FORAGING_FORTUNE),
    TIMER(DARK_RED, SkyblockIcon.TIMBER),
    // </editor-fold>

    // <editor-fold desc="Fishing Stats">
    FISHING_SPEED(AQUA, SkyblockIcon.FISHING_SPEED),
    SEA_CREATURE_CHANCE(DARK_AQUA, SkyblockIcon.SEA_CREATURE_CHANCE),
    DOUBLE_HOOK_CHANCE(BLUE, SkyblockIcon.DOUBLE_HOOK_CHANCE),
    TROPHY_FISH_CHANCE(
        GOLD,
        SkyblockIcon.TROPHY_FISH_CHANCE,
        displayName = "Trophy Chance",
    ),
    TREASURE_CHANCE(GOLD, SkyblockIcon.TREASURE_CHANCE),
    // </editor-fold>

    // <editor-fold desc="Miscellaneous Stats">
    // TODO get from Minecraft walk speed attribute (500+ Speed works fine now)
    SPEED(WHITE, SkyblockIcon.SPEED, hypixelId = "WALK_SPEED"),
    MAGIC_FIND(AQUA, SkyblockIcon.MAGIC_FIND),
    PET_LUCK(LIGHT_PURPLE, SkyblockIcon.PET_LUCK),
    HEAT_RESISTANCE(RED, SkyblockIcon.HEAT_RESISTANCE),
    COLD_RESISTANCE(AQUA, SkyblockIcon.COLD_RESISTANCE),
    RESPIRATION(DARK_AQUA, SkyblockIcon.RESPIRATION),
    PRESSURE_RESISTANCE(BLUE, SkyblockIcon.PRESSURE_RESISTANCE),
    FEAR(DARK_PURPLE, SkyblockIcon.FEAR),
    TRACKING(LIGHT_PURPLE, SkyblockIcon.TRACKING),
    // </editor-fold>

    // <editor-fold desc="Hunting Stats">
    PULL(AQUA, SkyblockIcon.PULL),
    HUNTING_FORTUNE(LIGHT_PURPLE, SkyblockIcon.HUNTING_FORTUNE),
    CHARM_CHANCE(AQUA, SkyblockIcon.CHARM_CHANCE),
    // </editor-fold>

    // <editor-fold desc="Wisdom Stats">
    COMBAT_WISDOM(DARK_AQUA, SkyblockIcon.COMBAT_WISDOM),
    FARMING_WISDOM(DARK_AQUA, SkyblockIcon.FARMING_WISDOM),
    FISHING_WISDOM(DARK_AQUA, SkyblockIcon.FISHING_WISDOM),
    MINING_WISDOM(DARK_AQUA, SkyblockIcon.MINING_WISDOM),
    FORAGING_WISDOM(DARK_AQUA, SkyblockIcon.FORAGING_WISDOM),
    ENCHANTING_WISDOM(DARK_AQUA, SkyblockIcon.ENCHANTING_WISDOM),
    ALCHEMY_WISDOM(DARK_AQUA, SkyblockIcon.ALCHEMY_WISDOM),
    CARPENTRY_WISDOM(DARK_AQUA, SkyblockIcon.CARPENTRY_WISDOM),
    RUNECRAFTING_WISDOM(DARK_AQUA, SkyblockIcon.RUNECRAFTING_WISDOM),
    TAMING_WISDOM(DARK_AQUA, SkyblockIcon.TAMING_WISDOM),
    SOCIAL_WISDOM(DARK_AQUA, SkyblockIcon.SOCIAL_WISDOM),
    HUNTING_WISDOM(DARK_AQUA, SkyblockIcon.HUNTING_WISDOM),
    // </editor-fold>

    // <editor-fold desc="Rift Stats">
    RIFT_TIME(GREEN, SkyblockIcon.RIFT_TIME),
    RIFT_DAMAGE(DARK_PURPLE, SkyblockIcon.RIFT_DAMAGE),

    // TODO get from Minecraft walk speed attribute
    RIFT_SPEED(
        WHITE,
        SkyblockIcon.SPEED,
        displayName = "Speed",
        hypixelId = "RIFT_WALK_SPEED",
    ),
    RIFT_INTELLIGENCE(
        AQUA,
        SkyblockIcon.INTELLIGENCE,
        displayName = "Intelligence",
    ),

    // MAGIC_FIND is just the overworld stat
    MANA_REGEN(AQUA, SkyblockIcon.MANA_REGEN),
    HEARTS(RED, SkyblockIcon.HEARTS),
    // </editor-fold>

    UNKNOWN(GRAY, SkyblockIcon.QUESTION_MARK, generatePatterns = false),
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
            if (!UtilsPatterns.skyblockMenuInventory.isInside()) return
            val list = event.inventoryItems[PLAYER_STATS_SLOT_INDEX]?.getCleanLore() ?: return
            DelayedRun.runNextTick { // Delayed to not impact opening time
                assignEntry(list, StatSourceType.SKYBLOCK_MENU) { it.menuPattern }
            }
        }

        private val statsMenuRelevantSlotIndexes = listOf(14, 15, 16, 23, 24, 25, 32, 33, 34)

        private fun onStatsMenu(event: InventoryFullyOpenedEvent) {
            if (!CurrentEquipmentApi.inventory.isInside()) return
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
            event.move(141, "#profile.stats.HUNTER_FORTUNE", "#profile.stats.HUNTING_FORTUNE")
            // Stats are stored under their lowercase name, so none of the renames above ever matched anything
            event.move(142, "#profile.stats.true_defence", "#profile.stats.true_defense")
            event.move(142, "#profile.stats.nether_wart_fortune", "#profile.stats.nether_stalk_fortune")
            event.move(142, "#profile.stats.hunter_fortune", "#profile.stats.hunting_fortune")
            // Left behind by stats that were read back while their rename was still missing
            event.remove(142, "#profile.stats.unknown")
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
