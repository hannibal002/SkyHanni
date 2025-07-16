package de.hype.bingonet.shared.objects

import com.google.gson.JsonObject
import de.hype.bingonet.environment.packetconfig.EnvironmentPacketConfig
import org.apache.commons.text.StringEscapeUtils
import java.util.function.Predicate

open class Message @JvmOverloads constructor(textJson: String, string: String, actionbar: Boolean = false) {
    var isActionBar: Boolean
    var source: MessageSource
    var json: String
        protected set
    private var unformattedString: String? = null
    private var playerName: String? = null
    var string: String
        protected set
    private lateinit var unformattedStringJsonEscape: String
    private var noRanks: String? = null
    private var isFromSelf: Boolean? = null
    private var selfUserName: String

    init {
        var string = string
        selfUserName = this.selfUsername
        this.json = textJson
        this.string = string
        this.isActionBar = actionbar
        if (actionbar) this.source = MessageSource.SERVER
        else this.source = MessageSource.Companion.getMessageSource(this)
        if (source == MessageSource.PRIVATE_MESSAGE_SENT) isFromSelf = true
    }

    open val selfUsername: String
        get() = EnvironmentPacketConfig.selfUsername

    fun getUnformattedString(): String {
        var unformattedString = unformattedString
        if (unformattedString != null) return unformattedString
        unformattedString = string.replace("§.".toRegex(), "").trim { it <= ' ' }
        this.unformattedString = unformattedString
        return unformattedString
    }

    val messageContent: String
        get() {
            if (source == MessageSource.SERVER) return getUnformattedString()
            return getUnformattedString().split(":".toRegex(), limit = 2).toTypedArray()[1].trim { it <= ' ' }
        }

    fun getPlayerName(): String {
        var playerName = playerName
        if (playerName != null) return playerName
        playerName = getUnformattedString()
        val columnIndex = playerName.indexOf(":")
        if (columnIndex == -1) {
            this.playerName = ""
            return ""
        }
        playerName = playerName.split(":".toRegex(), limit = 2).toTypedArray()[0]
        if (playerName.startsWith("From") || playerName.startsWith("To")) {
            playerName = playerName.replaceFirst("From".toRegex(), "").replace("To", "").trim { it <= ' ' }
        }
        if (playerName.contains(">")) {
            playerName = playerName.split(">".toRegex(), limit = 2).toTypedArray()[1]
        }
        //        playerName = playerName.replaceFirst("\\[[^\\]]*\\](?:\\s?[^\\x00-\\x7F]+\\s*?\\s?\\[[^\\]]*\\])*", "").trim()// replaces every [] and unicode character before a asci character.
        playerName = playerName.replace("[^\\x00-\\x7F]+\\s*".toRegex(), "").replace("\\[[^\\]]*\\]".toRegex(), "")
            .trim { it <= ' ' }
        if (playerName.contains(" ")) playerName = ""
        if (playerName.matches("[^a-zA-Z0-9_-]+".toRegex())) playerName = ""
        this.playerName = playerName
        return playerName
    }

    fun getNoRanks(): String {
        var noRanks = noRanks
        if (noRanks == null) {
            noRanks = getUnformattedString().replace("[^\\x00-\\x7F]+\\s*".toRegex(), "")
                .replace("\\[[^\\]]*\\]".toRegex(), "").trim { it <= ' ' }.replace("\\s+".toRegex(), " ")
            this.noRanks = noRanks
        }
        return noRanks
    }

    fun replaceInJson(replace: String, replaceWith: String) {
        try {
            this.json = json.replaceFirst(replace.toRegex(), StringEscapeUtils.escapeJson(replaceWith))
        } catch (e: Exception) {
            System.err.println(
                "String that caused the problems: Replace: $replace | Replace With: ${
                    StringEscapeUtils.escapeJson(
                        replaceWith
                    )
                } | Test: $json"
            )
            e.printStackTrace()
        }
    }

    fun contains(string: String): Boolean {
        return getUnformattedString().contains(string)
    }

    fun startsWith(string: String): Boolean {
        return getUnformattedString().startsWith(string)
    }

    fun endsWith(string: String): Boolean {
        return getUnformattedString().endsWith(string)
    }

    override fun toString(): String {
        return getUnformattedString()
    }

    fun isFromSelf(): Boolean {
        var isFromSelf = isFromSelf
        if (isFromSelf == null) {
            isFromSelf = getPlayerName() == selfUserName
            this.isFromSelf = isFromSelf
        }
        return isFromSelf
    }

    val isFromParty: Boolean
        get() = source == MessageSource.PARTY_CHAT

    val isFromGuild: Boolean
        get() = source == MessageSource.GUILD_CHAT || source == MessageSource.OFFICER_CHAT

    val isServerMessage: Boolean
        get() = source == MessageSource.SERVER

    val isMsg: Boolean
        /**
         * returns true if message is a RECEIVING msg! for either type use [.isAnyMsg]
         */
        get() = source == MessageSource.PRIVATE_MESSAGE_RECEIVE

    val isAnyMsg: Boolean
        get() = source == MessageSource.PRIVATE_MESSAGE_RECEIVE

    enum class MessageSource(val sourceName: String, val detection: Predicate<String>?, val replyCommandStart: String) {
        COOP("Coop Chat", { it.startsWith("Co-op >") }, "/cc @"),
        GUILD_CHAT("Guild Chat", { it.startsWith("Guild >") }, "/gc @"),
        OFFICER_CHAT("Guild Officer Chat", { it.startsWith("Officer >") }, "/oc @"),
        PARTY_CHAT("Party Chat", { it.startsWith("Party >") }, "/pc @"),
        PRIVATE_MESSAGE_RECEIVE("Private Message", { it.startsWith("From ") }, "/msg "),
        PRIVATE_MESSAGE_SENT("Private Message", { it.startsWith("To ") }, ""),
        NPC("Server Chat (NPC)", { it.startsWith("[NPC]") }, ""),
        ALL_CHAT("All Chat", null, "@"),
        SERVER("Server Message", null, "@"),
        SELFCREATED("Generated by Client Code", null, ""),
        ;

        companion object {
            fun getMessageSource(message: Message): MessageSource {
                val string = message.getUnformattedString()
                for (value in entries) {
                    if (value.detection == null) continue
                    if (value.detection.test(string)) return value
                }
                if (message.getPlayerName().isEmpty()) return SERVER
                return ALL_CHAT
            }
        }
    }

    companion object {
        @JvmStatic
        fun of(string: String): Message {
            val obj = JsonObject()
            obj.addProperty("text", string)
            val message = Message(obj.toString(), string)
            message.source = MessageSource.SELFCREATED
            return message
        }

        @JvmStatic
        fun tellraw(json: String): Message {
            val message = Message(json, "")
            message.source = MessageSource.SELFCREATED
            return message
        }
    }
}
