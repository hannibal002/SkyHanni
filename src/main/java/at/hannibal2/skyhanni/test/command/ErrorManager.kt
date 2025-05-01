package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.ChangedChatErrorsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.RepoErrorData
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.client.Minecraft
import net.minecraft.crash.CrashReport
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object ErrorManager {

    // random id -> error message
    private val errorMessages = mutableMapOf<String, String>()
    private val fullErrorMessages = mutableMapOf<String, String>()
    private val cache = TimeLimitedSet<CachedError>(10.minutes)
    private var repoErrors: List<RepoErrorData> = emptyList()

    private val breakAfter = listOf(
        "at at.hannibal2.skyhanni.config.commands.Commands\$createCommand",
        "at net.minecraftforge.fml.common.eventhandler.EventBus.post",
        "at at.hannibal2.skyhanni.mixins.hooks.NetHandlerPlayClientHookKt.onSendPacket",
        "at net.minecraft.client.main.Main.main",
        "at.hannibal2.skyhanni.api.event.EventListeners.createZeroParameterConsumer",
        "at.hannibal2.skyhanni.api.event.EventListeners.createSingleParameterConsumer",
    )

    private val replace = mapOf(
        "at.hannibal2.skyhanni." to "SH.",
        "io.moulberry.notenoughupdates." to "NEU.",
        "net.minecraft." to "MC.",
        "net.minecraftforge.fml." to "FML.",
    )

    private val replaceEntirely = mapOf(
        "at.hannibal2.skyhanni.api.event.EventListeners.createZeroParameterConsumer" to "<Skyhanni event post>",
        "at.hannibal2.skyhanni.api.event.EventListeners.createSingleParameterConsumer" to "<Skyhanni event post>",
    )

    private val ignored = listOf(
        "at java.lang.Thread.run",
        "at java.util.concurrent.",
        "at java.lang.reflect.",
        "at net.minecraft.network.",
        "at net.minecraft.client.Minecraft.addScheduledTask(",
        "at net.minecraftforge.fml.common.network.handshake.",
        "at net.minecraftforge.fml.common.eventhandler.",
        "at net.fabricmc.devlaunchinjector.",
        "at io.netty.",
        "at com.google.gson.internal.",
        "at sun.reflect.",

        "at at.hannibal2.skyhanni.config.commands.SimpleCommand.",
        "at at.hannibal2.skyhanni.config.commands.Commands\$createCommand\$1.processCommand",
        "at at.hannibal2.skyhanni.test.command.ErrorManager.logError",
        "at at.hannibal2.skyhanni.test.command.ErrorManager.skyHanniError",
        "at at.hannibal2.skyhanni.api.event.SkyHanniEvent.post",
        "at at.hannibal2.skyhanni.api.event.EventHandler.post",
        "at net.minecraft.launchwrapper.",
    )

    // this hides the whole stack trace of one error of the list of all errors in the error message
    // where the error class name is the key and the first line contains one of the entries in the list of values
    private val skipErrorEntry = emptyMap<String, List<String>>()
//         "java.lang.reflect.InvocationTargetException" to listOf(
//             "EventListeners.createZeroParameterConsumer",
//             "EventListeners.createSingleParameterConsumer",
//         ),
//     )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shtestreseterrorcache") {
            description = "Resets the cache of errors."
            category = CommandCategory.DEVELOPER_TEST
            callback {
                cache.clear()
                ChatUtils.chat("Error cache reset.")
            }
        }
    }

    // Extra data from last thrown error
    private var cachedExtraData: String? = null

    // throw an error, best to not use it if not absolutely necessary
    fun skyHanniError(message: String, vararg extraData: Pair<String, Any?>): Nothing {
        buildExtraDataString(extraData)?.let {
            cachedExtraData = it
        }
        throw IllegalStateException(message.removeColor())
    }

    private fun copyError(errorId: String) {
        val fullErrorMessage = KeyboardManager.isModifierKeyDown()
        val errorMessage = if (fullErrorMessage) {
            fullErrorMessages[errorId]
        } else {
            errorMessages[errorId]
        }
        val name = if (fullErrorMessage) "Full error" else "Error"
        ChatUtils.chat(
            errorMessage?.let {
                OSUtils.copyToClipboard(it)
                "$name copied into the clipboard, please report it on the SkyHanni discord!"
            } ?: "Error id not found!",
        )
    }

    inline fun crashInDevEnv(reason: String, t: (String) -> Throwable = { RuntimeException(it) }) {
        if (!PlatformUtils.isDevEnvironment) return
        Minecraft.getMinecraft().crashed(CrashReport("SkyHanni - $reason", t(reason)))
    }

    // just log for debug cases
    fun logErrorStateWithData(
        userMessage: String,
        internalMessage: String,
        vararg extraData: Pair<String, Any?>,
        ignoreErrorCache: Boolean = false,
        noStackTrace: Boolean = false,
        betaOnly: Boolean = false,
        condition: () -> Boolean = { true },
    ): Boolean {
        if (extraData.isNotEmpty()) {
            cachedExtraData = null
        }
        return logError(
            IllegalStateException(internalMessage),
            userMessage,
            ignoreErrorCache,
            noStackTrace,
            *extraData,
            betaOnly = betaOnly,
            condition = condition,
        )
    }

    // log with stack trace from other try catch block
    fun logErrorWithData(
        throwable: Throwable,
        message: String,
        vararg extraData: Pair<String, Any?>,
        ignoreErrorCache: Boolean = false,
        noStackTrace: Boolean = false,
        betaOnly: Boolean = false,
    ): Boolean = logError(throwable, message, ignoreErrorCache, noStackTrace, *extraData, betaOnly = betaOnly)

    data class CachedError(val className: String, val lineNumber: Int, val errorMessage: String)

    private fun logError(
        originalThrowable: Throwable,
        message: String,
        ignoreErrorCache: Boolean,
        noStackTrace: Boolean,
        vararg extraData: Pair<String, Any?>,
        betaOnly: Boolean = false,
        condition: () -> Boolean = { true },
    ): Boolean {
        if (betaOnly && !SkyHanniMod.isBetaVersion) return false
        val throwable = originalThrowable.maybeSkipError()
        if (!ignoreErrorCache) {
            val cachedError = throwable.stackTrace.getOrNull(0)?.let {
                CachedError(it.fileName ?: "<unknown>", it.lineNumber, message)
            } ?: CachedError("<empty stack trace>", 0, message)
            if (cachedError in cache) return false
            cache.add(cachedError)
        }
        if (!condition()) return false

        Error(message, throwable).printStackTrace()
        MinecraftCompat.localPlayerOrNull ?: return false

        val fullStackTrace: String
        val stackTrace: String

        if (noStackTrace) {
            fullStackTrace = "<no stack trace>"
            stackTrace = "<no stack trace>"
        } else {
            fullStackTrace = throwable.getCustomStackTrace(true).joinToString("\n")
            stackTrace = throwable.getCustomStackTrace(false).joinToString("\n")
        }
        val randomId = StringUtils.generateRandomId()

        val extraDataString = getExtraDataOrCached(extraData)
        val rawMessage = message.removeColor()
        errorMessages[randomId] = "```\nSkyHanni ${SkyHanniMod.VERSION}: $rawMessage\n \n$stackTrace\n$extraDataString```"
        fullErrorMessages[randomId] =
            "```\nSkyHanni ${SkyHanniMod.VERSION}: $rawMessage\n(full stack trace)\n \n$fullStackTrace\n$extraDataString```"

        val finalMessage = buildFinalMessage(message) ?: return false
        ChatUtils.clickableChat(
            "§c[SkyHanni-${SkyHanniMod.VERSION}]: $finalMessage Click here to copy the error into the clipboard.",
            onClick = { copyError(randomId) },
            "§eClick to copy!",
            prefix = false,
        )
        return true
    }

    private fun getExtraDataOrCached(extraData: Array<out Pair<String, Any?>>): String {
        cachedExtraData?.let {
            cachedExtraData = null
            if (extraData.isEmpty()) {
                return it
            }
        }
        return buildExtraDataString(extraData).orEmpty()
    }

    private fun buildFinalMessage(message: String): String? {
        var finalMessage = message
        val rawMessage = message.removeColor()

        var hideError = false
        for (repoError in repoErrors) {
            for (string in repoError.messageStartsWith) {
                if (rawMessage.startsWith(string)) {
                    hideError = true
                }
            }
            for (string in repoError.messageExact) {
                if (rawMessage == string) {
                    hideError = true
                }
            }
            if (hideError) {
                repoError.replaceMessage?.let {
                    finalMessage = it
                    hideError = false
                }
                repoError.customMessage?.let {
                    ChatUtils.userError(it)
                    return null
                }
                break
            }
        }

        if (finalMessage.last() !in ".?!") {
            finalMessage += "§c."
        }
        return if (hideError) null else finalMessage
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ChangedChatErrorsJson>("ChangedChatErrors")
        val version = SkyHanniMod.modVersion

        repoErrors = data.changedErrorMessages.filter { it.fixedIn == null || version < it.fixedIn }
    }

    private fun buildExtraDataString(extraData: Array<out Pair<String, Any?>>): String? {
        val extraDataString = if (extraData.isNotEmpty()) {
            val builder = StringBuilder()
            for ((key, value) in extraData) {
                builder.append(key)
                builder.append(": ")
                if (value is Iterable<*>) {
                    builder.append("\n")
                    for (line in value) {
                        builder.append(" - '$line'")
                        builder.append("\n")
                    }
                } else {
                    builder.append("'$value'")
                }
                builder.append("\n")
            }
            "\nExtra data:\n$builder"
        } else null
        return extraDataString
    }

    private fun Throwable.getCustomStackTrace(
        fullStackTrace: Boolean,
        parent: List<String> = emptyList(),
    ): List<String> = buildList {
        add("Caused by ${this@getCustomStackTrace.javaClass.name}: $message")

        for (traceElement in stackTrace) {
            val text = "\tat $traceElement"
            if (!fullStackTrace && text in parent) {
                break
            }
            var visualText = text
            if (!fullStackTrace) {
                for ((from, to) in replaceEntirely) {
                    if (visualText.contains(from)) {
                        visualText = to
                        break
                    }
                }
                for ((from, to) in replace) {
                    visualText = visualText.replace(from, to)
                }
            }
            if (!fullStackTrace && breakAfter.any { text.contains(it) }) {
                add(visualText)
                break
            }
            if (ignored.any { text.contains(it) }) continue
            add(visualText)
        }

        if (this === cause) {
            add("<Infinite recurring causes>")
            return@buildList
        }

        cause?.let {
            addAll(it.getCustomStackTrace(fullStackTrace, this))
        }
    }

    // tries to use the cause instead of the actual error. Returns itself if doesnt work.
    fun Throwable.maybeSkipError(): Throwable {
        val cause = cause ?: return this
        val causeClassName = this@maybeSkipError.javaClass.name
        val breakOnFirstLine = skipErrorEntry[causeClassName]

        for (traceElement in stackTrace) {
            val line = traceElement.toString()
            breakOnFirstLine?.let { list ->
                if (list.any { line.contains(it) }) return cause
            }
        }

        return this
    }
}
