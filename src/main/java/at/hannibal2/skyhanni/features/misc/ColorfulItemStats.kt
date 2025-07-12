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
        "Speed" to "§f",
        "Strength" to "§c",
        "Intelligence" to "§b",
        "Crit Chance" to "§9",
        "Crit Damage" to "§9",
        "Bonus Attack Speed" to "§e",
        "Ability Damage" to "§c",
        "Magic Find" to "§b",
        "Pet Luck" to "§d",
        "True Defense" to "§f",
        "Fishing Speed" to "§b",
        "Sea Creature Chance" to "§3",
        "Trophy Fish Chance" to "§6",
        "Treasure Chance" to "§6",
        "Bonus Pest Chance" to "§2",
        "Heat Resistance" to "§c",
        "Ferocity" to "§c",
        "Mining Speed" to "§6",
        "Mining Spread" to "§e",
        "Mining Fortune" to "§6",
        "Foraging Fortune" to "§6",
        "Farming Fortune" to "§6",
        "Health Regen" to "§c",
        "Vitality" to "§4",
        "Mending" to "§a",
        "Swing Range" to "§e",
        "Respiration" to "§3",
        "Pressure Resistance" to "§9",
        "Hunter Fortune" to "§d",
        "Sweep" to "§2"
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

                "§7${stat}: ${
                    if (colorStatMap.contains(stat)) colorStatMap[stat] else group("OldColor")
                }${
                    group("Bonus")
                }${
                    if (iconStatMap.contains(stat)) iconStatMap[stat] else ""
                }"
            }
        }
    }
}
