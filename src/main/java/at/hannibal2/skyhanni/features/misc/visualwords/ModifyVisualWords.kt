package at.hannibal2.hanni.features.misc.visualwords

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.mixins.transformers.AccessorMixinGuiNewChat
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.convertToFormatted
import at.hannibal2.hanni.utils.collection.TimeLimitedCache
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.minutes

@HanniModule
object ModifyVisualWords {

    private val config get() = HanniMod.feature.gui.modifyWords
    private val textCache = TimeLimitedCache<String, String>(5.minutes)

    // Replacements the user added manually via /shwords
    var userModifiedWords = mutableListOf<VisualWord>()

    // Replacements the mod added automatically for some features, april jokes, etc.
    private val modModifiedWords = mutableListOf<VisualWord>()
    private var finalWordsList = listOf<VisualWord>()
    private var debug = false

    fun update() {
        finalWordsList = modModifiedWords + userModifiedWords
        textCache.clear()
        HanniMod.visualWordsData.modifiedWords = userModifiedWords
        (Minecraft.getMinecraft().ingameGUI.chatGUI as Any as AccessorMixinGuiNewChat).refreshChat_hanni()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shdebugvisualwords") {
            description = "Prints in the console all replaced words by /shwords"
            callback { toggleDebug() }
        }
    }

    private fun toggleDebug() {
        debug = !debug
        ChatUtils.chat("Visual Words debug ${if (debug) "enabled" else "disabled"}")
        if (debug) {
            update()
        }
    }

    var changeWords = true
    fun modifyText(originalText: String?): String? {
        var modifiedText = originalText ?: return null
        if (!SkyBlockUtils.onHypixel) return originalText
        if (!config.enabled) return originalText
        if (!SkyBlockUtils.inSkyBlock && !OutsideSBFeature.MODIFY_VISUAL_WORDS.isSelected()) return originalText
        if (!changeWords) return originalText

        if (userModifiedWords.isEmpty() && HanniMod.visualWordsData.modifiedWords.isNotEmpty()) {
            userModifiedWords.addAll(HanniMod.visualWordsData.modifiedWords)
            update()
        }

        return textCache.getOrPut(originalText) {
            if (originalText.startsWith("§§")) {
                modifiedText = modifiedText.removePrefix("§§")
            } else {
                for (modifiedWord in finalWordsList) {
                    if (!modifiedWord.enabled) continue
                    val phrase = modifiedWord.phrase.convertToFormatted()

                    if (phrase.isEmpty()) continue

                    val original = modifiedText
                    val replacement = modifiedWord.replacement.convertToFormatted()
                    modifiedText = modifiedText.replace(
                        phrase, replacement, modifiedWord.isCaseSensitive(),
                    )
                    if (debug && original != modifiedText) {
                        println("Visual words Change debug: '$original' -> `$modifiedText` (`$phrase` -> `$replacement`)")
                    }
                }
            }

            modifiedText
        }
    }
}
