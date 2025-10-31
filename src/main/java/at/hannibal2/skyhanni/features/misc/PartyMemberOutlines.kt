package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.data.PartyApi
import at.hannibal2.hanni.events.RenderEntityOutlineEvent
import at.hannibal2.hanni.features.dungeon.DungeonApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.SkyBlockUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import java.awt.Color

@HanniModule
object PartyMemberOutlines {

    private val config get() = HanniMod.feature.misc.highlightPartyMembers

    @HandleEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && event.type === RenderEntityOutlineEvent.Type.NO_XRAY) {
            event.queueEntitiesToOutline { entity -> getEntityOutlineColor(entity) }
        }
    }

    fun isEnabled() = config.enabled &&
        (SkyBlockUtils.inSkyBlock || OutsideSBFeature.HIGHLIGHT_PARTY_MEMBERS.isSelected()) && !DungeonApi.inDungeon()

    private fun getEntityOutlineColor(entity: Entity): Color? {
        if (entity !is EntityOtherPlayerMP || !PartyApi.partyMembers.contains(entity.name)) return null
        return config.outlineColor.toColor()
    }
}
