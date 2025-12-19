package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Team
import java.awt.Color

@SkyHanniModule
object DungeonTeammateOutlines {

    private val config get() = SkyHanniMod.feature.dungeon

    @HandleEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && event.type === RenderEntityOutlineEvent.Type.XRAY) {
            event.queueEntitiesToOutline { entity -> getEntityOutlineColor(entity) }
        }
    }

    private fun isEnabled() = DungeonApi.inDungeon() && config.highlightTeammates

    private fun getEntityOutlineColor(entity: Entity): Color? {
        if (entity !is RemotePlayer || entity.team == null) return null

        // Must be visible on the scoreboard
        val team = entity.team as PlayerTeam
        if (team.nameTagVisibility == Team.Visibility.NEVER) return null

        val colorFormat = StringUtils.getFormatFromString(team.colorPrefix)
        return if (colorFormat.length >= 2)
            colorFormat[1].toLorenzColor()?.toColor()
        else null
    }
}
