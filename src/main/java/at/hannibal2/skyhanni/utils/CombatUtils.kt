package at.hannibal2.skyhanni.utils

import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils
import io.github.moulberry.notenoughupdates.util.XPInformation

object CombatUtils {

    private var lastTotalXp = -1f
    private val xpGainQueue = mutableListOf<Float>()
    private var xpGainTimer = 0
    private var lastUpdate: Long = -1
    private var skillInfo: XPInformation.SkillInfo? = null
    private var skillInfoLast: XPInformation.SkillInfo? = null
    private const val skillType = "Combat"
    var xpGainHourLast = -1f
    var xpGainHour = -1f
    var isKilling = false

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
                xpGainTimer = 3
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

    /**
     * Taken from NotEnoughUpdates
     */
    fun interp(now: Float, last: Float): Float {
        var interp = now
        if (last >= 0 && last != now) {
            var factor = (System.currentTimeMillis() - lastUpdate) / 1000f
            factor = LerpUtils.clampZeroOne(factor)
            interp = last + (now - last) * factor
        }
        return interp
    }

}