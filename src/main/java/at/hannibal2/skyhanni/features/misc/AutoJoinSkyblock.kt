package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.system.PlatformUtils
import kotlin.time.Duration.Companion.seconds

@HanniModule
object AutoJoinSkyblock {

    private var lastJoin = SimpleTimeMark.farPast()

    @HandleEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        if (!HanniMod.feature.misc.autoJoinSkyblock) return
        if (lastJoin.passedSince() < 30.seconds) return
        lastJoin = SimpleTimeMark.now()

        val delay = if (PlatformUtils.isDevEnvironment) 5.seconds else 1.seconds
        DelayedRun.runDelayed(delay) {
            HypixelCommands.skyblock()
        }
    }
}
