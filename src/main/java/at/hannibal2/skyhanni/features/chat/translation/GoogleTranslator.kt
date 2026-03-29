package at.hannibal2.skyhanni.features.chat.translation

import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.time.Duration.Companion.seconds

object GoogleTranslator {

    @Suppress("InjectDispatcher")
    suspend fun getTranslation(
        message: String,
        targetLanguage: String,
        sourceLanguage: String = "auto",
    ): Array<String>? = withContext(Dispatchers.IO) {
        val encode = URLEncoder.encode(message, "UTF-8")
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&sl=$sourceLanguage&tl=$targetLanguage&q=$encode&dj=1"

        val messageToSend = StringBuilder()
        val (_, jsonResponse) = ApiUtils.getTypedJsonResponse<JsonObject>(url, "Google Translate API").assertSuccessWithData()
            ?: return@withContext null

        val language = jsonResponse["src"].toString() // the detected language the message is in
        val sentences = jsonResponse["sentences"] as? JsonArray ?: return@withContext null

        for (sentenceObject in sentences) {
            val obj = sentenceObject as JsonObject
            messageToSend.append(obj["trans"])
        }
        return@withContext arrayOf(messageToSend.toString(), language)
    }

    fun toNativeLanguage(args: Array<String>, nativeLanguage: String) {
        val message = args.joinToString(" ").removeColor()

        CoroutineConfig("translator toNativeLanguage", 10.seconds).launchCoroutine {
            val translation = getTranslation(message, nativeLanguage)
            val translatedMessage = translation?.get(0) ?: "Error!"
            val detectedLanguage = translation?.get(1) ?: "Error!"

            if (message == translatedMessage) {
                ChatUtils.userError("Translation is the same as the original message!")
                return@launchCoroutine
            }
            ChatUtils.clickableChat(
                "Found translation: §f$translatedMessage",
                onClick = {
                    CoroutineConfig("translator toNativeLanguage clickableChat", 10.seconds).launchCoroutine {
                        OSUtils.copyToClipboardAsync(translatedMessage)
                    }
                },
                "§eClick to copy!\n§eOriginal message: §f$message §7(Language: $detectedLanguage)",
            )
        }
    }

    fun fromNativeLanguage(args: Array<String>) {
        if (args.size < 2) {
            ChatUtils.userError("Usage: /shcopytranslation <language code (found at the end of a translation)> <message>")
            return
        }
        val language = args[0]
        val message = args.drop(1).joinToString(" ")

        CoroutineConfig("translator fromNativeLanguage").launchCoroutine {
            val translation = getTranslation(message, language, "auto")?.get(0) ?: "Error!"
            ChatUtils.clickableChat(
                "Copied §f$language §etranslation to clipboard: §f$translation",
                onClick = {
                    CoroutineConfig("translator fromNativeLanguage clickableChat").launchCoroutine {
                        OSUtils.copyToClipboardAsync(translation)
                    }
                },
                "§eClick to copy!\n§eOriginal message: §f$message",
            )
        }
    }
}
