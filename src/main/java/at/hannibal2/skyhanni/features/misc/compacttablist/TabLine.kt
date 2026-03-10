package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

class TabLine(val component: Component, val type: TabStringType, val customName: Component? = null) {

    fun getWidth(): Int {
        val mc = Minecraft.getInstance()
        var width = if (customName == null) mc.font.width(component)
        else mc.font.width(customName)

        if (type === TabStringType.PLAYER) width += 8 + 2
        if (type === TabStringType.TEXT) width += 4
        return width
    }

    fun getInfo(): PlayerInfo? {
        val minecraft = Minecraft.getInstance()
        val usernameFromLine = TabStringType.usernameFromComponent(component)
        return minecraft.connection?.getPlayerInfo(usernameFromLine)
    }

    private var entity: Player? = null

    fun getEntity(playerInfo: PlayerInfo): Player? {
        entity?.let { return it }
        val entity = MinecraftCompat.localWorld.getPlayerByUUID(playerInfo.profile.id)
        this.entity = entity
        return entity
    }
}
