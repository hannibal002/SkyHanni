package at.hannibal2.skyhanni.utils

import net.minecraft.util.ChatComponentText

object ChatComponentUtils {
    fun text(text: String, init: ChatComponentText.() -> Unit = {}): ChatComponentText =
        ChatComponentText(text).also(init)
}
