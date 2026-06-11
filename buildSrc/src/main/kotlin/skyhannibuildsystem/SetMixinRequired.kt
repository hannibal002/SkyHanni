package skyhannibuildsystem

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Mutates processResources output in-place before dev runs or archive packaging")
abstract class SetMixinRequired : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mixinConfigFile: RegularFileProperty

    @get:Input
    abstract val required: Property<Boolean>

    @TaskAction
    fun setRequired() {
        val file = mixinConfigFile.get().asFile
        val parsedConfig = file.reader().use { JsonParser.parseReader(it) }.asJsonObject
        val updatedConfig = JsonObject()

        for ((key, value) in parsedConfig.entrySet()) {
            if (key == "required") continue
            updatedConfig.add(key, value)
            if (required.get() && key == "package") {
                updatedConfig.addProperty("required", true)
            }
        }

        if (required.get() && !updatedConfig.has("required")) {
            updatedConfig.addProperty("required", true)
        }

        file.writeText(GSON.toJson(updatedConfig) + "\n")
    }

    companion object {
        private val GSON = GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()
    }
}
