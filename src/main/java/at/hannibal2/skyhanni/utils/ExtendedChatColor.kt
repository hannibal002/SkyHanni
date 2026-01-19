package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.italic
import at.hannibal2.skyhanni.utils.compat.stackHover
import at.hannibal2.skyhanni.utils.compat.strikethrough
import at.hannibal2.skyhanni.utils.compat.underlined
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting
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
                    val comp = componentBuilder {
                        for (i in (0 until 100)) {
                            val color = Color.HSBtoRGB(i / 100F, 1f, 1f)
                            // its funny this doesn't even use extended chat color anymore...
                            append(" ") {
                                withColor(color)
                                strikethrough = true
                            }
                        }
                    }
                    ChatUtils.chat(comp)
                }
            }
            event.registerBrigadier("shtestcomponentbuilder") {
                description = "Sends an example componentBuilder output in chat"
                category = CommandCategory.DEVELOPER_TEST
                simpleCallback {
                    ChatUtils.chat(
                        componentBuilder {
                            withColor(1146986)
                            append("Hello this is the componentBuilder ")
                            append("example") {
                                underlined = true
                                hover = Component.literal("ඞ")
                                withColor(TextHelper.getChromaColorStyle())
                            }
                            append(". ")
                            append {
                                withColor(10238139)
                                append("You can do epic things ")
                                append("like ") {
                                    italic = true
                                }
                                append("Click Events!") {
                                    command = "/ac lol u clicked"
                                    hover = componentBuilder {
                                        append("Click for epic chat message") {
                                            strikethrough = true
                                            withColor(ChatFormatting.RED)
                                        }
                                    }
                                    underlined = true
                                }
                            }
                            appendWithColor("", 16737792) {
                                append(" And overall it makes working with ")
                                append("Components") {
                                    bold = true
                                }
                                append(" much nicer.")
                            }
                            val heldItem = InventoryUtils.getItemInHand()
                            if (heldItem != null) {
                                append(" Look its the item you are holding!") {
                                    stackHover = heldItem
                                    withColor("#349EFB")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
