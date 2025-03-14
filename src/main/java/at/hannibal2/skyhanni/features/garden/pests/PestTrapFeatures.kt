package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestTrapConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.garden.pests.PestTrapDataEvent
import at.hannibal2.skyhanni.features.garden.pests.PestTrapApi.MAX_TRAPS
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.audio.ISound
import kotlin.time.Duration.Companion.seconds

private typealias WarningReason = PestTrapConfig.WarningConfig.WarningReason
private typealias WarningDisplayType = PestTrapConfig.WarningConfig.WarningDisplayType

@SkyHanniModule
object PestTrapFeatures {

    private val config get() = SkyHanniMod.feature.garden.pests.pestTrap
    private val enabledTypes: WarningDisplayType get() = config.warningConfig.warningDisplayType.get()
    private val userEnabledWarnings: List<WarningReason> get() = config.warningConfig.enabledWarnings.get()
    private val chatWarnEnabled: Boolean
        get() = enabledTypes in listOf(
            WarningDisplayType.CHAT,
            WarningDisplayType.BOTH
        )
    private val titleWarnEnabled: Boolean
        get() = enabledTypes in listOf(
            WarningDisplayType.TITLE,
            WarningDisplayType.BOTH
        )

    private val activeWarnings: MutableList<WarningReason> = mutableListOf()
    private val reminderInterval: Property<Int> get() = config.warningConfig.warningIntervalSeconds
    private var nextWarningMark: SimpleTimeMark = SimpleTimeMark.farPast()
    private val soundString get(): String = config.warningConfig.warningSound.get()
    private var warningSound: ISound? = refreshSound()

    private fun getNextWarningMark() = SimpleTimeMark.now() + reminderInterval.get().toInt().seconds
    private fun refreshSound() = soundString.takeIf(String::isNotEmpty)?.let { SoundUtils.createSound(it, 1f) }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.warningConfig.warningSound) {
            warningSound = refreshSound()
        }
        ConditionalUtils.onToggle(reminderInterval) {
            nextWarningMark = getNextWarningMark()
        }
        ConditionalUtils.onToggle(config.warningConfig.warningDisplayType) {
            nextWarningMark = SimpleTimeMark.now() + 5.seconds
        }
    }

    @HandleEvent
    fun onPestTrapDataUpdate(event: PestTrapDataEvent) {
        activeWarnings.clear()
        if (event.trapsPlaced < MAX_TRAPS && WarningReason.UNPLACED_TRAPS in userEnabledWarnings)
            activeWarnings.add(WarningReason.UNPLACED_TRAPS)
        if (event.anyFull && WarningReason.TRAP_FULL in userEnabledWarnings)
            activeWarnings.add(WarningReason.TRAP_FULL)
        if (event.anyNoBait && WarningReason.NO_BAIT in userEnabledWarnings)
            activeWarnings.add(WarningReason.NO_BAIT)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (activeWarnings.isEmpty() || nextWarningMark.isInFuture()) return
        val activeWarnings = activeWarnings.map { it.warningString }

        warningSound?.playSound()
        tryWarnTitle(activeWarnings.first())
        tryWarnChat(activeWarnings)

        nextWarningMark = getNextWarningMark()
    }

    private fun tryWarnChat(finalWarnings: List<String>) {
        if (!chatWarnEnabled) return
        finalWarnings.forEach { warning ->
            ChatUtils.chat(warning)
        }
    }

    private fun tryWarnTitle(finalWarning: String) {
        if (!titleWarnEnabled) return
        LorenzUtils.sendTitle(finalWarning, 3.seconds, 2.8, 7f)
    }
}
