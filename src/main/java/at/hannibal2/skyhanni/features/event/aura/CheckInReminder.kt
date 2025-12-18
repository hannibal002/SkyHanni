package at.hannibal2.skyhanni.features.event.aura

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledFeaturesJson
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CheckInReminder {

    private val config get() = SkyHanniMod.feature.event.aura

    @Suppress("MaxLineLength")
    private val checkInPattern by RepoPattern.list(
        "misc.aura.checkin",
        "§e\\[NPC] Goon§f: §rAura thanks you for your cooperation\\. Come back in 30 minutes if you do not want to face the §oconsequences\\.",
        "§e\\[NPC] Goon§f: §rNow go, be a productive citizen for our great society\\.",
    )

    private val sound by lazy { SoundUtils.createSound("random.anvil_land", 0.0f) }

    private var auraSurveillance = true
    private var lastCheckIn = SimpleTimeMark.farPast()
    private var lastWarning = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        if (checkInPattern.anyMatches(event.message)) {
            lastCheckIn = SimpleTimeMark.now()
            DelayedRun.runNextTick {
                ChatUtils.chat("§aStat debuff timer reset!")
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (!auraSurveillance || !isEnabled()) return
        if (lastCheckIn.passedSince() < 30.minutes) return
        if (lastWarning.passedSince() < 1.minutes) return

        sound.playSound()
        TitleManager.sendTitle(
            "§cStat Debuff!",
            duration = 5.seconds,
            addType = TitleManager.TitleAddType.FORCE_FIRST,
        )
        ChatUtils.clickToActionOrDisable(
            "Talk to a Surveillance Goon to get rid of your §c10% stat debuff§e!",
            config::checkInReminder,
            actionName = "warp to the Hub",
            action = { HypixelCommands.warp("hub") },
        )
        lastWarning = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val disabledFeatures = event.getConstant<DisabledFeaturesJson>("DisabledFeatures")
        auraSurveillance = disabledFeatures.features?.get("aura_surveillance") ?: true
    }

    private fun isEnabled() = config.checkInReminder
}
