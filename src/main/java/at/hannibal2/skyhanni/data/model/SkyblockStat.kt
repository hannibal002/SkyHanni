package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.intellij.lang.annotations.Language
import java.util.EnumMap
import java.util.regex.Pattern
import kotlin.math.roundToInt

enum class SkyblockStat(
    val icon: String,
    @Language("RegExp") tabListPatternS: String,
    @Language("RegExp") menuPatternS: String,
) {
    DAMAGE("§c❁", "", ""),  // Weapon only
    HEALTH("§c❤", " Health: §r§c❤(?<value>.*)", " §c❤ Health §f(?<value>.*)"),
    DEFENSE("§a❈", " Defense: §r§a❈(?<value>.*)", " §a❈ Defense §f(?<value>.*)"),
    STRENGTH("§c❁", " Strength: §r§c❁(?<value>.*)", " §c❁ Strength §f(?<value>.*)"),
    INTELLIGENCE("§b✎", " Intelligence: §r§b✎(?<value>.*)", " §b✎ Intelligence §f(?<value>.*)"),
    CRIT_DAMAGE("§9☠", " Crit Damage: §r§9☠(?<value>.*)", " §9☠ Crit Damage §f(?<value>.*)"),
    CRIT_CHANCE("§9☣", " Crit Chance: §r§9☣(?<value>.*)", " §9☣ Crit Chance §f(?<value>.*)"),
    FEROCITY("§c⫽", " Ferocity: §r§c⫽(?<value>.*)", " §c⫽ Ferocity §f(?<value>.*)"),
    BONUS_ATTACK_SPEED("§e⚔", " Attack Speed: §r§e⚔(?<value>.*)", " §e⚔ Bonus Attack Speed §f(?<value>.*)"),
    ABILITY_DAMAGE("§c๑", " Ability Damage: §r§c๑(?<value>.*)", " §c๑ Ability Damage §f(?<value>.*)"),
    HEALTH_REGEN("§c❣", " Health Regen: §r§c❣(?<value>.*)", " §c❣ Health Regen §f(?<value>.*)"),
    VITALITY("§4♨", " Vitality: §r§4♨(?<value>.*)", " §4♨ Vitality §f(?<value>.*)"),
    MENDING("§a☄", " Mending: §r§a☄(?<value>.*)", " §a☄ Mending §f(?<value>.*)"),
    TRUE_DEFENCE("§7❂", " True Defense: §r§f❂(?<value>.*)", " §f❂ True Defense §f(?<value>.*)"),
    SWING_RANGE("§eⓈ", " Swing Range: §r§eⓈ(?<value>.*)", " §eⓈ Swing Range §f(?<value>.*)"),
    SPEED("§f✦", " Speed: §r§f✦(?<value>.*)", " §f✦ Speed §f(?<value>.*)"),
    SEA_CREATURE_CHANCE("§3α", " Sea Creature Chance: §r§3α(?<value>.*)", " §3α Sea Creature Chance §f(?<value>.*)"),
    MAGIC_FIND("§b✯", " Magic Find: §r§b✯(?<value>.*)", " §b✯ Magic Find §f(?<value>.*)"),
    PET_LUCK("§d♣", " Pet Luck: §r§d♣(?<value>.*)", " §d♣ Pet Luck §f(?<value>.*)"),
    FISHING_SPEED("§b☂", " Fishing Speed: §r§b☂(?<value>.*)", " §b☂ Fishing Speed §f(?<value>.*)"),
    DOUBLE_HOOK_CHANCE("§9⚓", " Double Hook Chance: §r§9⚓(?<value>.*)", ""),
    BONUS_PEST_CHANCE("§2ൠ", " Bonus Pest Chance: §r§2ൠ(?<value>.*)", " §2ൠ Bonus Pest Chance §f(?<value>.*)"),
    COMBAT_WISDOM("§3☯", " Combat Wisdom: §r§3☯(?<value>.*)", " §3☯ Combat Wisdom §f(?<value>.*)"),
    MINING_WISDOM("§3☯", " Mining Wisdom: §r§3☯(?<value>.*)", " §3☯ Mining Wisdom §f(?<value>.*)"),
    FARMING_WISDOM("§3☯", " Farming Wisdom: §r§3☯(?<value>.*)", " §3☯ Farming Wisdom §f(?<value>.*)"),
    FORAGING_WISDOM("§3☯", " Foraging Wisdom: §r§3☯(?<value>.*)", " §3☯ Foraging Wisdom §f(?<value>.*)"),
    FISHING_WISDOM("§3☯", " Fishing Wisdom: §r§3☯(?<value>.*)", " §3☯ Fishing Wisdom §f(?<value>.*)"),
    ENCHANTING_WISDOM("§3☯", " Enchanting Wisdom: §r§3☯(?<value>.*)", " §3☯ Enchanting Wisdom §f(?<value>.*)"),
    ALCHEMY_WISDOM("§3☯", " Alchemy Wisdom: §r§3☯(?<value>.*)", " §3☯ Alchemy Wisdom §f(?<value>.*)"),
    CARPENTRY_WISDOM("§3☯", " Carpentry Wisdom: §r§3☯(?<value>.*)", " §3☯ Carpentry Wisdom §f(?<value>.*)"),
    RUNECRAFTING_WISDOM("§3☯", " Runecrafting Wisdom: §r§3☯(?<value>.*)", " §3☯ Runecrafting Wisdom §f(?<value>.*)"),
    SOCIAL_WISDOM("§3☯", " Social Wisdom: §r§3☯(?<value>.*)", " §3☯ Social Wisdom §f(?<value>.*)"),
    TAMING_WISDOM("§3☯", " Taming Wisdom: §r§3☯(?<value>.*)", " §3☯ Taming Wisdom §f(?<value>.*)"),
    MINING_SPEED("§6⸕", " Mining Speed: §r§6⸕(?<value>.*)", " §6⸕ Mining Speed §f(?<value>.*)"),
    BREAKING_POWER("§2Ⓟ", "", " §2Ⓟ Breaking Power §f(?<value>.*)"),
    PRISTINE("§5✧", " Pristine: §r§5✧(?<value>.*)", " §5✧ Pristine §f(?<value>.*)"),
    FORAGING_FORTUNE("§☘", " Foraging Fortune: §r§6☘(?<value>.*)", " §6☘ Foraging Fortune §f(?<value>.*)"),
    FARMING_FORTUNE("§6☘", " §r§7(?:§m)?Farming Fortune: ☘(?<value>.*)", " §7(?:§m)☘ Farming Fortune (?<value>.*)"),
    MINING_FORTUNE("§6☘", " Mining Fortune: §r§6☘(?<value>.*)", " §6☘ Mining Fortune §f(?<value>.*)"),
    FEAR("§a☠", "", ""), // Skyblock does not like fear. It only shows during Great Spook, therefore no Data.
    COLD_RESISTANCE("§b❄", " Cold Resistance: §r§b❄(?<value>.*)", ""),
    WHEAT_FORTUNE("§7☘", "", " §7(?:§m)☘ Wheat Fortune (?<value>.*)"),
    CARROT_FORTUNE("§7☘", "", " §7(?:§m)☘ Carrot Fortune (?<value>.*)"),
    POTATO_FORTUNE("§7☘", "", " §7(?:§m)☘ Potato Fortune (?<value>.*)"),
    PUMPKIN_FORTUNE("§7☘", "", " §7(?:§m)☘ Pumpkin Fortune (?<value>.*)"),
    MELON_FORTUNE("§7☘", "", " §7(?:§m)☘ Melon Fortune (?<value>.*)"),
    MUSHROOM_FORTUNE("§7☘", "", " §7(?:§m)☘ Mushroom Fortune (?<value>.*)"),
    CACTUS_FORTUNE("§7☘", "", " §7(?:§m)☘ Cactus Fortune (?<value>.*)"),
    NETHER_WART_FORTUNE("§7☘", "", " §7(?:§m)☘ Nether Wart Fortune (?<value>.*)"),
    COCOA_BEANS_FORTUNE("§7☘", "", " §7(?:§m)☘ Cocoa Beans Fortune (?<value>.*)"),
    ;

    var lastKnowValue: Double
        get() = ProfileStorageData.profileSpecific?.stats?.get(this) ?: 0.0
        set(value) {
            ProfileStorageData.profileSpecific?.stats?.set(this, value)
        }

    var lastSource: StatSourceType = StatSourceType.UNKNOWN

    val capitalizedName = name.lowercase().allLettersFirstUppercase()

    val iconWithName = "$icon $capitalizedName"

    val keyName = name.lowercase().replace('_', '.')

    val displayValue get() = icon + lastKnowValue.roundToInt()

    val tablistPattern by RepoPattern.pattern("stats.tablist.$keyName", tabListPatternS)
    val menuPattern by RepoPattern.pattern("stats.menu.$keyName", menuPatternS)

    fun asString(value: Int) = (if (value > 0) "+" else "") + value.toString() + " " + this.icon

    @SkyHanniModule
    companion object {

        val fontSizeOfLargestIcon by lazy {
            entries.maxOf { Minecraft.getMinecraft().fontRendererObj.getStringWidth(it.icon) } + 1
        }

        init {
            entries.forEach {
                it.tablistPattern
                it.menuPattern
            }
        }

        @SubscribeEvent
        fun onInventory(event: InventoryFullyOpenedEvent) {
            if (!LorenzUtils.inSkyBlock) return
            onSkyblockMenu(event)
            onStatsMenu(event)
        }

        private const val PLAYER_STATS_SLOT_INDEX = 13

        private fun onSkyblockMenu(event: InventoryFullyOpenedEvent) {
            if (event.inventoryName != "SkyBlock Menu") return
            val list = event.inventoryItems[PLAYER_STATS_SLOT_INDEX]?.getLore() ?: return
            DelayedRun.runNextTick {  // Delayed to not impact opening time
                assignEntry(list, StatSourceType.SKYBLOCK_MENU) { it.menuPattern }
            }
        }

        private val statsMenuRelevantSlotIndexes = listOf(15, 16, 24, 25)

        private fun onStatsMenu(event: InventoryFullyOpenedEvent) {
            if (event.inventoryName != "Your Equipment and Stats") return
            val list = statsMenuRelevantSlotIndexes.mapNotNull { event.inventoryItems[it]?.getLore() }.flatten()
            if (list.isEmpty()) return
            DelayedRun.runNextTick {  // Delayed to not impact opening time
                assignEntry(list, StatSourceType.STATS_MENU) { it.menuPattern }
            }
        }

        @SubscribeEvent
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
                entry.lastKnowValue = matchResult
                entry.lastSource = type
                break // Exit the inner loop once a match is found
            }
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
