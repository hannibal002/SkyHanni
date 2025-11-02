package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.entity.player.PlayerEntity

class TabLine(val text: String, val type: TabStringType, val customName: String = text) {

    fun getWidth(): Int {
        val mc = MinecraftClient.getInstance()
        var width = mc.textRenderer.getWidth(customName)
        if (type === TabStringType.PLAYER) {
            width += 8 + 2 // Player head
        }
        if (type === TabStringType.TEXT) {
            width += 4
        }
        return width
    }

    fun getInfo(): PlayerListEntry? {
        val minecraft = MinecraftClient.getInstance()
        val usernameFromLine = TabStringType.usernameFromLine(text)
        return minecraft.networkHandler?.getPlayerListEntry(usernameFromLine)
    }

    private var entity: PlayerEntity? = null

    fun getEntity(pLayerInfo: PlayerListEntry): PlayerEntity? {
        entity?.let {
            return it
        }
        val entity = MinecraftCompat.localWorld.getPlayerByUuid(pLayerInfo.profile.id)
        this.entity = entity
        return entity
    }
}
