package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.getPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.*
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import java.net.URLDecoder
import java.net.URLEncoder

class Translator {
    private val messageContentRegex = Regex(".*: (.*)")

    // Logic for listening for a user click on a chat message is from NotEnoughUpdates

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onGuiChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        if (message.getPlayerName() == "-") return

        val editedComponent =
            if (event.chatComponent.siblings.size > 0) event.chatComponent.siblings.last() else event.chatComponent

        val clickStyle = createClickStyle(message.removeColor(), editedComponent.chatStyle)
        editedComponent.setChatStyle(clickStyle)
    }

    private fun createClickStyle(message: String, style: ChatStyle): ChatStyle {
        style.setChatClickEvent(
            ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/shsendtranslation ${messageContentRegex.find(message.removeColor())!!.groupValues[1]}"
            )
        )
        style.setChatHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                ChatComponentText("§bClick to translate!")
            )
        )
        return style
    }


    companion object {
        private val config get() = SkyHanniMod.feature.chat

        // Using my own getJSONResponse because of 1 line of difference.
        private val parser = JsonParser()
        private val builder: HttpClientBuilder =
            HttpClients.custom().setUserAgent("SkyHanni/${SkyHanniMod.version}")
                .setDefaultHeaders(
                    mutableListOf(
                        BasicHeader("Pragma", "no-cache"),
                        BasicHeader("Cache-Control", "no-cache")
                    )
                )
                .setDefaultRequestConfig(
                    RequestConfig.custom()
                        .build()
                )
                .useSystemProperties()

        /*
         * Simplified version of the JSON response:
         * [
         *   [
         *     [
         *       'translated sentence one with a space after the punctuation. '
         *       'original sentence one without a space after the punctuation.'
         *     ],
         *     [
         *       'translated sentence two without punctuation bc it's last'
         *       'original sentence two without punctuation'
         *     ]
         *   ],
         *   null,
         *   'target language as a two letter code following ISO 639-1',
         * ]
         */

        private fun getJSONResponse(urlString: String, silentError: Boolean = false): JsonElement {
            val client = builder.build()
            try {
                client.execute(HttpGet(urlString)).use { response ->
                    val entity = response.entity
                    if (entity != null) {
                        val retSrc = EntityUtils.toString(entity)
                        try {
                            return parser.parse(retSrc)
                        } catch (e: JsonSyntaxException) {
                            if (e.message?.contains("Use JsonReader.setLenient(true)") == true) {
                                println("MalformedJsonException: Use JsonReader.setLenient(true)")
                                println(" - getJSONResponse: '$urlString'")
                                LorenzUtils.debug("MalformedJsonException: Use JsonReader.setLenient(true)")
                            } else if (retSrc.contains("<center><h1>502 Bad Gateway</h1></center>")) {
                                e.printStackTrace()

                            } else {
                                CopyErrorCommand.logError(
                                    Error("Hypixel API error for url: '$urlString'", e),
                                    "Failed to load data from Hypixel API"
                                )
                            }
                        }
                    }
                }
            } catch (throwable: Throwable) {
                if (silentError) {
                    throw throwable
                } else {
                    CopyErrorCommand.logError(
                        Error("Hypixel API error for url: '$urlString'", throwable),
                        "Failed to load data from Hypixel API"
                    )
                }
            } finally {
                client.close()
            }
            return JsonObject()
        }

        private fun getTranslationToEnglish(message: String): String {
            val url =
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=" + URLEncoder.encode(
                    message,
                    "UTF-8"
                )

            var messageToSend = ""
            val layer1 = getJSONResponse(url).asJsonArray
            val language = layer1[2].toString()
            if (language == "en") return "Unable to translate!"

            val layer2 = layer1[0] as JsonArray

            for (layer3 in layer2) {
                val arrayLayer3 = layer3 as JsonArray
                val sentence = arrayLayer3[0].toString()
                val sentenceWithoutQuotes = sentence.substring(1, sentence.length - 1)
                messageToSend = "$messageToSend$sentenceWithoutQuotes"
            }
            messageToSend = "$messageToSend §7(Language: $language)"

            return URLDecoder.decode(messageToSend, "UTF-8").replace("\\", "")

        }

        private fun getTranslationFromEnglish(message: String, lang: String): String {
            val url =
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=$lang&dt=t&q=" + URLEncoder.encode(
                    message,
                    "UTF-8"
                )

            val layer1 = getJSONResponse(url).asJsonArray
            val layer2 = layer1[0] as JsonArray

            val firstSentence = (layer2[0] as JsonArray).get(0).toString()
            var messageToSend = firstSentence.substring(0, firstSentence.length - 1)
            for (sentenceIndex in 1..<layer2.size()) {
                val sentence = (layer2[sentenceIndex] as JsonArray).get(0).toString()
                val sentenceWithoutQuotes = sentence.substring(1, sentence.length - 1)
                messageToSend = "$messageToSend$sentenceWithoutQuotes"
            } // The first translated sentence only has 1 extra char at the end, but sentences after it need 1 at the front and 1 at the end removed in the substring
            messageToSend = messageToSend.substring(1, messageToSend.length)
            return URLDecoder.decode(messageToSend, "UTF-8").replace("\\", "")
        }

        fun toEnglish(args: Array<String>) {
            if (!isEnabled()) return
            var message = ""
            for (i in args) {
                message = "$message$i "
            }

            val translation = getTranslationToEnglish(message)

            if (translation == "Unable to translate!") LorenzUtils.chat("§c[SkyHanni] Unable to translate message :(")
            else LorenzUtils.chat("§e[SkyHanni] Found translation: §f$translation")
        }

        fun fromEnglish(args: Array<String>) {
            if (!isEnabled()) return
            if (args.size < 2 || args[0].length != 2) { // args[0] is the language code
                LorenzUtils.chat("§cUsage: /shcopytranslation <two letter language code (at the end of a translation)> <message>")
                return
            }
            val language = args[0]
            var message = ""
            for (i in 1..<args.size) {
                message = "$message${args[i]} "
            }

            val translation = getTranslationFromEnglish(message, language)
            LorenzUtils.chat("§e[SkyHanni] Copied translation to clipboard: $translation")
            OSUtils.copyToClipboard(translation)
        }


        // TODO reenable once the translator is working again
//        fun isEnabled() = config.translator
        fun isEnabled() = false
    }
}