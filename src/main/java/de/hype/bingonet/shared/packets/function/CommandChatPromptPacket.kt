package de.hype.bingonet.shared.packets.function

import com.google.gson.annotations.Expose
import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.Message
import de.hype.bingonet.shared.objects.Message.Companion.tellraw
import org.apache.commons.text.StringEscapeUtils
import java.lang.String
import kotlin.Boolean
import kotlin.Double

class CommandChatPromptPacket(private var commands: MutableList<CommandRecord>, private val message: String) :
    AbstractPacket() {
    /**
     * @return The for maliciously checked command list
     */
    fun getCommands(): MutableList<CommandRecord> {
        commands = commands.filter { !it.isMalicious }.toMutableList()
        return commands
    }

    val printMessage: Message
        get() {
            //{"jformat":8,"jobject":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Bingo Net is a mod that you use"}],"text":"Bingo Net Server "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"is suggesting you to execute commands. "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[],"text":"(Hover for List) "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"To execute them press "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"keybind":"Chat Prompt Yes / Open Menu"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Attached Message: "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_gray","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"@message"}],"command":"%s","jtemplate":"tellraw"}
            val base =
                "[{\"text\":\"Bingo Net Server \",\"color\":\"gold\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Bingo Net is a mod that you use\"]}},\"is suggesting you to execute commands. \",{\"text\":\"(Hover for List) \",\"color\":\"green\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@hover\"]}},\"To execute them press \",{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"gold\"},\"\\n\",\"@warnings\",\"Attached Message: \",{\"text\":\"@message\",\"color\":\"dark_gray\"}]"
            val warningString = if (commands
                    .any { it.command.startsWith("/warp") }
            ) "§cThe Command List contains a command that can cause changing Servers!" else ""
            return tellraw(
                base.replace(
                    "@message",
                    StringEscapeUtils.escapeJson(message.toString()).replace(
                        "@hover",
                        String.join(
                            "\n",
                            commands.map { it.command }.toMutableList()
                        )
                    )
                )
                    .replace("@warnings", warningString)
            )
        }

    class CommandRecord {
        var command: kotlin.String
        var delay: Double

        constructor(command: kotlin.String, delay: Double) {
            this.command = command
            this.delay = delay
        }

        constructor(command: kotlin.String) {
            this.command = command
            this.delay = 0.0
        }

        val isMalicious: Boolean
            /**
             * This method checks whether a command could be malicious.
             */
            get() {
                //I decided to allow some commands which are not used by me but that are not malicious.
                if (command.startsWith("/p ")) return false
                if (command.startsWith("/party ")) return false
                if (command.startsWith("/boop ")) return false

                //These commands get a prefix so its an indicator they were send by the mod.
                if (command.startsWith("/msg ")) {
                    if (!command.startsWith("/msg $baseModPrefix")) {
                        command = command.replace(
                            "/msg ",
                            "/msg $baseModPrefix: "
                        )
                    }
                    return false
                }
                if (command.startsWith("/r ")) {
                    if (!command.startsWith("/r $baseModPrefix")) {
                        command =
                            command.replace("/r ", "/r $baseModPrefix: ")
                    }
                    return false
                }
                if (command.startsWith("/pc ")) {
                    if (!command.startsWith("/pc $baseModPrefix")) {
                        command = command.replace(
                            "/pc ",
                            "/pc $baseModPrefix: "
                        )
                    }
                    return false
                }
                if (command.startsWith("/ac ")) {
                    if (!command.startsWith("/ac $baseModPrefix")) {
                        command = command.replace(
                            "/ac ",
                            "/ac $baseModPrefix: "
                        )
                    }
                    return false
                }
                if (command.startsWith("/gc ")) {
                    if (!command.startsWith("/gc $baseModPrefix")) {
                        command = command.replace(
                            "/gc ",
                            "/gc $baseModPrefix: "
                        )
                    }
                    return false
                }

                //This command prints a extra warning message into chat
                if (command.startsWith("/warp ")) return false
                return true
            }
    }

    companion object {
        @Expose(serialize = false, deserialize = false)
        private const val baseModPrefix = "Bingo Net(Client Mod)"
    }
}
