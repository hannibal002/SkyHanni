package at.hannibal2.skyhanni.features.misc.ghostcounter

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import io.github.moulberry.notenoughupdates.util.Utils
import java.io.FileReader

object GhostUtil {

    fun reset() {
        for (opt in GhostData.Option.values()) {
            opt.set(0.0)
            opt.set(0.0, true)
        }
        GhostCounter.hidden?.totalMF = 0.0
        GhostCounter.update()
    }

     fun isUsingCTGhostCounter(): Boolean {
        return GhostCounter.ghostCounterV3File.exists() && GhostCounter.ghostCounterV3File.isFile
    }

    fun prettyTime(millis: Long): Map<String, String> {
        val seconds = millis / 1000 % 60
        val minutes = millis / 1000 / 60 % 60
        val hours = millis / 1000 / 60 / 60 % 24
        val days = millis / 1000 / 60 / 60 / 24
        return buildMap {
            if (millis < 0) {
                clear()
            } else if (minutes == 0L && hours == 0L && days == 0L) {
                put("seconds", seconds.toString())
            } else if (hours == 0L && days == 0L) {
                put("seconds", seconds.toString())
                put("minutes", minutes.toString())
            } else if (days == 0L) {
                put("seconds", seconds.toString())
                put("minutes", minutes.toString())
                put("hours", hours.toString())
            } else {
                put("seconds", seconds.toString())
                put("minutes", minutes.toString())
                put("hours", hours.toString())
                put("days", days.toString())
            }
        }
    }
    fun importCTGhostCounterData() {
        val c = ProfileStorageData.profileSpecific?.ghostCounter ?: return
        if (isUsingCTGhostCounter()) {
            if (c.ctDataImported) {
                LorenzUtils.chat("§e[SkyHanni] §cYou already imported GhostCounterV3 data!")
                return
            }
            val json = ConfigManager.gson.fromJson(FileReader(GhostCounter.ghostCounterV3File), com.google.gson.JsonObject::class.java)
            GhostData.Option.GHOSTSINCESORROW.add(json["ghostsSinceSorrow"].asDouble)
            GhostData.Option.SORROWCOUNT.add(json["sorrowCount"].asDouble)
            GhostData.Option.BAGOFCASH.add(json["BagOfCashCount"].asDouble)
            GhostData.Option.PLASMACOUNT.add(json["PlasmaCount"].asDouble)
            GhostData.Option.VOLTACOUNT.add(json["VoltaCount"].asDouble)
            GhostData.Option.GHOSTLYBOOTS.add(json["GhostlyBootsCount"].asDouble)
            GhostData.Option.KILLS.add(json["ghostsKilled"].asDouble)
            GhostCounter.hidden?.totalMF = GhostCounter.hidden?.totalMF?.plus(json["TotalMF"].asDouble) ?: json["TotalMF"].asDouble
            GhostData.Option.TOTALDROPS.add(json["TotalDrops"].asDouble)
            c.ctDataImported = true
            LorenzUtils.chat("§e[SkyHanni] §aImported data successfully!")
        } else
            LorenzUtils.chat("§e[SkyHanni] §cGhostCounterV3 ChatTriggers module not found!")
    }

    fun String.formatText(value: Int, session: Int = -1): String {
        return Utils.chromaStringByColourCode(this.replace("%value%", value.addSeparators())
                .replace("%session%", session.addSeparators())
                .replace("&", "§"))
    }

    fun String.formatText(t: String): String {
        return Utils.chromaStringByColourCode(this.replace("%value%", t)
                .replace("&", "§"))
    }

    fun String.formatText(value: Double, session: Double): String {
        return Utils.chromaStringByColourCode(this.replace("%value%", value.roundToPrecision(2).addSeparators())
                .replace("%session%", session.roundToPrecision(2).addSeparators())
                .replace("&", "§"))
    }

    fun String.formatBestiary(currentKill: Int, killNeeded: Int): String {
        return Utils.chromaStringByColourCode(
                this.replace("%currentKill%", if (GhostCounter.config.showMax) GhostCounter.bestiaryCurrentKill.addSeparators() else currentKill.addSeparators())
                        .replace("%percentNumber%", percent(GhostCounter.bestiaryCurrentKill.toDouble()))
                        .replace("%killNeeded%", NumberUtil.format(killNeeded))
                        .replace("%currentLevel%", if (GhostCounter.hidden?.bestiaryNextLevel?.toInt()!! < 0) "46" else "${GhostCounter.hidden?.bestiaryNextLevel?.toInt()!! - 1}")
                        .replace("%nextLevel%", if (GhostCounter.config.showMax) "46" else "${GhostCounter.hidden?.bestiaryNextLevel?.toInt()!!}")
                        .replace("&", "§"))
    }

    private fun percent(number: Double): String {
        return "${((number / 3_000_000) * 100).roundToPrecision(4)}"
    }
}