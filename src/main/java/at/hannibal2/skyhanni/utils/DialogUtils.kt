package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import kotlin.time.Duration
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex

//? if >= 26.3 {
import net.minecraft.client.Minecraft
import org.lwjgl.sdl.SDLMessageBox
//?} else {
/*import org.lwjgl.util.tinyfd.TinyFileDialogs
*///?}

@SkyHanniModule
object DialogUtils {
    //? if >= 26.3 {
    // https://wiki.libsdl.org/SDL3/SDL_MessageBoxFlags
    private const val SDL_MESSAGEBOX_INFORMATION = 0x40
    //?}

    private val dialogMutex = Mutex()

    private val popupCoroutine = CoroutineSettings(
        "openPopupWindow",
        timeout = Duration.INFINITE,
        withIOContext = true,
    ).withMutex(dialogMutex)

    //? if < 26.3 {
    /*/**
     * tinyfd replaces the whole string with `INVALID MESSAGE WITH QUOTES` if any of these are present,
     * as the Unix backends build a shell command line.
     */
    private val forbiddenCharacters = charArrayOf('"', '\'', '`')

    /**
     * Passing this as the title displays nothing and instead reports whether a graphical backend is available.
     * Without a backend, tinyfd falls back to a console prompt that would block the thread on stdin forever.
     */
    private const val QUERY_TITLE = "tinyfd_query"

    private val hasGraphicalBackend by lazy { tinyfdMessageBox(QUERY_TITLE, "") }

    /**
     * The backend the query selected, e.g. `applescript` or `basicinput` for the console fallback.
     * Only meaningful directly after a call, so it is read while still holding [dialogMutex].
     */
    private fun currentBackend(): String? = TinyFileDialogs.tinyfd_getGlobalChar("tinyfd_response")
    *///?}

    /**
     * Opens a modal message box outside the game window.
     *
     * [message] is plain text; only `\n` is supported for line breaks.
     */
    fun openPopupWindow(
        title: String,
        message: String,
        condition: () -> Boolean = { true },
    ): Job = popupCoroutine.launch {
        runCatching {
            if (!condition()) return@runCatching

            //? if >= 26.3 {
            SDLMessageBox.SDL_ShowSimpleMessageBox(
                SDL_MESSAGEBOX_INFORMATION,
                title,
                message,
                Minecraft.getInstance().window.handle(),
            )
            //?} else {
            /*if (!hasGraphicalBackend) {
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
            tinyfdMessageBox(title.stripForbiddenChars(), message.stripForbiddenChars())
            *///?}
        }.onFailure { e ->
            ErrorManager.logErrorWithData(
                e, "Failed to open a popup window",
                "title" to title,
                "message" to message,
            )
        }
    }

    //? if < 26.3 {
    /*private fun String.stripForbiddenChars(): String = filterNot { it in forbiddenCharacters }

    private fun tinyfdMessageBox(title: String, message: String): Boolean {
        return TinyFileDialogs.tinyfd_messageBox(title, message, "ok", "info", 1) != 0
    }
    *///?}

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestdialog") {
            description = "Opens a test dialog."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { openPopupWindow("SkyHanni Test Dialog", "Hello World!") }
        }
    }
}
