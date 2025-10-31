package at.hannibal2.hanni.features.nether.kuudra

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ClickType
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.entity.EntityClickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils.isNpc
import net.minecraft.entity.player.EntityPlayer

@HanniModule
object KuudraProfileViewerBlocker {

    private val config get() = HanniMod.feature.crimsonIsle

    @HandleEvent(onlyOnIsland = IslandType.KUUDRA_ARENA)
    fun onClickEntity(event: EntityClickEvent) {
        if (!config.disableProfileViewerInKuudra) return

        if (event.clickType != ClickType.RIGHT_CLICK) return
        if (event.clickedEntity !is EntityPlayer) return
        if (event.clickedEntity.isNpc()) return

        event.cancel()
    }
}
