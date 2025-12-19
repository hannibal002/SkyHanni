package at.skyhanni.sharedvariables

import java.io.File
import java.util.Properties

enum class MultiVersionStage(val label: String) {
    OFF("off"),
    FULL("compile")
    ;

    fun shouldCompile(projectTarget: ProjectTarget): Boolean {
        return true
    }

    fun shouldCreateProject(projectTarget: ProjectTarget): Boolean {
        return true
    }

    companion object {
        lateinit var activeState: MultiVersionStage
        fun initFrom(file: File) {
            val prop = Properties()
            if (file.exists()) {
                file.inputStream().use(prop::load)
            }
            val multiVersion = prop["skyhanni.multi-version"]
            activeState = MultiVersionStage.values().find { it.label == multiVersion } ?: OFF
            println("SkyHanni multi version stage loaded: $activeState")
        }
    }
}
