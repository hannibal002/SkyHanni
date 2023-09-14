package at.hannibal2.skyhanni.data

import net.minecraft.util.ChatComponentText

class CustomChatComponentText(text: String) : ChatComponentText(text) {
    fun setCustomText(text: String) {
        this.text = text
    }
}