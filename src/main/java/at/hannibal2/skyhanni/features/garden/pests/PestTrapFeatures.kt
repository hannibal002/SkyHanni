package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestTrapConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.garden.pests.PestTrapDataUpdatedEvent
import at.hannibal2.skyhanni.features.garden.pests.PestTrapApi.PestTrapData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import net.minecraft.client.audio.ISound
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

private typealias WarningReason = PestTrapConfig.WarningConfig.WarningReason
private typealias WarningDisplayType = PestTrapConfig.WarningConfig.WarningDisplayType

@SkyHanniModule
object PestTrapFeatures {

    private val config get() = SkyHanniMod.feature.garden.pests.pestTrap
    private val warnTypes get() = config.warning.warnType.get()
    private val chatWarnEnabled: Boolean get() = warnTypes in listOf(WarningDisplayType.CHAT, WarningDisplayType.BOTH)
    private val titleWarnEnabled: Boolean get() = warnTypes in listOf(WarningDisplayType.TITLE, WarningDisplayType.BOTH)
    private val activeWarnings: MutableMap<WarningReason, Set<Int>> = enumMapOf()

    private var nextWarningMark: SimpleTimeMark = SimpleTimeMark.farPast()
    private var warningSound: ISound? = null

    private fun getNextWarningMark() = SimpleTimeMark.now() + max(10, config.warning.warningIntervalSeconds.get()).seconds
    private fun refreshSound() = config.warning.sound.get().takeIf(String::isNotEmpty)?.let { SoundUtils.createSound(it, 1f) }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.warning.sound) {
            warningSound = refreshSound()
        }.also { warningSound = refreshSound() }

        ConditionalUtils.onToggle(
            config.warning.warningIntervalSeconds,
            config.warning.warnType,
        ) {
            nextWarningMark = getNextWarningMark()
        }.also { nextWarningMark = getNextWarningMark() }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onPestTrapDataUpdate(event: PestTrapDataUpdatedEvent) {
        val data = event.data

        ChatUtils.chat("Full traps: ${data.checkFullWarnings()}")
        ChatUtils.chat("No bait traps: ${data.checkNoBaitWarnings()}")

        data.checkFullWarnings().takeIfNotEmpty()?.let {
            activeWarnings[WarningReason.TRAP_FULL] = it
        } ?: activeWarnings.remove(WarningReason.TRAP_FULL)

        data.checkNoBaitWarnings().takeIfNotEmpty()?.let {
            activeWarnings[WarningReason.NO_BAIT] = it
        } ?: activeWarnings.remove(WarningReason.NO_BAIT)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed(event: SecondPassedEvent) {
        val applicableWarnings = activeWarnings.filter { it.key in config.warning.warnReason }
        if (applicableWarnings.isEmpty() || nextWarningMark.isInFuture()) return

        warningSound?.playSound()
        applicableWarnings.forEach { (reason, traps) ->
            val displayFormat = reason.getDisplayFormat(traps)
            if (titleWarnEnabled) TitleManager.sendTitle(displayFormat, height = 2.8, fontSize = 7f)
            if (chatWarnEnabled) ChatUtils.chat(displayFormat, replaceSameMessage = true)
        }

        nextWarningMark = getNextWarningMark()
    }

    private fun WarningReason.getDisplayFormat(traps: Set<Int>): String {
        val trapCount = traps.size
        val pluralizedTraps = StringUtils.pluralize(trapCount, "Trap")
        return when (this) {
            WarningReason.TRAP_FULL -> "Full $pluralizedTraps: ${traps.getWarningFormat()}"
            WarningReason.NO_BAIT -> "No Bait $pluralizedTraps: ${traps.getWarningFormat()}"
        }
    }

    private fun Set<Int>.getWarningFormat() = joinToString("§8, ") { "§a#$it" }

    private fun List<PestTrapData>.checkFullWarnings() = this.filter {
        it.count >= PestTrapApi.MAX_PEST_COUNT_PER_TRAP
    }.map { it.number }.toSet()

    private fun List<PestTrapData>.checkNoBaitWarnings() = this.filter {
        it.baitCount == 0
    }.map { it.number }.toSet()
}
