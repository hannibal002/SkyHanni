package at.hannibal2.skyhanni.features.misc.ghostcounter

import java.util.regex.Pattern
import kotlin.math.roundToInt

object GhostData {

    private var session = mutableMapOf(
        Option.KILLS to 0.0,
        Option.SORROWCOUNT to 0.0,
        Option.VOLTACOUNT to 0.0,
        Option.PLASMACOUNT to 0.0,
        Option.GHOSTLYBOOTS to 0.0,
        Option.BAGOFCASH to 0.0,
        Option.TOTALDROPS to 0.0,
        Option.SCAVENGERCOINS to 0.0,
        Option.MAXKILLCOMBO to 0.0,
        Option.SKILLXPGAINED to 0.0
    )

    val bestiaryData = mutableMapOf<Int, Int>().apply {
        for (i in 1..25) {
            this[i] = when (i) {
                1 -> 5
                2 -> 5
                3 -> 5
                4 -> 10
                5 -> 25
                6 -> 50
                7 -> 100
                8 -> 150
                9 -> 150
                10 -> 250
                11 -> 750
                12 -> 1_500
                13 -> 2_000
                14, 15, 16, 17 -> 2_500
                18 -> 3_000
                19, 20 -> 3_500
                21 -> 25_000
                22, 23, 24, 25 -> 50_000
                else -> 0
            }
        }
    }

    enum class Option(val pattern: Pattern? = null) {
        KILLS,
        SORROWCOUNT("§6§lRARE DROP! §r§9Sorrow §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()),
        VOLTACOUNT("§6§lRARE DROP! §r§9Volta §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()),
        PLASMACOUNT("§6§lRARE DROP! §r§9Plasma §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()),
        GHOSTLYBOOTS("§6§lRARE DROP! §r§9Ghostly Boots §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()),
        BAGOFCASH("§eThe ghost's death materialized §r§61,000,000 coins §r§efrom the mists!".toPattern()),
        KILLCOMBOCOINS("[+]\\d+ Kill Combo [+](?<coin>.*) coins per kill".toPattern()),
        TOTALDROPS,
        GHOSTSINCESORROW,
        SCAVENGERCOINS,
        MAXKILLCOMBO,
        KILLCOMBO("[+]\\d+ Kill Combo [+](?<coin>.*) coins per kill".toPattern()),
        SKILLXPGAINED;

        fun add(i: Double, s: Boolean = false) {
            if (s)
                session[this] = session[this]?.plus(i) ?: i
            else
                GhostCounter.hidden?.data?.set(this, GhostCounter.hidden?.data?.get(this)?.plus(i) ?: i)
        }

        fun set(i: Double, s: Boolean = false) {
            if (s)
                session[this] = i
            else
                GhostCounter.hidden?.data?.set(this, i)
        }

        fun getInt(s: Boolean = false): Int {
            return if (s)
                session[this]?.roundToInt() ?: 0
            else
                GhostCounter.hidden?.data?.get(this)?.roundToInt() ?: 0
        }

        fun get(s: Boolean = false): Double {
            return if (s)
                session[this] ?: 0.0
            else
                GhostCounter.hidden?.data?.get(this) ?: 0.0
        }
    }
}
