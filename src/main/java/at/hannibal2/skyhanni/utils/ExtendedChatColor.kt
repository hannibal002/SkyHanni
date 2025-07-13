package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.Text
import java.awt.Color
//#if MC > 1.21
//$$ import net.minecraft.text.MutableText
//#endif

class ExtendedChatColor(
    val rgb: Int,
    val hasAlpha: Boolean = false,
) {
    constructor(hex: String, hasAlpha: Boolean = false) : this(ColorUtils.getColorFromHex(hex), hasAlpha)

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        val hexCode = rgb.toUInt().toString(16)
            .padStart(8, '0')
            .drop(if (hasAlpha) 0 else 2)
        stringBuilder.append("§#")
        for (code in hexCode) {
            stringBuilder.append('§').append(code)
        }
        stringBuilder.append("§/")
        return stringBuilder.toString()
    }

    fun asText(): Text {
        //#if MC < 1.21
        return Text.of(this.toString())
        //#else
        //$$ return (Text.of("") as MutableText).withColor(rgb)
        //#endif
    }

    @SkyHanniModule
    companion object {

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shtestrainbow") {
                description = "Sends a rainbow in chat"
                category = CommandCategory.DEVELOPER_TEST
                callback {
                    val string = StringBuilder()
                    for (i in (0 until 100)) {
                        val color = Color.HSBtoRGB(i / 100F, 1f, 1f)
                        val extendedChatColor = ExtendedChatColor(color, false)
                        string.append("$extendedChatColor§m ")
                    }
                    ChatUtils.chat(string.toString())
                }
            }
        }
    }
}
