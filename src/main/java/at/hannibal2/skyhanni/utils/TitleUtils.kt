package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import java.util.Deque
import java.util.LinkedList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TitleUtils {
    private val sortQueue: Deque<Quint<String, Duration, Double, Float, Double>> = LinkedList()
    private val titleQueue: Deque<Quad<String, Duration, Double, Float>> = LinkedList()

    private var lastTitle: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastTitleDuration: Duration = 0.seconds

    fun addTitleToSortQueue(
        text: String,
        duration: Duration,
        height: Double = 1.8,
        fontSize: Float = 4f,
        value: Double
    ) {
        sortQueue.add(Quint(text, duration, height, fontSize, value))
    }

    private fun addTitleToQueue(text: String, duration: Duration, height: Double = 1.8, fontSize: Float = 4f) {
        titleQueue.add(Quad(text, duration, height, fontSize))
    }

    @HandleEvent
    fun onTick(e: SkyHanniTickEvent) {
        if (sortQueue.isNotEmpty()) {
            val sorted = sortQueue.sortedBy { it.fifth }
            addTitleToQueue(sorted.first().first, sorted.first().second, sorted.first().third, sorted.first().fourth)
            sortQueue.clear()
        }
        if (titleQueue.isNotEmpty() && lastTitle.passedSince() > lastTitleDuration) {
            val title = titleQueue.poll()
            LorenzUtils.sendTitle(title.first, title.second, title.third, title.fourth)
            lastTitle = SimpleTimeMark.now()
            lastTitleDuration = title.second
            ChatUtils.chat("Sent title: ${title.first}")
        }
    }
}
