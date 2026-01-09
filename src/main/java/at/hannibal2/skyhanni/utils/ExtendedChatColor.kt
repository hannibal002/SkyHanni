package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.chat.Component
import java.awt.Color

class ExtendedChatColor(
    val rgb: Int
) {
    constructor(hex: String) : this(ColorUtils.getColorFromHex(hex))

    fun asText(string: String = ""): Component {
        return Component.literal(string).withColor(rgb)
    }

    @SkyHanniModule
    companion object {

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shtestrainbow") {
                description = "Sends a rainbow in chat"
                category = CommandCategory.DEVELOPER_TEST
                callback {
                    val comp = Component.literal("")
                    for (i in (0 until 100)) {
                        val color = Color.HSBtoRGB(i / 100F, 1f, 1f)
                        val extendedChatColor = ExtendedChatColor(color)
                        comp.append(extendedChatColor.asText("§m "))
                    }
                    ChatUtils.chat(comp)
                }
            }
        }
    }
}
