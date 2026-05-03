package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.CrashReport
import net.minecraft.client.Minecraft

@SkyHanniModule
object CrashOnJoin {
    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        Minecraft.getInstance().delayCrash(
            CrashReport(
                "SkyHanni Crash on Join",
                Throwable("Joining SkyBlock may lead to bans!")
            ),
        )
    }
}
