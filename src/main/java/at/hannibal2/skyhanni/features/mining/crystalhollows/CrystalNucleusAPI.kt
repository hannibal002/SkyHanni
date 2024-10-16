package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object CrystalNucleusAPI {

    private val patternGroup = RepoPattern.group("mining.crystalnucleus")

    /**
     * REGEX-TEST:   §r§5§lCRYSTAL NUCLEUS LOOT BUNDLE
     */
    private val startPattern by patternGroup.pattern(
        "loot.start",
        " {2}§r§5§lCRYSTAL NUCLEUS LOOT BUNDLE.*",
    )

    /**
     * REGEX-TEST: §3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val endPattern by patternGroup.pattern(
        "loot.end",
        "§3§l▬{64}",
    )

    private var inLoot = false
    private val loot = mutableListOf<Pair<String, Int>>()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!IslandType.CRYSTAL_HOLLOWS.isInIsland()) return

        val message = event.message

        if (startPattern.matches(message)) {
            inLoot = true
            return
        }
        if (!inLoot) return

        if (endPattern.matches(message)) {
            CrystalNucleusLootEvent(loot).post()
            loot.clear()
            inLoot = false
            return
        }

        // All loot rewards start with 4 spaces.
        // To simplify regex statements, this check is done outside the main logic.
        // This also nerfs the "§r§a§lREWARDS" message.
        message.takeIf { it.startsWith("    ") }?.substring(4)?.let { lootMessage ->
            ItemUtils.readItemAmount(lootMessage)?.let { pair ->
                loot.add(
                    when(pair.first) {
                        // Assume enchanted books are Fortune IV books
                        "§fEnchanted" -> "§9Fortune IV" to pair.second
                        "§fEnchanted Book" -> "§9Fortune IV" to pair.second
                        else -> pair
                    }
                )
            }
        }
    }
}
