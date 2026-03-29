package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.client.NarratorStatus
import net.minecraft.sounds.SoundSource
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object MutedNarratorWarning {

    private val config get() = SkyHanniMod.feature.misc
    private val warningEnabled get() = config.mutedNarratorWarning.get()
    private val reminderInterval = 1.minutes
    private val lastReminded: SimpleTimeMark = SimpleTimeMark.farPast()

    private val narratorActive get(): Boolean = runCatching {
        Minecraft.getInstance().narrator.isActive
    }.getOrDefault(false)

    private val isNarratorMuted: Boolean get() = runCatching {
        val narratorVolume = Minecraft.getInstance().options.getFinalSoundSourceVolume(SoundSource.VOICE)
        return narratorVolume == 0f
    }.getOrDefault(false)

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (!warningEnabled || !narratorActive || !isNarratorMuted) return
        if (lastReminded.passedSince() < reminderInterval) return

        ChatUtils.clickToActionOrDisable(
            "You currently have the Minecraft narrator turn on, but it is muted.",
            config::mutedNarratorWarning,
            "turn off narrator",
            { Minecraft.getInstance().options.narrator().set(NarratorStatus.OFF) },
        )
    }
}
