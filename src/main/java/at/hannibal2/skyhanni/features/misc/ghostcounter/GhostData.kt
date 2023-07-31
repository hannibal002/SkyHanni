package at.hannibal2.skyhanni.features.misc.ghostcounter

import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostData.Option.*
import java.util.regex.Pattern
import kotlin.math.roundToInt

object GhostData {

    private var session = mutableMapOf(
        KILLS to 0.0,
        SORROWCOUNT to 0.0,
        VOLTACOUNT to 0.0,
        PLASMACOUNT to 0.0,
        GHOSTLYBOOTS to 0.0,
        BAGOFCASH to 0.0,
        TOTALDROPS to 0.0,
        SCAVENGERCOINS to 0.0,
        MAXKILLCOMBO to 0.0,
        SKILLXPGAINED to 0.0
    )

    val bestiaryData = mutableMapOf<Int, Int>().apply {

        if (GhostCounter.bestiaryUpdate) {
            val commonValue = 100_000
            for (i in 1..46) {
                this[i] = when (i) {
                    1 -> 10
                    2 -> 15
                    3 -> 75
                    4 -> 150
                    5 -> 250
                    6 -> 500
                    7 -> 1_500
                    8 -> 2_500
                    9 -> 5_000
                    10 -> 15_000
                    11 -> 25_000
                    12 -> 50_000
                    else -> commonValue
                }
            }
        } else {
            val commonValue = 100_000
            for (i in 1..46) {
                this[i] = when (i) {
                    1 -> 10
                    2 -> 15
                    3 -> 75
                    4 -> 150
                    5 -> 250
                    6 -> 500
                    7 -> 1_500
                    8 -> 2_500
                    9 -> 5_000
                    10 -> 15_000
                    11 -> 25_000
                    12 -> 50_000
                    else -> commonValue
                }
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