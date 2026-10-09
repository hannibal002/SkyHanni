package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.fishing.BaitUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FishingBaitWarnings {
    private val config get() = SkyHanniMod.feature.fishing.fishingBaitWarnings

    private var lastBait: FishingApi.BaitType? = null
    private var wasUsingBait = true

    private val DARK_BAIT = "DARK_BAIT".toInternalName()
    private val LIGHT_BAIT = "LIGHT_BAIT".toInternalName()

    @HandleEvent
    private fun onWorldChange() {
        lastBait = null
        wasUsingBait = true
    }

    @HandleEvent
    private fun onBaitUpdate(event: BaitUpdateEvent) {
        if (!FishingApi.holdingRod) {
            wasUsingBait = false
            lastBait = null
            return
        }

        val bait = event.baitType

        lastBait?.let {
            if (it != bait && config.baitChangeWarning) {
                val beforeName = lastBait?.displayName ?: "None"
                val afterName = bait?.displayName ?: "None"
                showBaitChangeWarning(beforeName, afterName)
            }
        }
        wasUsingBait = bait != null
        lastBait = bait
    }

    @HandleEvent
    private fun onBobberCast() {
        if (config.noBaitWarning && !wasUsingBait) showNoBaitWarning()
        if (config.darkAndLightWarning) checkDarkAndLightWarning()
    }

    private fun checkDarkAndLightWarning() {
        val currentBait = FishingApi.currentBait ?: return
        val shouldBeDaytime = when (currentBait.internalName) {
            LIGHT_BAIT -> true
            DARK_BAIT -> false
            else -> return
        }
        val hasSun = SkyBlockTime.now().isDayTime()
        if (shouldBeDaytime != hasSun) {
            SoundUtils.playClickSound()
            TitleManager.sendTitle("§eWrong Bait!", duration = 2.seconds)
            val timeText = if (hasSun) "Day" else "Night"
            ChatUtils.chat("You are using ${currentBait.displayName} while it is $timeText")
        }
    }

    private fun showBaitChangeWarning(before: String, after: String) {
        SoundUtils.playClickSound(isWarning = true)
        TitleManager.sendTitle("§eBait changed!", duration = 2.seconds)
        ChatUtils.chat("Fishing Bait changed: $before §e-> $after")
    }

    private fun showNoBaitWarning() {
        SoundUtils.playErrorSound()
        TitleManager.sendTitle("§cNo bait is used!", duration = 2.seconds)
        ChatUtils.chat("You're not using any fishing baits!")
    }
}
