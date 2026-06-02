package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.mining.CorpseLootedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CorpseApi {

    private val patternGroup = RepoPattern.group("mining.mineshaft")
    private val chatPatternGroup = patternGroup.group("chat")

    /**
     * REGEX-TEST:   §r§b§l§r§9§lLAPIS §r§b§lCORPSE LOOT!
     * REGEX-TEST:   §r§b§l§r§7§lTUNGSTEN §r§b§lCORPSE LOOT!
     * REGEX-TEST:   §r§b§l§r§6§lUMBER §r§b§lCORPSE LOOT!
     * REGEX-TEST:   §r§b§l§r§f§lVANGUARD §r§b§lCORPSE LOOT!
     */
    private val startPattern by chatPatternGroup.pattern(
        "start",
        " {2}§r§b§l§r§(?<color>.)§l(?<name>.*) §r§b§lCORPSE LOOT! ?"
    )

    /**
     * REGEX-TEST: §a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val endPattern by chatPatternGroup.pattern("end", "§a§l▬{64}")

    private var inLoot = false
    private val loot = mutableListOf<Pair<String, Int>>()

    private var corpseType: CorpseType? = null

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.message

        startPattern.matchMatcher(message) {
            inLoot = true
            val name = group("name")
            corpseType = CorpseType.valueOf(name)
            return
        }

        if (!inLoot) return

        if (endPattern.matches(message)) {
            corpseType?.let {
                CorpseLootedEvent(it, loot.toList()).post()
            }
            corpseType = null
            loot.clear()
            inLoot = false
            return
        }

        ItemUtils.readItemStackFromChat(message)?.let {
            loot.add(it)
        }
    }
}
