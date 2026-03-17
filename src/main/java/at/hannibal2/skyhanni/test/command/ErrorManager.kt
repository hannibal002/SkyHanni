package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.data.jsonobjects.repo.ChangedChatErrorsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.ErrorManagerJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.RepoErrorData
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.ClientConnectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.CrashReport
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.minutes

/** Crashes if [value] is false and in developer environment */
fun requireDevEnv(value: Boolean) = requireDevEnv(value, null)

/** Crashes if [value] is false and in developer environment */
fun requireDevEnv(value: Boolean, lazyMessage: (() -> Any)?) {
    if (!value) {
        val msg = lazyMessage?.invoke()?.toString()
        val message = "Failed requirement in Dev Environment" + msg?.let { ": $it" }.orEmpty()
        ErrorManager.crashInDevEnv(message)
    }
}

@SkyHanniModule
object ErrorManager {

    // These are left as mutable properties so that they can be updated by the repo
    // however we still want to leave some defaults in here in case a repo reload fails
    // and tries to call ErrorManager methods.
    // <editor-fold defaultstate="collapsed" desc="Error Manager Data (semi-REPO controlled)">
    private var breakAfter: List<String> = listOf(
        "at at.hannibal2.skyhanni.config.commands.Commands\$createCommand",
        "at net.minecraftforge.fml.common.eventhandler.EventBus.post",
        "at at.hannibal2.skyhanni.mixins.hooks.NetHandlerPlayClientHookKt.onSendPacket",
        "at net.minecraft.client.main.Main.main",
        "at.hannibal2.skyhanni.api.event.EventListeners.createZeroParameterConsumer",
        "at.hannibal2.skyhanni.api.event.EventListeners.createSingleParameterConsumer",
        "at.hannibal2.skyhanni.api.event.SkyHanniEvent.post",
    )
    private var replacements: Map<String, String> = mapOf(
        "at.hannibal2.skyhanni." to "SH.",
        "io.moulberry.notenoughupdates." to "NEU.",
        "net.minecraft." to "MC.",
        "net.minecraftforge.fml." to "FML.",
        "knot//" to "",
        "java.base/" to "",
    )
    private var entireReplacements: Map<String, String> = mapOf(
        "at.hannibal2.skyhanni.api.event.EventListeners.createZeroParameterConsumer" to "<Skyhanni event post>",
        "at.hannibal2.skyhanni.api.event.EventListeners.createSingleParameterConsumer" to "<Skyhanni event post>",
    )
    private var ignored: List<String> = listOf(
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
    // </editor-fold>

    // random id -> error message
    private val errorMessages = mutableMapOf<String, String>()
    private val fullErrorMessages = mutableMapOf<String, String>()
    private val cache = TimeLimitedSet<CachedError>(10.minutes)
    private var repoErrors: List<RepoErrorData> = emptyList()

    // this hides the whole stack trace of one error of the list of all errors in the error message
    // where the error class name is the key and the first line contains one of the entries in the list of values
    private val skipErrorEntry = emptyMap<String, List<String>>()

    private val copyErrorCoroutine = CoroutineConfig("error manager copy error")

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestreseterrorcache") {
            description = "Resets the cache of errors."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                cache.clear()
                ChatUtils.chat("Error cache reset.")
            }
        }
        event.registerBrigadier("shthrowerror") {
            description = "Throws an error to test error manager."
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback {
                logErrorWithData(NullPointerException(), "Manually triggered error!")
            }
        }
    }


    // Extra data from last thrown error
    private var cachedExtraData: String? = null

    // throw an error, best to not use it if not absolutely necessary
    // when extraData is not used, rather call kotlin's `error()`
    fun skyHanniError(message: String, vararg extraData: Pair<String, Any?>): Nothing {
        buildExtraDataString(extraData)?.let {
            cachedExtraData = it
        }
        throw IllegalStateException(message.removeColor())
    }

    private fun copyError(errorId: String) = copyErrorCoroutine.launch {
        val fullErrorMessage = KeyboardManager.isModifierKeyDown()
        val errorMessage = if (fullErrorMessage) {
            fullErrorMessages[errorId]
        } else {
            errorMessages[errorId]
        }
        val name = if (fullErrorMessage) "Full error" else "Error"
        ChatUtils.chat(
            errorMessage?.let {
                val copied = ClipboardUtils.copyToClipboardAsync(it).await() ?: false
                if (copied) "$name copied into the clipboard, please report it on the SkyHanni discord!"
                else "$name could not be copied to clipboard!"
            } ?: "Error id not found!",
        )
    }

    inline fun crashInDevEnv(reason: String, t: (String) -> Throwable = { RuntimeException(it) }) {
        if (!PlatformUtils.isDevEnvironment) return
        Minecraft.getInstance().delayCrash(CrashReport("SkyHanni - $reason", t(reason)))
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
        ).crashIfNotYetOnAServer()
    }

    // log with stack trace from other try catch block
    fun logErrorWithData(
        throwable: Throwable,
        message: String = throwable.message ?: "message is null",
        vararg extraData: Pair<String, Any?>,
        ignoreErrorCache: Boolean = false,
        noStackTrace: Boolean = false,
        betaOnly: Boolean = false,
    ): Boolean = logError(throwable, message, ignoreErrorCache, noStackTrace, *extraData, betaOnly = betaOnly).crashIfNotYetOnAServer()

    data class CachedError(val className: String, val lineNumber: Int, val errorMessage: String)

    enum class ErrorState {
        LOGGED,
        BLOCKED_NOT_NEEDED,
        BLOCKED_CAN_NOT_SHOW,
    }

    @Suppress("ReturnCount")
    private fun logError(
        originalThrowable: Throwable,
        message: String,
        ignoreErrorCache: Boolean,
        noStackTrace: Boolean,
        vararg extraData: Pair<String, Any?>,
        betaOnly: Boolean = false,
        condition: () -> Boolean = { true },
    ): ErrorState {
        if (!condition()) return ErrorState.BLOCKED_NOT_NEEDED

        // TODO add missing debug enabled check
        if (betaOnly && !SkyHanniMod.isBetaVersion) return ErrorState.BLOCKED_NOT_NEEDED

        val throwable = originalThrowable.maybeSkipError()
        if (!ignoreErrorCache) {
            val cachedError = throwable.stackTrace.getOrNull(0)?.let {
                CachedError(it.fileName ?: "<unknown>", it.lineNumber, message)
            } ?: CachedError("<empty stack trace>", 0, message)
            if (cachedError in cache) return ErrorState.BLOCKED_NOT_NEEDED
            cache.add(cachedError)
        }

        Error(message, throwable).printStackTrace()

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
        val label = getLabel()
        errorMessages[randomId] = "```\n$label: $rawMessage\n \n$stackTrace\n$extraDataString```"
        fullErrorMessages[randomId] =
            "```\n$label: $rawMessage\n(full stack trace)\n \n$fullStackTrace\n$extraDataString```"

        val isConnected = MinecraftCompat.localPlayerOrNull != null

        val finalMessage = buildFinalMessage(message) ?: return ErrorState.BLOCKED_CAN_NOT_SHOW
        if (!isConnected) {
            errorsToShowOnJoin[randomId] = finalMessage
            return ErrorState.BLOCKED_CAN_NOT_SHOW
        }
        ChatUtils.clickableChat(
            "§c[$label]: $finalMessage Click here to copy the error into the clipboard.",
            onClick = { copyError(randomId) },
            "§eClick to copy!",
            prefix = false,
        )
        return ErrorState.LOGGED
    }

    private fun getLabel(): String {
        val shVersion = SkyHanniMod.VERSION
        val mcVersion = PlatformUtils.MC_VERSION
        return "SkyHanni $shVersion $mcVersion"
    }

    // random id -> final message
    private val errorsToShowOnJoin = mutableMapOf<String, String>()

    private fun ErrorState.crashIfNotYetOnAServer(): Boolean {
        if (this == ErrorState.BLOCKED_NOT_NEEDED) return false

        // TODO find way to properly do this before the config loads
        val devCrash = false
        if (devCrash) {
            if (!MinecraftData.hasLeftMainScreen) {
                fullErrorMessages.entries.firstOrNull()?.value?.let {
                    crashInDevEnv("error message in main screen: \n \n \n \n $it")
                } ?: run {
                    crashInDevEnv("error message in main screen")
                }
            }
        }

        return this == ErrorState.LOGGED
    }

    @HandleEvent(ClientConnectEvent::class)
    fun onClientConnect() {
        if (errorsToShowOnJoin.isEmpty()) return
        val label = getLabel()
        val state = if (MinecraftData.hasLeftMainScreen) "During startup" else "While not on a server"
        ChatUtils.chat("§e$state, $label found ${errorsToShowOnJoin.size} errors!", prefix = false)
        ChatUtils.chat("  §7(Click on the message to copy the error into the clipboard)", prefix = false)
        for ((id, message) in errorsToShowOnJoin) {
            ChatUtils.clickableChat(
                "§7- §c$message",
                onClick = { copyError(id) },
                "§eClick to copy!",
                prefix = false,
            )
        }
        errorsToShowOnJoin.clear()
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
        val repoData = event.getConstant<ErrorManagerJson>("ErrorManager")
        breakAfter = repoData.breakAfter
        replacements = repoData.replacements
        entireReplacements = repoData.entireReplacements
        ignored = repoData.ignored

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
                for ((from, to) in entireReplacements) {
                    if (visualText.contains(from)) {
                        visualText = to
                        break
                    }
                }
                for ((from, to) in replacements) {
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

    // tries to use the cause instead of the actual error. Returns itself if it doesn't work.
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
