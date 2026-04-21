package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object GiftCleanDisplay {

    private val config get() = SkyHanniMod.feature.misc.giftCleanDisplay

    @HandleEvent
    fun onNameTagRender(event: EntityDisplayNameEvent<ArmorStand>) {
        if (!config) return
        if (!SkyBlockUtils.inSkyBlock) return

        val name = event.chatComponent.formattedTextCompatLessResets()
        val stripped = name.replace(Regex("§."), "")

        if (stripped.startsWith("From:") || stripped.startsWith("To:")) {
            event.chatComponent = Component.empty()
        }
    }
}
