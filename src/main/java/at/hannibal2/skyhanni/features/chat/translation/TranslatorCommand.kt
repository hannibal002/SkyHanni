package at.hannibal2.skyhanni.features.chat.translation

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.AbstractSourcedChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.CoopChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.GuildChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.setClickRunCommand
import at.hannibal2.skyhanni.utils.compat.setHoverShowText
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TranslatorCommand {
    private val config get() = SkyHanniMod.feature.chat.translator
    private var lastUserChange = SimpleTimeMark.farPast()
    private fun getNativeLanguage(): String = config.languageCode.get().ifEmpty { "en" }

    fun isEnabled() = config.translateOnClick

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtranslateadvanced") {
            description = "Translates a message in an inputted language to another inputted language."
            category = CommandCategory.DEVELOPER_TEST
            legacyCallbackArgs { translateAdvancedCommand(it) }
        }
        event.registerBrigadier("shcopytranslation") {
            description = "Copy the translation of a message in another language to your clipboard.\n" +
                "Uses a 2 letter language code that can be found at the end of a translation message."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { GoogleTranslator.fromNativeLanguage(it) }
        }
        event.registerBrigadier("shtranslate") {
            description = "Translates a message in another language to your language."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { GoogleTranslator.toNativeLanguage(it, getNativeLanguage()) }
        }
    }

    // I can't handle the abstract event because NPC chat and show items inherit from it
    // TODO make it so that the plugin doesnt detect this function as an event function
    @Suppress("HandleEventInspection")
    fun replaceChatComponent(event: AbstractSourcedChatEvent.Modify) {
        if (!isEnabled()) return

        val text = event.messageComponent.getText()
        val chatComponent = event.chatComponent.copy()
        chatComponent.style = chatComponent.style.setClickRunCommand("/shtranslate $text").setHoverShowText("§bClick to translate!")
        event.replaceComponent(chatComponent, "translator")
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onAllChat(event: PlayerAllChatEvent.Modify) = replaceChatComponent(event)

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onGuildChat(event: GuildChatEvent.Modify) = replaceChatComponent(event)

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onCoopChat(event: CoopChatEvent.Modify) = replaceChatComponent(event)

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onPartyChat(event: PartyChatEvent.Modify) = replaceChatComponent(event)

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onPrivateChat(event: PrivateMessageChatEvent.Modify) = replaceChatComponent(event)

    private fun translateAdvancedCommand(args: Array<String>) {
        if (args.size < 3) {
            ChatUtils.userError("Usage: /shtranslateadvanced <source lang code> <target lang code> <message>")
            return
        }
        val sourceLanguage = args[0]
        val targetLanguage = args[1]
        val message = args.drop(2).joinToString(" ")

        CoroutineConfig("shtranslateadvanced", 10.seconds).launchCoroutine {
            val translation = GoogleTranslator.getTranslation(message, targetLanguage, sourceLanguage)
            val translatedMessage = translation?.get(0) ?: "Error!"
            val detectedLanguage = if (sourceLanguage == "auto") " ${translation?.get(1) ?: "Error!"}" else ""

            ChatUtils.clickableChat(
                "Found translation from sl: $sourceLanguage: §f$translatedMessage §7(tl: $targetLanguage)",
                onClick = {
                    CoroutineConfig("shtranslateadvanced clickableChat", 10.seconds).launchCoroutine {
                        OSUtils.copyToClipboardAsync(translatedMessage)
                    }
                },
                "§eClick to copy!\n§eOriginal message: §f$message §7(sl: $sourceLanguage$detectedLanguage)",
            )
        }
    }

    @HandleEvent
    fun onConfigLoad() {
        config.languageCode.onToggle {
            if (lastUserChange.passedSince() < 50.milliseconds) return@onToggle
            lastUserChange = SimpleTimeMark.now()

            val text = config.languageCode.get()
            if (text.isEmpty()) {
                config.languageName.set(TranslatableLanguage.ENGLISH)
            } else {
                for (language in TranslatableLanguage.entries) {
                    if (language.languageCode.equals(text, ignoreCase = true)) {
                        config.languageName.set(language)
                        return@onToggle
                    }
                }
                config.languageName.set(TranslatableLanguage.UNKNOWN)
            }
        }

        config.languageName.onToggle {
            if (lastUserChange.passedSince() < 50.milliseconds) return@onToggle
            lastUserChange = SimpleTimeMark.now()

            config.languageCode.set(config.languageName.get().languageCode)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(55, "chat.translator", "chat.translator.translateOnClick")
    }
}
