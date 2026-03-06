package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.utils.InitFinishedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.mojang.blaze3d.pipeline.RenderPipeline

@SkyHanniModule
object IrisCompat {

    private const val IRIS_BASE_PATH = "net.irisshaders.iris.api.v0"
    private const val IRIS_PROGRAM_PATH = "$IRIS_BASE_PATH.IrisProgram"
    private const val IRIS_API_PATH = "$IRIS_BASE_PATH.IrisApi"

    private val isIrisLoaded by lazy { PlatformUtils.isModInstalled("iris") }
    private val irisProgramEnum by lazy { Class.forName(IRIS_PROGRAM_PATH) }

    enum class IrisProgram {
        BASIC,
        LINES,
        TEXTURED,
        ;

        fun asJavaEnum(): Any = java.lang.Enum.valueOf(
            irisProgramEnum.asSubclass(Enum::class.java),
            this.name
        )
    }

    @HandleEvent
    fun onInitFinished(event: InitFinishedEvent) {
        if (!isIrisLoaded) return
        try {
            val irisApiClass = Class.forName(IRIS_API_PATH)
            val irisInstance = irisApiClass.getMethod("getInstance").invoke(null)
            val irisInstanceClass = irisInstance?.javaClass ?: return
            val pipelineMethod = runCatching {
                irisInstanceClass.getMethod(
                    "assignPipeline",
                    RenderPipeline::class.java,
                    irisProgramEnum
                )
            }.getOrNull() ?: return

            SkyHanniRenderPipeline.entries.forEach { shPipeline ->
                try {
                    pipelineMethod.invoke(irisInstance, shPipeline, shPipeline.irisProgram.asJavaEnum())
                } catch (exception: Exception) {
                    ErrorManager.logErrorWithData(exception, "Failed to assign Iris pipeline!")
                }
            }
        } catch (exception: Exception) {
            ErrorManager.logErrorWithData(exception, "Failed to initialize Iris compat!")
        }
    }
}
