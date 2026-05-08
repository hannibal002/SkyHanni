package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ItemUtils.isSkull
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import java.awt.Color

@SkyHanniModule
object PartyMemberOutlines {

    private val config get() = SkyHanniMod.feature.misc.highlightPartyMembers

    @HandleEvent(onlyOnSkyblockOrFeatures = [OutsideSBFeature.HIGHLIGHT_PARTY_MEMBERS])
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (!config.enabled || DungeonApi.inDungeon()) return
        if (event.type === RenderEntityOutlineEvent.Type.NO_XRAY) {
            event.queueEntitiesToOutline { entity -> getEntityOutlineColor(entity) }
        }
    }

    private fun getEntityOutlineColor(entity: Entity): Color? {
        val playerEntity = entity as? RemotePlayer? ?: return null
        if (!PartyApi.partyMembers.contains(playerEntity.name.string)) return null
        if (playerEntity.equipment[EquipmentSlot.HEAD].isSkull()) return null
        return config.outlineColor.toColor()
    }
}
