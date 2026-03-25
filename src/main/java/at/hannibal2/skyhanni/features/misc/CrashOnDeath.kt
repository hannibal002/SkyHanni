package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.CrashReport
import net.minecraft.client.Minecraft

@SkyHanniModule
object CrashOnDeath {

    private val config get() = SkyHanniMod.feature.misc

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerDeath(event: PlayerDeathEvent.Allow) {
        if (!config.crashOnDeath) return

        if (event.isSelf) {
            Minecraft.getInstance().delayCrash(
                CrashReport("Not Reading", Throwable("Don't toggle all the options")),
            )
        }
    }
}
