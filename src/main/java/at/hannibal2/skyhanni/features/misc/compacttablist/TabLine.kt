package at.hannibal2.skyhanni.features.misc.compacttablist

import net.minecraft.client.Minecraft

class TabLine(var text: String, var type: TabStringType) {

    fun getWidth(): Int {
        val mc = Minecraft.getMinecraft()
        var width = mc.fontRendererObj.getStringWidth(text)
        if (type === TabStringType.PLAYER) {
            width += 8 + 2 // Player head
        }
        if (type === TabStringType.TEXT) {
            width += 4
        }
        return width
    }
}