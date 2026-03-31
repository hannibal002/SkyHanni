package at.hannibal2.skyhanni.features.misc.discordrpc

import at.hannibal2.skyhanni.utils.collection.CollectionUtils.associateNotNull
import java.nio.file.Files
import kotlin.io.path.Path

/**
 * Discovers and opens a [DiscordIPCPipe] to a running Discord client.
 *
 * On Windows, scans `\\.\pipe\discord-ipc-{0..9}`.
 * On Unix, searches candidate directories derived from environment variables,
 * UID-based fallbacks, and Flatpak per-app forwarded socket paths.
 *
 * When discovery fails on Unix, [diagnoseUnix] checks for a Flatpak sandbox restriction
 * and surfaces an actionable message and the [DiscordIPCException.isSandboxIssue] flag.
 */
object DiscordIPCPipeManager {

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    /**
     * Opens a [DiscordIPCPipe] to a running Discord client.
     *
     * @param onDebugInfo Called with diagnostic key-value pairs on failure.
     * @throws DiscordIPCException If no pipe can be found or connected.
     */
    fun open(onDebugInfo: (Map<String, String>) -> Unit = {}): DiscordIPCPipe {
        if (isWindows) return openWindows()
        return openUnix(onDebugInfo)
    }

    private fun openWindows(): DiscordIPCPipe {
        for (i in 0..9) runCatching { return DiscordIPCPipe.Windows("\\\\.\\pipe\\discord-ipc-$i") }
        throw DiscordIPCException("No Discord IPC pipe found on Windows. Is Discord running?")
    }

    private fun openUnix(onDebugInfo: (Map<String, String>) -> Unit): DiscordIPCPipe {
        val uid = resolveUid()
        val procEnviron = readProcEnviron()
        fun env(key: String) = System.getenv(key) ?: procEnviron[key]

        val baseDirs = listOfNotNull(
            env("XDG_RUNTIME_DIR"),
            env("TMPDIR"),
            env("TMP"),
            uid?.let { "/run/user/$it" },
            "/tmp",
        ).distinct()

        val subDirs = listOf(
            "",
            "app/com.discordapp.Discord",
            "app/com.discordapp.DiscordCanary",
            "snap.discord",
            "snap.discord-canary",
            "snap.discord-ptb",
        )

        val flatpakDirs = uid?.let {
            runCatching {
                Files.list(Path("/run/user/$it/.flatpak")).map { app -> "$app/xdg-run" }.toList()
            }.getOrDefault(emptyList())
        }.orEmpty()

        val allDirs = baseDirs.flatMap { base ->
            subDirs.map { if (it.isEmpty()) base else "$base/$it" }
        } + flatpakDirs

        val errors = mutableMapOf<String, String>()
        for (dir in allDirs) {
            for (i in 0..9) {
                val path = Path("$dir/discord-ipc-$i")
                runCatching { return DiscordIPCPipe.Unix(path) }
                    .onFailure { errors["$path"] = it.message ?: "unknown" }
            }
        }

        val diagnosis = diagnoseUnix(uid, baseDirs)
        onDebugInfo(buildDebugInfo(uid, baseDirs, flatpakDirs, procEnviron, errors, diagnosis))

        throw DiscordIPCException(
            diagnosis?.message ?: "No Discord IPC socket found on Unix. Is Discord running? Last error: ${errors.values.lastOrNull()}",
            isSandboxIssue = diagnosis?.isSandboxIssue == true,
        )
    }

    /**
     * Attempts to identify why no Unix socket was found.
     *
     * Checks for a Flatpak sandbox via `/.flatpak-info` and whether any candidate
     * directories are readable by the JVM process.
     */
    private fun diagnoseUnix(uid: String?, baseDirs: List<String>): Diagnosis? {
        val flatpakInfo = readFlatpakInfo()
        if (flatpakInfo != null) {
            val appName = flatpakInfo["name"] ?: "your-launcher"
            val runtimePath = uid?.let { "/run/user/$it" } ?: "/run/user/\$UID"
            return Diagnosis(
                message = "Discord RPC is blocked by the Flatpak sandbox ($appName). " +
                    "Fix: flatpak override --user $appName --filesystem=$runtimePath",
                isSandboxIssue = true,
            )
        }
        if (baseDirs.none { java.io.File(it).canRead() }) return Diagnosis(
            message = "No candidate runtime directories are accessible from this process. Is Discord running?",
            isSandboxIssue = false,
        )
        return null
    }

    private data class Diagnosis(val message: String, val isSandboxIssue: Boolean)

    /**
     * Reads `/.flatpak-info` if present, returning its key=value pairs.
     * Returns null when not running inside a Flatpak sandbox.
     */
    private fun readFlatpakInfo(): Map<String, String>? {
        val file = java.io.File("/.flatpak-info")
        if (!file.exists()) return null
        return file.readLines()
            .filter { '=' in it && !it.startsWith('[') }
            .associateNotNull { line ->
                line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() }
            }
    }

    private fun resolveUid(): String? =
        System.getenv("XDG_RUNTIME_DIR")?.substringAfterLast('/')?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            ?: runCatching { java.io.File("/proc/self/loginuid").readText().trim() }.getOrNull()
            ?: runCatching {
                java.io.File("/proc/self/status").useLines { lines ->
                    lines.firstOrNull { it.startsWith("Uid:") }?.split("\t")?.getOrNull(1)
                }
            }.getOrNull()

    private fun readProcEnviron(): Map<String, String> = runCatching {
        java.io.File("/proc/self/environ").readBytes()
            .toString(Charsets.UTF_8)
            .split('\u0000')
            .associateNotNull { entry ->
                entry.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
            }
    }.getOrDefault(emptyMap())

    private fun buildDebugInfo(
        uid: String?,
        baseDirs: List<String>,
        flatpakDirs: List<String>,
        procEnviron: Map<String, String>,
        errors: Map<String, String>,
        diagnosis: Diagnosis?,
    ) = mapOf(
        "uid" to (uid ?: "null"),
        "baseDirs" to baseDirs.joinToString("|"),
        "flatpakDirs" to flatpakDirs.joinToString("|"),
        "xdgFromEnv" to (System.getenv("XDG_RUNTIME_DIR") ?: "null"),
        "xdgFromProc" to (procEnviron["XDG_RUNTIME_DIR"] ?: "null"),
        "diagnosis" to (diagnosis?.message ?: "none"),
        "errors" to errors.entries
            .groupBy { it.value }
            .map { (error, entries) -> "${entries.first().key} (+${entries.size - 1} more): $error" }
            .ifEmpty { listOf("none") }
            .joinToString("|"),
    )
}
