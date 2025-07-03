package skyhannibuildsystem

import at.hannibal2.changelog.ModVersion
import at.skyhanni.sharedvariables.DependencyType
import at.skyhanni.sharedvariables.ModrinthDependency
import at.skyhanni.sharedvariables.ProjectTarget
import com.github.mizosoft.methanol.Methanol
import com.github.mizosoft.methanol.MultipartBodyPublisher
import com.github.mizosoft.methanol.MutableRequest
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.time.Duration

abstract class PublishToModrinth : DefaultTask() {

    @get:InputDirectory
    abstract val jarDirectory: DirectoryProperty

    @get:Input
    abstract var changelog: String

    @get:Input
    abstract var versionNumber: String

    @get:Input
    abstract var modrinthToken: String

    private val userAgent: String
        get() = "SkyHanni-$versionNumber"

    @TaskAction
    fun publishToModrinth() {
        val jars = jarDirectory.get().asFile.listFiles()?.filter { it.extension == "jar" }.orEmpty()

        for (jar in jars) {
            processJar(jar)
        }
    }

    private val jarNamePattern = "SkyHanni-(?<modVersion>[\\d.]+)-mc(?<mcVersion>[\\d.]+)\\.jar".toPattern()
    private val client by lazy { constructClient() }

    private fun processJar(file: File) {
        val fileName = file.name
        val match = jarNamePattern.matcher(fileName)
        if (!match.matches()) {
            throw IllegalArgumentException("Jar file name '$fileName' does not match the expected pattern.")
        }

        val modVersion = match.group("modVersion")
        val mcVersion = match.group("mcVersion")

        if (modVersion != versionNumber) {
            throw IllegalArgumentException("Mod version '$modVersion' does not match the expected version '$versionNumber'.")
        }

        val projectTarget = ProjectTarget.findByMcVersion(mcVersion)
            ?: throw IllegalArgumentException("No ProjectTarget found for Minecraft version '$mcVersion'.")

        val modrinthInfo = projectTarget.modrinthInfo
            ?: throw IllegalArgumentException("No ModrinthInfo found for ProjectTarget '$projectTarget'.")

        val modVersionObj = ModVersion.fromString(modVersion)

        val versionName = "$modVersion for $mcVersion"
        val versionNumber = modVersionObj.asString
        val dependencies = modrinthInfo.dependencies.createDependencyArray()
        val gameVersions = modrinthInfo.minecraftVersions.toVersionArray()
        val loader = modrinthInfo.loader
        val versionType = if (modVersionObj.isBeta) "beta" else "release"
        val loaders = loader.toLoadersArray()
        val featured = ProjectTarget.values().last() == projectTarget
        val status = "listed"
        val requestedStatus = "listed"

        val fileParts = JsonArray()
        fileParts.add(fileName)

        val modrinthJson = JsonObject()
        modrinthJson.addProperty("name", versionName)
        modrinthJson.addProperty("version_number", versionNumber)
        modrinthJson.addProperty("changelog", changelog)
        modrinthJson.add("dependencies", dependencies)
        modrinthJson.add("game_versions", gameVersions)
        modrinthJson.addProperty("version_type", versionType)
        modrinthJson.add("loaders", loaders)
        modrinthJson.addProperty("featured", featured)
        modrinthJson.addProperty("status", status)
        modrinthJson.addProperty("requested_status", requestedStatus)
        modrinthJson.addProperty("project_id", ModrinthDependency.SKYHANNI.projectId)
        modrinthJson.add("file_parts", fileParts)
        modrinthJson.addProperty("primary_file", fileName)

        val modrinthBody = MultipartBodyPublisher.newBuilder()
            .textPart("data", modrinthJson.toString())
            .filePart(fileName, file.toPath())
            .build()

        val modrinthRequest = MutableRequest.POST("https://api.modrinth.com/v2/version", modrinthBody)
            .timeout(Duration.ofSeconds(30L))
            .header("Authorization", modrinthToken)
            .header("User-Agent", userAgent)

        val modrinthRespnse = client.send(modrinthRequest, HttpResponse.BodyHandlers.ofString())
        val responseCode = modrinthRespnse.statusCode()
        if (responseCode !in 200..201) {
            throw RuntimeException("Failed to publish to Modrinth: HTTP $responseCode - ${modrinthRespnse.body()}")
        }

        val responseBody = modrinthRespnse.body()
        println("Successfully published to Modrinth: $responseBody")
    }

    private fun Map<ModrinthDependency, DependencyType>.createDependencyArray(): JsonArray {
        val array = JsonArray()

        for ((dependency, type) in this) {
            val jsonObj = JsonObject()
            jsonObj.addProperty("project_id", dependency.projectId)
            jsonObj.addProperty("dependency_type", type.apiName)
            array.add(jsonObj)
        }

        return array
    }

    private fun List<String>.toVersionArray(): JsonArray {
        val array = JsonArray()
        for (version in this) {
            array.add(version)
        }
        return array
    }

    private fun String.toLoadersArray(): JsonArray {
        val array = JsonArray()
        array.add(this)
        return array
    }

    private fun constructClient(): Methanol {
        val timeOut = Duration.ofSeconds(30L)
        return Methanol.newBuilder()
            .connectTimeout(timeOut)
            .requestTimeout(timeOut)
            .readTimeout(timeOut)
            .headersTimeout(timeOut)
            .userAgent(userAgent)
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NEVER)
            .executor(Runnable::run)
            .build()
    }
}
