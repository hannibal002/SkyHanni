package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SpecialColour
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PartyMemberOutlines {
    private val config get() = SkyHanniMod.feature.misc.highlightPartyMembers

    @SubscribeEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && event.type === RenderEntityOutlineEvent.Type.NO_XRAY) {
            event.queueEntitiesToOutline { entity -> getEntityOutlineColor(entity) }
        }
    }

    private fun isEnabled() = (LorenzUtils.inSkyBlock || config.showOutsideSB) && !LorenzUtils.inDungeons && config.enabled

    private fun getEntityOutlineColor(entity: Entity): Int? {
        if (entity !is EntityOtherPlayerMP || !PartyAPI.partyMembers.contains(entity.name)) return null

        return SpecialColour.specialToChromaRGB(config.outlineColor)
    }
}