package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

class RepoManager(private val configLocation: File) {

    private val gson get() = ConfigManager.gson
    private var latestRepoCommit: String? = null
    private val repoLocation: File = File(configLocation, "repo")
    private var error = false
    private var lastRepoUpdate = SimpleTimeMark.farPast()

    companion object {

        val successfulConstants = mutableListOf<String>()
        val unsuccessfulConstants = mutableListOf<String>()

        private var lastConstant: String? = null

        fun setlastConstant(constant: String) {
            lastConstant?.let {
                successfulConstants.add(it)
            }
            lastConstant = constant
        }
    }

    fun loadRepoInformation() {
        atomicShouldManuallyReload.set(true)
        if (SkyHanniMod.feature.dev.repoAutoUpdate) {
            fetchRepository(false).thenRun(this::reloadRepository)
        } else {
            reloadRepository()
        }
    }

    private val atomicShouldManuallyReload = AtomicBoolean(false)// TODO remove the workaround

    fun updateRepo() {
        atomicShouldManuallyReload.set(true)
        fetchRepository(true).thenRun { this.reloadRepository("Repo updated successful.") }
    }

    fun reloadLocalRepo() {
        atomicShouldManuallyReload.set(true)
        reloadRepository("Repo loaded from local files successful.")
    }

    private fun fetchRepository(command: Boolean): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val currentCommitJSON: JsonObject? = getJsonFromFile(File(configLocation, "currentCommit.json"))
                latestRepoCommit = null
                try {
                    InputStreamReader(URL(getCommitApiUrl()).openStream())
                        .use { inReader ->
                            val commits: JsonObject = gson.fromJson(inReader, JsonObject::class.java)
                            latestRepoCommit = commits["sha"].asString
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (latestRepoCommit == null || latestRepoCommit!!.isEmpty()) return@supplyAsync false
                val file = File(configLocation, "repo")
                if (file.exists() && currentCommitJSON != null && currentCommitJSON["sha"].asString == latestRepoCommit
                ) {
                    if (unsuccessfulConstants.isEmpty() && lastRepoUpdate.passedSince() < 1.minutes) {
                        if (command) {
                            ChatUtils.chat("§7The repo is already up to date!")
                            atomicShouldManuallyReload.set(false)
                        }
                        return@supplyAsync false
                    }
                }
                lastRepoUpdate = SimpleTimeMark.now()
                RepoUtils.recursiveDelete(repoLocation)
                repoLocation.mkdirs()
                val itemsZip = File(repoLocation, "sh-repo-main.zip")
                try {
                    itemsZip.createNewFile()
                } catch (e: IOException) {
                    return@supplyAsync false
                }
                val url = URL(getDownloadUrl(latestRepoCommit))
                val urlConnection = url.openConnection()
                urlConnection.connectTimeout = 15000
                urlConnection.readTimeout = 30000
                try {
                    urlConnection.getInputStream().use { `is` ->
                        FileUtils.copyInputStreamToFile(
                            `is`,
                            itemsZip
                        )
                    }
                } catch (e: IOException) {
                    Exception(
                        "Failed to download SkyHanni Repo! Please report this issue on the discord!",
                        e
                    ).printStackTrace()
                    if (command) {
                        ChatUtils.error("An error occurred while trying to reload the repo! See logs for more info.")
                    }
                    return@supplyAsync false
                }
                RepoUtils.unzipIgnoreFirstFolder(
                    itemsZip.absolutePath,
                    repoLocation.absolutePath
                )
                if (currentCommitJSON == null || currentCommitJSON["sha"].asString != latestRepoCommit) {
                    val newCurrentCommitJSON = JsonObject()
                    newCurrentCommitJSON.addProperty("sha", latestRepoCommit)
                    try {
                        writeJson(newCurrentCommitJSON, File(configLocation, "currentCommit.json"))
                    } catch (ignored: IOException) {
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true
        }
    }

    private fun reloadRepository(answerMessage: String = ""): CompletableFuture<Void?> {
        val comp = CompletableFuture<Void?>()
        if (!atomicShouldManuallyReload.get()) return comp
        ErrorManager.resetCache()
        Minecraft.getMinecraft().addScheduledTask {
            error = false
            successfulConstants.clear()
            unsuccessfulConstants.clear()
            lastConstant = null

            RepositoryReloadEvent(repoLocation, gson).postAndCatchAndBlock(ignoreErrorCache = true) {
                error = true
                lastConstant?.let {
                    unsuccessfulConstants.add(it)
                }
                lastConstant = null
            }
            comp.complete(null)
            if (answerMessage.isNotEmpty() && !error) {
                ChatUtils.chat("§a$answerMessage")
            }
            if (error) {
                ChatUtils.clickableChat(
                    "Error with the repo detected, try /shupdaterepo to fix it!",
                    "shupdaterepo",
                    prefixColor = "§c"
                )
                if (unsuccessfulConstants.isEmpty()) {
                    unsuccessfulConstants.add("All Constants")
                }
            }
        }
        return comp
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Repo Status")

        if (unsuccessfulConstants.isEmpty() && successfulConstants.isNotEmpty()) {
            event.addIrrelevant("Repo working fine")
            return
        }

        event.addData {
            add("Successful Constants (${successfulConstants.size}):")

            add("Unsuccessful Constants (${unsuccessfulConstants.size}):")

            for ((i, constant) in unsuccessfulConstants.withIndex()) {
                add("   - $constant")
                if (i == 5) {
                    add("...")
                    break
                }
            }
        }
    }

    fun displayRepoStatus(joinEvent: Boolean) {
        if (joinEvent) {
            if (unsuccessfulConstants.isNotEmpty()) {
                ChatUtils.error(
                    "§7Repo Issue! Some features may not work. Please report this error on the Discord!\n"
                        + "§7Repo Auto Update Value: §c${SkyHanniMod.feature.dev.repoAutoUpdate}\n"
                        + "§7If you have Repo Auto Update turned off, please try turning that on.\n"
                        + "§cUnsuccessful Constants §7(${unsuccessfulConstants.size}):"
                )
                for (constant in unsuccessfulConstants) {
                    ChatUtils.chat("   §e- §7$constant")
                }
            }
            return
        }
        if (unsuccessfulConstants.isEmpty() && successfulConstants.isNotEmpty()) {
            ChatUtils.chat("Repo working fine! Commit hash: $latestRepoCommit", prefixColor = "§a")
            return
        }
        ChatUtils.chat("Repo has errors! Commit has: ${latestRepoCommit ?: "null"}", prefixColor = "§c")
//         if (successfulConstants.isNotEmpty()) ChatUtils.chat(
        if (successfulConstants.isNotEmpty()) LorenzUtils.chat(
            "Successful Constants §7(${successfulConstants.size}):",
            prefixColor = "§a"
        )
        for (constant in successfulConstants) {
            ChatUtils.chat("   §a- §7$constant", false)
        }
        ChatUtils.chat("Unsuccessful Constants §7(${unsuccessfulConstants.size}):")
        for (constant in unsuccessfulConstants) {
            ChatUtils.chat("   §e- §7$constant", false)
        }
    }

    /**
     * Parses a file in to a JsonObject.
     */
    private fun getJsonFromFile(file: File?): JsonObject? {
        try {
            BufferedReader(
                InputStreamReader(
                    FileInputStream(file),
                    StandardCharsets.UTF_8
                )
            ).use { reader ->
                return gson.fromJson(reader, JsonObject::class.java)
            }
        } catch (e: java.lang.Exception) {
            return null
        }
    }

    private fun getCommitApiUrl(): String {
        val repoUser = "hannibal002"
        val repoName = "SkyHanni-REPO"
        val repoBranch = "main"
        return String.format("https://api.github.com/repos/%s/%s/commits/%s", repoUser, repoName, repoBranch)
    }

    private fun getDownloadUrl(commitId: String?): String {
        val repoUser = "hannibal002"
        val repoName = "SkyHanni-REPO"
        return String.format("https://github.com/%s/%s/archive/%s.zip", repoUser, repoName, commitId)
    }

    @Throws(IOException::class)
    fun writeJson(json: JsonObject?, file: File) {
        file.createNewFile()
        BufferedWriter(
            OutputStreamWriter(
                FileOutputStream(file),
                StandardCharsets.UTF_8
            )
        ).use { writer -> writer.write(gson.toJson(json)) }
    }

    @SubscribeEvent
    fun onNeuRepoReload(event: io.github.moulberry.notenoughupdates.events.RepositoryReloadEvent) {
        NeuRepositoryReloadEvent().postAndCatch()
    }
}
