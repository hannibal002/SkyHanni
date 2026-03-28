package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ResourcePackReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.compat.createResourceLocation
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import org.intellij.lang.annotations.Language
import java.util.regex.Pattern
import kotlin.math.roundToInt

@Language("RegExp")
private const val VALUE_PATTERN = "(?<value>[\\d,.]+)(?: .*)?"

@Suppress("MaxLineLength")
enum class SkyblockStat(
    val hypixelIcon: String,
    @Language("RegExp") tabListPatternS: String,
    @Language("RegExp") menuPatternS: String,
    private val hypxelId: String? = null,
) {
    DAMAGE("§c❁", "", ""), // Weapon only
    HEALTH("§c❤", " *Health: ❤$VALUE_PATTERN", " *§c❤ Health §f$VALUE_PATTERN"), // TODO get from action bar
    DEFENSE("§a❈", " *Defense: ❈$VALUE_PATTERN", " *§a❈ Defense §f$VALUE_PATTERN"), // TODO get from action bar
    STRENGTH("§c❁", " *Strength: ❁$VALUE_PATTERN", " *§c❁ Strength §f$VALUE_PATTERN"),
    INTELLIGENCE(
        "§b✎",
        " *Intelligence: ✎$VALUE_PATTERN",
        " *§b✎ Intelligence §f$VALUE_PATTERN",
    ), // TODO get from action bar
    CRIT_DAMAGE(
        "§9☠", " *Crit Damage: ☠$VALUE_PATTERN", " *§9☠ Crit Damage §f$VALUE_PATTERN",
        hypxelId = "CRITICAL_DAMAGE",
    ),
    CRIT_CHANCE(
        "§9☣", " *Crit Chance: ☣$VALUE_PATTERN", " *§9☣ Crit Chance §f$VALUE_PATTERN",
        hypxelId = "CRITICAL_CHANCE",
    ),
    FEROCITY("§c⫽", " *Ferocity: ⫽$VALUE_PATTERN", " *§c⫽ Ferocity §f$VALUE_PATTERN"),
    BONUS_ATTACK_SPEED(
        "§e⚔",
        " *Attack Speed: ⚔$VALUE_PATTERN",
        " *§e⚔ Bonus Attack Speed §f$VALUE_PATTERN",
        hypxelId = "ATTACK_SPEED",
    ),
    ABILITY_DAMAGE(
        "§c๑", " *Ability Damage: ๑$VALUE_PATTERN", " *§c๑ Ability Damage §f$VALUE_PATTERN",
        hypxelId = "ABILITY_DAMAGE_PERCENT",
    ),
    HEALTH_REGEN(
        "§c❣",
        " *Health Regen: ❣$VALUE_PATTERN",
        " *§c❣ Health Regen §f$VALUE_PATTERN",
        "HEALTH_REGENERATION",
    ),
    VITALITY("§4♨", " *Vitality: ♨$VALUE_PATTERN", " *§4♨ Vitality §f$VALUE_PATTERN"),
    MENDING("§a☄", " *Mending: ☄$VALUE_PATTERN", " *§a☄ Mending §f$VALUE_PATTERN"),
    TRUE_DEFENSE("§f❂", " *True Defense: ❂$VALUE_PATTERN", " *§f❂ True Defense §f$VALUE_PATTERN"),
    SWING_RANGE("§eⓈ", " *Swing Range: Ⓢ$VALUE_PATTERN", " *§eⓈ Swing Range §f$VALUE_PATTERN"),

    // TODO add the way sba did get it (be careful with 500+ Speed)
    SPEED(
        "§f✦", " *Speed: ✦$VALUE_PATTERN", " *§f✦ Speed §f$VALUE_PATTERN",
        hypxelId = "WALK_SPEED",
    ),
    SEA_CREATURE_CHANCE("§3α", " *Sea Creature Chance: α$VALUE_PATTERN", " *§3α Sea Creature Chance §f$VALUE_PATTERN"),
    MAGIC_FIND("§b✯", " *Magic Find: ✯$VALUE_PATTERN", " *§b✯ Magic Find §f$VALUE_PATTERN"),
    PET_LUCK("§d♣", " *Pet Luck: ♣$VALUE_PATTERN", " *§d♣ Pet Luck §f$VALUE_PATTERN"),
    FISHING_SPEED("§b☂", " *Fishing Speed: ☂$VALUE_PATTERN", " *§b☂ Fishing Speed §f$VALUE_PATTERN"),
    TROPHY_FISH_CHANCE("§6♔", "Trophy Fish Chance: ♔$VALUE_PATTERN", " *§6♔ Trophy Fish Chance §f(?<value>\\d+)%"),
    DOUBLE_HOOK_CHANCE(
        "§9⚓",
        " *Double Hook Chance: ⚓$VALUE_PATTERN",
        " *§9⚓ Double Hook Chance §f(?<value>\\d+(?:\\.\\d+)?)%",
    ),
    TREASURE_CHANCE("§6⛃", " *Treasure Chance: ⛃(?<value>\\d+(?:\\.\\d+)?)", " *§6⛃ Treasure Chance §f(?<value>\\d+(?:\\.\\d+)?)%"),
    BONUS_PEST_CHANCE(
        "§2ൠ",
        " *(?:§r§7§m)?Bonus Pest Chance: ൠ$VALUE_PATTERN",
        " *(?:§7§m|§2)ൠ Bonus Pest Chance (?:§f)?$VALUE_PATTERN",
    ),
    COMBAT_WISDOM("§3☯", " *Combat Wisdom: ☯$VALUE_PATTERN", " *§3☯ Combat Wisdom §f$VALUE_PATTERN"),
    MINING_WISDOM("§3☯", " *Mining Wisdom: ☯$VALUE_PATTERN", " *§3☯ Mining Wisdom §f$VALUE_PATTERN"),
    FARMING_WISDOM("§3☯", " *Farming Wisdom: ☯$VALUE_PATTERN", " *§3☯ Farming Wisdom §f$VALUE_PATTERN"),
    FORAGING_WISDOM("§3☯", " *Foraging Wisdom: ☯$VALUE_PATTERN", " *§3☯ Foraging Wisdom §f$VALUE_PATTERN"),
    FISHING_WISDOM("§3☯", " *Fishing Wisdom: ☯$VALUE_PATTERN", " *§3☯ Fishing Wisdom §f$VALUE_PATTERN"),
    ENCHANTING_WISDOM("§3☯", " *Enchanting Wisdom: ☯$VALUE_PATTERN", " *§3☯ Enchanting Wisdom §f$VALUE_PATTERN"),
    ALCHEMY_WISDOM("§3☯", " *Alchemy Wisdom: ☯$VALUE_PATTERN", " *§3☯ Alchemy Wisdom §f$VALUE_PATTERN"),
    CARPENTRY_WISDOM("§3☯", " *Carpentry Wisdom: ☯$VALUE_PATTERN", " *§3☯ Carpentry Wisdom §f$VALUE_PATTERN"),
    RUNECRAFTING_WISDOM("§3☯", " *Runecrafting Wisdom: ☯$VALUE_PATTERN", " *§3☯ Runecrafting Wisdom §f$VALUE_PATTERN"),
    SOCIAL_WISDOM("§3☯", " *Social Wisdom: ☯$VALUE_PATTERN", " *§3☯ Social Wisdom §f$VALUE_PATTERN"),
    TAMING_WISDOM("§3☯", " *Taming Wisdom: ☯$VALUE_PATTERN", " *§3☯ Taming Wisdom §f$VALUE_PATTERN"),
    HUNTING_WISDOM("§3☯", " *Hunting Wisdom: ☯$VALUE_PATTERN", " *§3☯ Hunting Wisdom §f$VALUE_PATTERN"),

    MINING_SPEED("§6⸕", " *Mining Speed: ⸕$VALUE_PATTERN", " *§6⸕ Mining Speed §f$VALUE_PATTERN"),
    BREAKING_POWER("§2Ⓟ", "", " *§2Ⓟ Breaking Power §f$VALUE_PATTERN"),
    PRISTINE("§5✧", " *Pristine: ✧$VALUE_PATTERN", " *§5✧ Pristine §f$VALUE_PATTERN"),
    FORAGING_FORTUNE("§6☘", " *Foraging Fortune: ☘$VALUE_PATTERN", " *§6☘ Foraging Fortune §f$VALUE_PATTERN"),
    FARMING_FORTUNE(
        "§6☘",
        " *Farming Fortune: ☘$VALUE_PATTERN",
        " *(?:§7§m|§6)☘ Farming Fortune (?:§f)?$VALUE_PATTERN",
    ),
    MINING_FORTUNE("§6☘", " *Mining Fortune: ☘$VALUE_PATTERN", " *§6☘ Mining Fortune §f$VALUE_PATTERN"),
    FEAR("§5☠", " *Fear: ☠$VALUE_PATTERN", " *§5☠ Fear §f$VALUE_PATTERN"),
    COLD_RESISTANCE("§b❄", " *Cold Resistance: ❄$VALUE_PATTERN", ""),
    WHEAT_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Wheat Fortune $VALUE_PATTERN"),
    CARROT_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Carrot Fortune $VALUE_PATTERN"),
    POTATO_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Potato Fortune $VALUE_PATTERN"),
    PUMPKIN_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Pumpkin Fortune $VALUE_PATTERN"),
    MELON_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Melon Slice Fortune $VALUE_PATTERN"),
    MUSHROOM_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Mushroom Fortune $VALUE_PATTERN"),
    CACTUS_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Cactus Fortune $VALUE_PATTERN"),
    NETHER_STALK_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Nether Wart Fortune $VALUE_PATTERN"),
    COCOA_BEANS_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Cocoa Beans Fortune $VALUE_PATTERN"),
    SUGAR_CANE_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Sugar Cane Fortune $VALUE_PATTERN"),
    SUNFLOWER_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Sunflower Fortune $VALUE_PATTERN"),
    MOONFLOWER_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Moonflower Fortune $VALUE_PATTERN"),
    WILD_ROSE_FORTUNE("§6☘", "", " *(?:§7§m|§6)☘ Wild Rose Fortune $VALUE_PATTERN"),

    MINING_SPREAD(
        "§e▚",
        " *Mining Spread: ▚$VALUE_PATTERN",
        " *(§7§m|§e)▚ Mining Spread (§f)?$VALUE_PATTERN",
    ),
    GEMSTONE_SPREAD(
        "§e▚",
        " *Mining Spread: ▚$VALUE_PATTERN",
        " *(§7§m|§e)▚ Gemstone Spread (§f)?$VALUE_PATTERN",
    ),
    ORE_FORTUNE("§6☘", " *Ore Fortune: ☘$VALUE_PATTERN", " *§6☘ Ore Fortune §f103"),
    DWARVEN_METAL_FORTUNE(
        "§6☘",
        " *Dwarven Metal Fortune: ☘$VALUE_PATTERN",
        " *§6☘ Dwarven Metal Fortune §f$VALUE_PATTERN",
    ),
    BLOCK_FORTUNE("§6☘", " *Block Fortune: ☘$VALUE_PATTERN", " *§6☘ Block Fortune §f$VALUE_PATTERN"),
    GEMSTONE_FORTUNE("§6☘", " *Gemstone Fortune: ☘$VALUE_PATTERN", " *§6☘ Gemstone Fortune §f$VALUE_PATTERN"),
    HEAT_RESISTANCE("§c♨", " *Heat Resistance: ♨$VALUE_PATTERN", " *§c♨ Heat Resistance §f$VALUE_PATTERN"),

    SWEEP("§2∮", " *Sweep: ∮$VALUE_PATTERN", " *§2∮ Sweep §f$VALUE_PATTERN"),
    RESPIRATION("§3⚶", " *Respiration: ⚶$VALUE_PATTERN", " *§3⚶ Respiration §f$VALUE_PATTERN"),
    PRESSURE_RESISTANCE("§9❍", " *Pressure Resistance: ❍$VALUE_PATTERN", " *§9❍ Pressure Resistance §f$VALUE_PATTERN"),
    PULL("§bᛷ", " *Pull: ᛷ$VALUE_PATTERN", " *§bᛷ Pull §f$VALUE_PATTERN"),
    HUNTER_FORTUNE("§d☘", " *Hunter Fortune: ☘$VALUE_PATTERN", " *§d☘ Hunter Fortune §f$VALUE_PATTERN"),
    FIG_FORTUNE("§6☘", " *Fig Fortune: ☘$VALUE_PATTERN", " *§6☘ Fig Fortune §f$VALUE_PATTERN"),
    MANGROVE_FORTUNE("§6☘", " *Mangrove Fortune: ☘$VALUE_PATTERN", " *§6☘ Mangrove Fortune §f$VALUE_PATTERN"),

    RIFT_TIME("§aф", " *Rift Time: ф$VALUE_PATTERN", " *§aф Rift Time §f$VALUE_PATTERN"),
    RIFT_DAMAGE("§5❁", " *Rift Damage: ❁$VALUE_PATTERN", " *§5❁ Rift Damage §f$VALUE_PATTERN"),
    MANA_REGEN("§b⚡", " *Mana Regen: ⚡$VALUE_PATTERN", " *§b⚡ Mana Regen §f$VALUE_PATTERN"),
    HEARTS("§c♥", " *Hearts: ♥$VALUE_PATTERN", " *§c♥ Hearts §f$VALUE_PATTERN"),

    TRACKING("§d❃", " *Tracking: ❃$VALUE_PATTERN", " *§d❃ Tracking §f$VALUE_PATTERN"),

    UNKNOWN("§c?", "", "")
    ;

    var lastKnownValue: Double?
        get() = ProfileStorageData.profileSpecific?.stats?.get(this)
        set(value) {
            ProfileStorageData.profileSpecific?.stats?.set(this, value)
        }

    @Suppress("UNNECESSARY_SAFE_CALL")
    val icon: String
        get() = resourcePackOverrides?.get(name) ?: hypixelIcon

    var lastSource: StatSourceType = StatSourceType.UNKNOWN

    var lastAssignment: SimpleTimeMark = SimpleTimeMark.farPast()

    private val capitalizedName = name.lowercase().allLettersFirstUppercase()

    val iconWithName
        get() = "$icon $capitalizedName"

    private val keyName = name.lowercase().replace('_', '.')

    val displayValue get() = lastKnownValue?.let { icon + it.roundToInt() }

    val tablistPattern by RepoPattern.pattern("stats.tablist.no-color.$keyName", tabListPatternS)
    val menuPattern by RepoPattern.pattern("stats.menu.$keyName", menuPatternS)

    fun asString(value: Int) = (if (value > 0) "+" else "") + value.toString() + " " + this.icon

    @SkyHanniModule
    companion object {

        val fontSizeOfLargestIcon by lazy {
            entries.maxOf { Minecraft.getInstance().font.width(it.icon) } + 1
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

        fun getIconOrNull(string: String): String? = resourcePackOverrides[string] ?: getValueOrNull(string)?.icon

        private var resourcePackOverrides = emptyMap<String, String>()

        @HandleEvent
        fun onResourcePackLoad(event: ResourcePackReloadEvent) {
            val packOverrides = event.getJsonResource<Map<String, String>>(
                createResourceLocation("skyhanni", "icon_overrides.json"),
            )

            resourcePackOverrides = packOverrides.orEmpty()
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
