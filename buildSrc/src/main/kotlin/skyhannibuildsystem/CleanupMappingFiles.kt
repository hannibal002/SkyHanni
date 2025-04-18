package skyhannibuildsystem

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CleanupMappingFiles : DefaultTask() {

    @get:OutputDirectory
    abstract val mappingsDirectory: DirectoryProperty

    @TaskAction
    fun cleanupMappingFiles() {
        val mappingsDir = mappingsDirectory.get().asFile
        println("full path for mapping task: ${mappingsDir.absolutePath}")

        val mappingsFiles = mappingsDir.listFiles() ?: return

        for (file in mappingsFiles) {
            if (file.isFile && file.extension == "txt" && file.name.contains("mapping")) {
                processMappingFile(file)
            }
        }
    }

    private fun processMappingFile(file: File) {
        val isPatternMapping = file.name.startsWith("pattern-")

        val lines = mutableSetOf<Pair<String, String>>()

        file.forEachLine { line ->
            if (line.isNotBlank()) {
                findSection(line, isPatternMapping)?.let { parts ->
                    lines.add(Pair(parts, line))
                }
            }
        }

        val sortedLines: Map<String, List<String>> = lines.groupBy { it.first }
            .mapValues { it.value.map { it.second }.sorted() }
            .toSortedMap()

        writeMappings(file, sortedLines)
    }

    private fun findSection(line: String, isPatternMapping: Boolean): String? {
        val parts = line.split(" ")
        when (parts.size) {
            2 -> {
                if (isPatternMapping) return null
                val path = parts.first().split(".").dropLast(1).joinToString(".")
                return path
            }

            3 -> {
                if (isPatternMapping) return null
                return parts[0]
            }

            5 -> {
                if (!isPatternMapping) return null
                return parts[0]
            }

            else -> return null
        }
    }

    private fun writeMappings(file: File, sortedLines: Map<String, List<String>>) {
        file.delete()
        file.writeText("\n")

        for ((_, value) in sortedLines) {
            for (line in value) {
                file.appendText("$line\n")
            }
            file.appendText("\n")
        }
    }
}
