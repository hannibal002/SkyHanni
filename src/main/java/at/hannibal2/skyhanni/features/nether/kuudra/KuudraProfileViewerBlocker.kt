package at.hannibal2.skyhanni.features.nether.kuudra

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import net.minecraft.entity.player.EntityPlayer

@SkyHanniModule
object KuudraProfileViewerBlocker {

    private val config get() = SkyHanniMod.feature.crimsonIsle

    @HandleEvent(onlyOnIsland = IslandType.KUUDRA_ARENA)
    fun onClickEntity(event: EntityClickEvent) {
        if (!config.disableProfileViewerInKuudra) return

        if (event.clickType != ClickType.RIGHT_CLICK) return
        if (event.clickedEntity !is EntityPlayer) return
        if (event.clickedEntity.isNpc()) return

        event.cancel()
    }
}
