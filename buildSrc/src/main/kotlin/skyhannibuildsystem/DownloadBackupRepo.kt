package skyhannibuildsystem

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI

// Code taken from NotEnoughUpdates
abstract class DownloadBackupRepo : DefaultTask() {

    @get:Input
    abstract var user: String

    @get:Input
    abstract var repo: String

    @get:Input
    abstract var branch: String

    @get:Input
    abstract var resourcePath: String

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    val repoFile get() = outputDirectory.get().asFile.resolve(resourcePath)

    @TaskAction
    fun downloadRepo() {
        val downloadUrl = URI.create("https://github.com/$user/$repo/archive/refs/heads/$branch.zip").toURL()
        val file = repoFile
        file.parentFile.mkdirs()
        file.outputStream().use { out ->
            downloadUrl.openStream().use { inp ->
                inp.copyTo(out)
            }
        }
    }
}
