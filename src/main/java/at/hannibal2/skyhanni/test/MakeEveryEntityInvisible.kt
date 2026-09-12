package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MakeEveryEntityInvisible {
    private val config get() = DevApi.config.debug.unRenderLiterallyEveryEntity

    @HandleEvent
    private fun onEntityRenderEvent(event: CheckRenderEntityEvent<*>) {
        if (isEnabled()) event.cancel()
    }

    private fun isEnabled() = config
}
