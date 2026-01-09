package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.world.entity.player.Player

class TabLine(val text: String, val type: TabStringType, val customName: String = text) {

    fun getWidth(): Int {
        val mc = Minecraft.getInstance()
        var width = mc.font.width(customName)
        if (type === TabStringType.PLAYER) {
            width += 8 + 2 // Player head
        }
        if (type === TabStringType.TEXT) {
            width += 4
        }
        return width
    }

    fun getInfo(): PlayerInfo? {
        val minecraft = Minecraft.getInstance()
        val usernameFromLine = TabStringType.usernameFromLine(text)
        return minecraft.connection?.getPlayerInfo(usernameFromLine)
    }

    private var entity: Player? = null

    fun getEntity(pLayerInfo: PlayerInfo): Player? {
        entity?.let {
            return it
        }
        val entity = MinecraftCompat.localWorld.getPlayerByUUID(pLayerInfo.profile.id)
        this.entity = entity
        return entity
    }
}
