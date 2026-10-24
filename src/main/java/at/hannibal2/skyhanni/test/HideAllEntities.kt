package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.onEnable
import net.minecraft.world.entity.Entity

@SkyHanniModule
object HideAllEntities {
    private val config get() = DevApi.config.debug

    @HandleEvent(onlyOnSkyblock = true)
    private fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (isEnabled()) event.cancel()
    }

    @HandleEvent
    private fun onConfigLoad() {
        config.hideAllEntities.onEnable {
            ChatUtils.chat("use /shrendertoggle or disable Hide All Entities in dev config to see entities again.")
        }
    }

    private fun isEnabled() = config.hideAllEntities.get()

}
