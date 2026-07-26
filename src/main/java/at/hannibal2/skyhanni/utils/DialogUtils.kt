package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.time.Duration
import kotlinx.coroutines.sync.Mutex

@SkyHanniModule
object DialogUtils {

    private val dialogMutex = Mutex()

    private val popupCoroutine = CoroutineSettings(
        "openPopupWindow",
        timeout = Duration.INFINITE,
        withIOContext = true,
    ).withMutex(dialogMutex)

    /**
     * tinyfd replaces the whole string with `INVALID MESSAGE WITH QUOTES` if any of these are present,
     * as the Unix backends build a shell command line.
     */
    private val forbiddenCharacters = charArrayOf('"', '\'', '`')

    /**
     * Passing this as the title displays nothing and instead reports whether a graphical backend is available.
     * Without a backend, tinyfd falls back to a console prompt that would block the thread on stdin forever.
     */
    private const val QUERY_TITLE = "tinyfd_query"

    private val hasGraphicalBackend by lazy { messageBox(QUERY_TITLE, "") }

    /**
     * The backend the query selected, e.g. `applescript` or `basicinput` for the console fallback.
     * Only meaningful directly after a call, so it is read while still holding [dialogMutex].
     */
    private fun currentBackend(): String? = TinyFileDialogs.tinyfd_getGlobalChar("tinyfd_response")

    /**
     * Opens a modal message box outside of the game window.
     *
     * [message] is plain text; only `\n` is supported for line breaks.
     */
    fun openPopupWindow(title: String, message: String) {
        popupCoroutine.launch {
            runCatching {
                if (!hasGraphicalBackend) {
                    ErrorManager.logErrorStateWithData(
                        "Failed to open a popup window",
                        "No graphical dialog backend is available",
                        "backend" to currentBackend(),
                        // tinyfd unconditionally falls back to the console when this is set, even if empty
                        "SSH_TTY" to System.getenv("SSH_TTY"),
                        "title" to title,
                        "message" to message,
                    )
                    return@runCatching
                }
                messageBox(title.stripQuotes(), message.stripQuotes())
            }.onFailure { e ->
                ErrorManager.logErrorWithData(
                    e, "Failed to open a popup window",
                    "title" to title,
                    "message" to message,
                )
            }
        }
    }

    private fun String.stripQuotes(): String = filterNot { it in forbiddenCharacters }

    private fun messageBox(title: String, message: String): Boolean {
        //? if >= 26.1 {
        return TinyFileDialogs.tinyfd_messageBox(title, message, "ok", "info", 1) != 0
        //?} else {
        /*return TinyFileDialogs.tinyfd_messageBox(title, message, "ok", "info", true)
        *///?}
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestdialog") {
            description = "Opens a test dialog."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { openPopupWindow("SkyHanni Test Dialog", "Hello World!") }
        }
    }
}
