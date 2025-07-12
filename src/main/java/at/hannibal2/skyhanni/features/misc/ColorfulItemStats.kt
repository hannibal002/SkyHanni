package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.item.ItemHoverEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ColorfulItemStats {
    private val config get() = SkyHanniMod.feature.misc

    private val group = RepoPattern.group("misc.itemstats")

    /**
     * REGEX-TEST: §7Crit Chance: §c+30%
     * REGEX-TEST: §7Magic Find: §a+54.52
     */
    private val genericStat by group.pattern(
        "generic",
        "§7(?<Stat>[a-zA-Z ]+): (?<OldColor>§[0-9a-f])(?<Bonus>[-+]?[\\d.%]+)",
    )

    private val colorStatMap = mapOf(
        "Gear Score" to "§d",
        "Damage" to "§c",
        "Shot Cooldown" to "§a",
        "Health" to "§c",
        "Defense" to "§a",
        "Strength" to "§c",
        "Intelligence" to "§b",
        "Crit Chance" to "§9",
        "Crit Damage" to "§9",
        "Bonus Attack Speed" to "§e",
        "Ability Damage" to "§c",
        "True Defense" to "§f",
        "Ferocity" to "§c",
        "Health Regen" to "§c",
        "Vitality" to "§4",
        "Mending" to "§a",
        "Swing Range" to "§e",
        "Mining Speed" to "§6",
        "Mining Spread" to "§e",
        "Gemstone Spread" to "§e",
        "Pristine" to "§5",
        "Mining Fortune" to "§6",
        "Ore Fortune" to "§6",
        "Block Fortune" to "§6",
        "Dwarven Metal Fortune" to "§6",
        "Gemstone Fortune" to "§6",
        "Foraging Fortune" to "§6",
        "Farming Fortune" to "§6",
        "Wheat Fortune" to "§6",
        "Carrot Fortune" to "§6",
        "Potato Fortune" to "§6",
        "Pumpkin Fortune" to "§6",
        "Melon Fortune" to "§6",
        "Mushroom Fortune" to "§6",
        "Cactus Fortune" to "§6",
        "Sugar Cane Fortune" to "§6",
        "Fig Fortune" to "§6",
        "Mangrove Fortune" to "§6",
        "Hunter Fortune" to "§d",
        "Sweep" to "§2",
        "Nether Wart Fortune" to "§6",
        "Cocoa Beans Fortune" to "§6",
        "Combat Wisdom" to "§3",
        "Mining Wisdom" to "§3",
        "Farming Wisdom" to "§3",
        "Foraging Wisdom" to "§3",
        "Fishing Wisdom" to "§3",
        "Enchanting Wisdom" to "§3",
        "Alchemy Wisdom" to "§3",
        "Carpentry Wisdom" to "§3",
        "Runecrafting Wisdom" to "§3",
        "Social Wisdom" to "§3",
        "Taming Wisdom" to "§3",
        "Hunting Wisdom" to "§3",
        "Speed" to "§f",
        "Magic Find" to "§b",
        "Pet Luck" to "§d",
        "Bonus Pest Chance" to "§2",
        "Heat Resistance" to "§c",
        "Cold Resistance" to "§b",
        "Fear" to "§5",
        "Pull" to "§b",
        "Respiration" to "§3",
        "Pressure Resistance" to "§9",
        "Fishing Speed" to "§b",
        "Sea Creature Chance" to "§3",
        "Double Hook Chance" to "§9",
        "Trophy Fish Chance" to "§6",
        "Treasure Chance" to "§6",
    )

    private val iconStatMap = mapOf(
        "Magic Find" to "✯",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltipEvent(event: ItemHoverEvent) {
        if (!config.colorfulItemTooltips) return

        for ((index, line) in event.toolTip.withIndex()) {

            event.toolTip[index] = genericStat.replace(line) {

                val stat = group("Stat")
                buildString {
                    "§7$stat: "
                    colorStatMap[stat] ?: group("OldColor")
                    group("Bonus")
                    iconStatMap[stat].orEmpty()
                    group("OldColor")
                }
            }
        }
    }
}
