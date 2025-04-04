package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestTrapConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.garden.pests.PestTrapDataEvent
import at.hannibal2.skyhanni.features.garden.pests.PestTrapApi.MAX_TRAPS
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.audio.ISound
import kotlin.math.max
import kotlin.time.Duration
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
            WarningDisplayType.BOTH,
        )
    private val titleWarnEnabled: Boolean
        get() = enabledTypes in listOf(
            WarningDisplayType.TITLE,
            WarningDisplayType.BOTH,
        )

    private val allActiveWarnings: MutableList<WarningReason> = mutableListOf()
    private val reminderInterval: Property<Int> get() = config.warningConfig.warningIntervalSeconds
    private val virtualReminderInterval get() = max(10, reminderInterval.get()).seconds
    private var nextWarningMark: SimpleTimeMark = SimpleTimeMark.farPast()
    private val soundString get(): String = config.warningConfig.warningSound.get()
    private var warningSound: ISound? = refreshSound()

    private fun getNextWarningMark() = SimpleTimeMark.now() + virtualReminderInterval
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
        allActiveWarnings.clear()
        if (event.trapsPlaced < MAX_TRAPS) allActiveWarnings.add(WarningReason.UNPLACED_TRAPS)
        if (event.anyFull) allActiveWarnings.add(WarningReason.TRAP_FULL)
        if (event.anyNoBait) allActiveWarnings.add(WarningReason.NO_BAIT)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed(event: SecondPassedEvent) {
        val applicableWarnings = allActiveWarnings.filter { it in userEnabledWarnings }
        if (applicableWarnings.isEmpty() || nextWarningMark.isInFuture()) return
        val activeWarnings = applicableWarnings.map { it.warningString }

        warningSound?.playSound()
        if (titleWarnEnabled) TitleManager.sendTitle(activeWarnings.first(), height = 2.8, fontSize = 7f)
        if (chatWarnEnabled) activeWarnings.forEach { ChatUtils.chat(it) }

        nextWarningMark = getNextWarningMark()
    }
}
