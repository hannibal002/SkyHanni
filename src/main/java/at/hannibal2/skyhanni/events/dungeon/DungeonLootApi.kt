package at.hannibal2.skyhanni.events.dungeon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.costs
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher

@SkyHanniModule
object DungeonLootApi {

    private val DUNGEON_CHEST_KEY = "DUNGEON_CHEST_KEY".toInternalName()

    // infos about the current process of buying a chest
    private var cost = 0
    private var usedKey = false
    private var chestType: String? = null
    private var loot = mutableListOf<Pair<String, Int>>()


    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage
        val patterns = " {2}(?<type>.*) CHEST REWARDS".toPattern()
        patterns.matchMatcher(message) {
            val type = group("type")
            chestType = type
            return
        }

        val chestType = chestType ?: return

        if (message == "") {
            if (loot.isEmpty()) error("loot is empty!")
            DungeonLootEvent(cost, usedKey, chestType, loot).post()
            cost = 0
            usedKey = false
            this.chestType = null
            loot = mutableListOf()
            return
        }

        println("message: $message")
        ItemUtils.readItemStackFromChat(event.message)?.let {
            loot.add(it)
        }
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (DungeonApi.isInCroesus || IslandType.CATACOMBS.isInIsland()) {
            val stack = event.item ?: return
            val costs = stack.costs()
            println("costs: ${costs.size}")
            for ((internalName, amount) in costs) {
                println("internalName: $internalName ($amount)")
                when (internalName) {
                    NeuInternalName.SKYBLOCK_COIN -> {
                        cost = amount
                    }

                    DUNGEON_CHEST_KEY -> {
                        usedKey = true
                    }

                    else -> error("unknown cost of dungeon chest: $internalName (x$amount)")
                }
            }
        }
    }

}
