package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostCounter
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostData
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils
import io.github.moulberry.notenoughupdates.util.XPInformation

object CombatUtils {

    private var lastTotalXp = -1f
    private val xpGainQueue = mutableListOf<Float>()
    private var xpGainTimer = 0
    var lastUpdate: Long = -1
    private var skillInfo: XPInformation.SkillInfo? = null
    private var skillInfoLast: XPInformation.SkillInfo? = null
    private const val skillType = "Combat"
    var xpGainHourLast = -1f
    var xpGainHour = -1f
    var isKilling = false
    private var lastTotalKill = -1
    private val killGainQueue = mutableListOf<Int>()
    var lastKillUpdate: Long = -1
    var killGainHourLast = -1
    var killGainHour = -1
    private var gainTimer = 0
    var _isKilling = false


    /**
     * Taken from NotEnoughUpdates
     */
    fun calculateXP() {
        lastUpdate = System.currentTimeMillis()
        xpGainHourLast = xpGainHour
        skillInfoLast = skillInfo
        skillInfo = XPInformation.getInstance().getSkillInfo(skillType) ?: return
        val totalXp: Float = skillInfo!!.totalXp
        if (lastTotalXp > 0) {
            val delta: Float = totalXp - lastTotalXp

            if (delta > 0 && delta < 1000) {
                xpGainTimer = GhostCounter.config.pauseTimer
                xpGainQueue.add(0, delta)
                while (xpGainQueue.size > 30) {
                    xpGainQueue.removeLast()
                }
                var totalGain = 0f
                for (f in xpGainQueue) totalGain += f
                xpGainHour = totalGain * (60 * 60) / xpGainQueue.size
                isKilling = true
            } else if (xpGainTimer > 0) {
                xpGainTimer--
                xpGainQueue.add(0, 0f)
                while (xpGainQueue.size > 30) {
                    xpGainQueue.removeLast()
                }
                var totalGain = 0f
                for (f in xpGainQueue) totalGain += f
                xpGainHour = totalGain * (60 * 60) / xpGainQueue.size
                isKilling = true
            } else if (delta <= 0) {
                isKilling = false
            }
        }
        lastTotalXp = totalXp
    }

    /*
    Must be a better way to do this than repeating the calculateXP function
     */
    fun calculateETA() {
        lastKillUpdate = System.currentTimeMillis()
        killGainHourLast = killGainHour
        val nextLevel = GhostCounter.hidden?.bestiaryNextLevel?.toInt() ?: return
        val kill = GhostCounter.hidden?.bestiaryCurrentKill?.toInt() ?: return
        val sum = GhostData.bestiaryData.filterKeys { it <= nextLevel - 1 }.values.sum()
        val cKill = sum + kill
        val totalKill = if (GhostCounter.config.showMax) GhostCounter.bestiaryCurrentKill else cKill
        if (lastTotalKill > 0) {
            val delta: Int = totalKill - lastTotalKill
            if (delta in 1..19) {
                gainTimer = GhostCounter.config.pauseTimer
                killGainQueue.add(0, delta)
                while (killGainQueue.size > 30) {
                    killGainQueue.removeLast()
                }
                var totalGain = 0
                for (f in killGainQueue) totalGain += f
                killGainHour = totalGain * (60 * 60) / killGainQueue.size
                _isKilling = true
            } else if (gainTimer > 0) {
                gainTimer--
                killGainQueue.add(0, 0)
                while (killGainQueue.size > 30) {
                    killGainQueue.removeLast()
                }
                var totalGain = 0
                for (f in killGainQueue) totalGain += f
                killGainHour = totalGain * (60 * 60) / killGainQueue.size
                _isKilling = true
            } else if (delta <= 0) {
                _isKilling = false
            }
        }
        lastTotalKill = totalKill
    }

    /**
     * Taken from NotEnoughUpdates
     */
    fun interp(now: Float, last: Float, lastupdate: Long): Float {
        var interp = now
        if (last >= 0 && last != now) {
            var factor = (System.currentTimeMillis() - lastupdate) / 1000f
            factor = LerpUtils.clampZeroOne(factor)
            interp = last + (now - last) * factor
        }
        return interp
    }

}