package at.hannibal2.hanni.data

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.collection.CollectionUtils.addOrPut
import kotlin.time.Duration.Companion.seconds

@HanniModule
object EventCounter {

    private val config get() = HanniMod.feature.dev.debug

    private val map = mutableMapOf<String, Int>()
    private var lastUpdate = SimpleTimeMark.farPast()

    private var enabled = false

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        enabled = SkyBlockUtils.onHypixel && config.eventCounter
    }

    fun count(eventName: String) {
        if (!enabled) return

        map.addOrPut(eventName, 1)

        if (lastUpdate == SimpleTimeMark.farPast()) {
            lastUpdate = SimpleTimeMark.now()
        }

        if (lastUpdate.passedSince() > 1.seconds) {
            lastUpdate = SimpleTimeMark.now()

            print(map)

            map.clear()
        }
    }

    private fun print(map: MutableMap<String, Int>) {
        println("")
        var total = 0
        for ((name, amount) in map.entries.sortedBy { it.value }) {
            println("$name (${amount.addSeparators()} times)")
            total += amount
        }
        println("")
        println("total: ${total.addSeparators()}")
    }
}
