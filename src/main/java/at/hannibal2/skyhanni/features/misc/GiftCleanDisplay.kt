package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object GiftCleanDisplay {

    private val config get() = SkyHanniMod.feature.misc.giftCleanDisplay

    @HandleEvent(onlyOnSkyblock = true)
    fun onNameTagRender(event: EntityDisplayNameEvent<ArmorStand>) {
        if (!config) return

        if (event.chatComponent.string.startsWith("From:") || event.chatComponent.string.startsWith("To:")) {
            event.chatComponent = Component.empty()
        }
    }
}
