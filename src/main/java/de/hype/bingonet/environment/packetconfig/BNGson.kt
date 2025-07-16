package at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.environment.packetconfig

import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.shared.json.ColorSerializer
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.shared.json.DurationSerializer
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.shared.json.InstantSerializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.awt.Color
import java.time.Duration
import java.time.Instant

object BNGson {
    var ownSerializer: Gson = GsonBuilder().create()
    fun create(): Gson {
        return base.setPrettyPrinting().create()
    }

    fun createNotPrettyPrinting(): Gson {
        return base.create()
    }

    private val base: GsonBuilder
        get() = GsonBuilder()
            .registerTypeAdapter(Color::class.java, ColorSerializer())
            .registerTypeAdapter(Duration::class.java, DurationSerializer())
            .registerTypeAdapter(Instant::class.java, InstantSerializer())
}
