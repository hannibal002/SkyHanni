package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FrozenTreasureTracker {
    private val config get() = SkyHanniMod.feature.misc.frozenTreasureTracker
    private var display = listOf<List<Any>>()
    private var treasureMined = 0
    private var compactProcs = 0
    private var estimatedIce = 0
    private var icePerHour = 0

    private var treasureCount = mapOf<FrozenTreasures, Int>()

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            newList.add(map[index])
        }
        return newList
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }


    //FROZEN TREASURE! You found Enchanted Ice!

}