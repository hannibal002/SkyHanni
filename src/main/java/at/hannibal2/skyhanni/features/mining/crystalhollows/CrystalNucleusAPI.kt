package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
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
        " {2}§r§5§lCRYSTAL NUCLEUS LOOT BUNDLE",
    )

    /**
     * REGEX-TEST: §3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val endPattern by patternGroup.pattern(
        "loot.end",
        "§3§l▬{64}",
    )

    /**
     * REGEX-TEST: §r§2Jade Dye §r§8x4
     * REGEX-TEST: §r§5Divan Fragment §r§8x2
     * REGEX-TEST: §r§5Pickonimbus 2000
     * REGEX-TEST: §r§9⸕ Fine Amber Gemstone
     * REGEX-TEST: §r§9❈ Fine Amethyst Gemstone
     * REGEX-TEST: §r§9☘ Fine Jade Gemstone
     * REGEX-TEST: §r§9❁ Fine Jasper Gemstone
     * REGEX-TEST: §r§9❤ Fine Ruby Gemstone
     * REGEX-TEST: §r§9✎ Fine Sapphire Gemstone
     * REGEX-TEST: §r§9✧ Fine Topaz Gemstone
     * REGEX-TEST: §r§9Jaderald
     * REGEX-TEST: §r§a⸕ Flawed Amber Gemstone §r§8x12
     * REGEX-TEST: §r§a❈ Flawed Amethyst Gemstone §r§8x48
     * REGEX-TEST: §r§a☘ Flawed Jade Gemstone §r§8x12
     * REGEX-TEST: §r§a❁ Flawed Jasper Gemstone §r§8x6
     * REGEX-TEST: §r§a❤ Flawed Ruby Gemstone §r§8x36
     * REGEX-TEST: §r§a✎ Flawed Sapphire Gemstone §r§8x6
     * REGEX-TEST: §r§a✧ Flawed Topaz Gemstone §r§8x6
     * REGEX-TEST: §r§fPrehistoric Egg
     * REGEX-TEST: §r§dGemstone Powder §r§8x4,178
     */
    private val itemPattern by patternGroup.pattern("loot.item", "§r(?<item>.+)")

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
        val ssMessage = message.takeIf { it.startsWith("    ") }?.substring(4) ?: return

        var pair = itemPattern.matchMatcher(ssMessage) {
            ItemUtils.readItemAmount(group("item"))
        } ?: return
        // Assume enchanted books are Fortune IV books
        if (pair.first.let { it == "§fEnchanted" || it == "§fEnchanted Book" }) {
            pair = "§9Fortune IV" to pair.second
        }
        loot.add(pair)
    }

    //Todo: Actual logic
    fun isInNucleus(): Boolean {
        return true
    }
}
