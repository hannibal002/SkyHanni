package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.mining.FossilExcavationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FossilExcavatorApi {

    private val patternGroup = RepoPattern.group("mining.fossil.excavator")
    private val chatPatternGroup = patternGroup.group("chat")

    /**
     * REGEX-TEST:   §r§6§lEXCAVATION COMPLETE
     */
    private val startPattern by chatPatternGroup.pattern("start", " {2}§r§6§lEXCAVATION COMPLETE ?")

    /**
     * REGEX-TEST: §a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val endPattern by chatPatternGroup.pattern("end", "§a§l▬{64}")

    /**
     * REGEX-TEST:     §r§6Tusk Fossil
     */
    private val itemPattern by chatPatternGroup.pattern("item", " {4}§r(?<item>.+)")

    /**
     * REGEX-TEST: §cYou didn't find anything. Maybe next time!
     */
    private val emptyPattern by chatPatternGroup.pattern("empty", "§cYou didn't find anything. Maybe next time!")

    private var inLoot = false
    private val loot = mutableListOf<Pair<String, Int>>()

    var inExcavatorMenu = false

    val scrapItem = "SUSPICIOUS_SCRAP".toInternalName()

    val excavatorInventory = InventoryDetector(
        checkInventoryName = { it == "Fossil Excavator" },
        onCloseInventory = { inExcavatorMenu = false }
    )

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!excavatorInventory.isInside()) return
        inExcavatorMenu = event.inventoryItems.values.any {
            it.hoverName.string.removeColor() == "Start Excavator"
        }
    }

    @HandleEvent
    fun onWorldChange() {
        inExcavatorMenu = false
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onChat(event: SkyHanniChatEvent) {
        val message = event.message
        if (emptyPattern.matches(message)) FossilExcavationEvent(emptyList()).post()

        if (startPattern.matches(message)) {
            inLoot = true
            return
        }

        if (!inLoot) return

        if (endPattern.matches(message)) {
            FossilExcavationEvent(loot.toList()).post()
            loot.clear()
            inLoot = false
            return
        }
        var pair = itemPattern.matchMatcher(message) {
            /**
             * TODO fix the bug that readItemAmount produces two different outputs:
             * §r§fEnchanted Book -> §fEnchanted
             * §fEnchanted Book §r§8x -> §fEnchanted Book
             *
             * also maybe this is no bug, as enchanted book is no real item?
             */
            ItemUtils.readItemAmount(group("item"))
        } ?: return

        ItemUtils.readBookType(pair.first)?.let {
            pair = it to pair.second
        }
        loot.add(pair)
    }
}
