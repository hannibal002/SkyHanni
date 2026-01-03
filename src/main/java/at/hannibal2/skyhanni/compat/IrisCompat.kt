package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.utils.InitFinishedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.mojang.blaze3d.pipeline.RenderPipeline
import java.lang.reflect.Method

@SkyHanniModule
object IrisCompat {

    private val isIrisLoaded by lazy { PlatformUtils.isModInstalled("iris") }

    private var IRIS_INSTANCE: Any? = null
    private var IRIS_ASSIGN_PIPELINE_METHOD: Method? = null
    private var IRIS_PROGRAM_BASIC: Any? = null
    private var IRIS_PROGRAM_LINES: Any? = null
    private var IRIS_PROGRAMS_TEXTURED: Any? = null

    @HandleEvent
    fun onInitFinished(event: InitFinishedEvent) {
        if (!isIrisLoaded) return
        try {
            val irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi")
            IRIS_INSTANCE = irisApiClass.getMethod("getInstance").invoke(null)
            val irisInstanceClass = IRIS_INSTANCE?.javaClass ?: return

            val irisProgramEnum = Class.forName("net.irisshaders.iris.api.v0.IrisProgram")
            IRIS_PROGRAM_BASIC = java.lang.Enum.valueOf(irisProgramEnum.asSubclass(Enum::class.java), "BASIC")
            IRIS_PROGRAM_LINES = java.lang.Enum.valueOf(irisProgramEnum.asSubclass(Enum::class.java), "LINES")
            IRIS_PROGRAMS_TEXTURED = java.lang.Enum.valueOf(irisProgramEnum.asSubclass(Enum::class.java), "TEXTURED")

            IRIS_ASSIGN_PIPELINE_METHOD = irisInstanceClass.getMethod("assignPipeline", RenderPipeline::class.java, irisProgramEnum)
        } catch (exception: Exception) {
            ErrorManager.logErrorWithData(exception, "Failed to initialize Iris compat!")
        }
        assignPipelines()
    }

    private fun assignPipelines() {
        assignPipeline(SkyHanniRenderPipeline.LINES(), IRIS_PROGRAM_LINES)
        assignPipeline(SkyHanniRenderPipeline.LINES_XRAY(), IRIS_PROGRAM_LINES)
        assignPipeline(SkyHanniRenderPipeline.FILLED(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.FILLED_XRAY(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.TRIANGLES(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.TRIANGLES_XRAY(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.TRIANGLE_FAN(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.TRIANGLE_FAN_XRAY(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.QUADS(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.QUADS_XRAY(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.ROUNDED_RECT(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.ROUNDED_TEXTURED_RECT(), IRIS_PROGRAMS_TEXTURED)
        assignPipeline(SkyHanniRenderPipeline.ROUNDED_RECT_OUTLINE(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.CIRCLE(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.RADIAL_GRADIENT_CIRCLE(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.CHROMA_STANDARD(), IRIS_PROGRAM_BASIC)
        assignPipeline(SkyHanniRenderPipeline.CHROMA_TEXT(), IRIS_PROGRAMS_TEXTURED)
    }

    private fun assignPipeline(pipeline: RenderPipeline, enumValue: Any?) {
        enumValue ?: return
        if (!isIrisLoaded) return
        IRIS_ASSIGN_PIPELINE_METHOD?.let { method ->
            try {
                method.invoke(IRIS_INSTANCE, pipeline, enumValue)
            } catch (exception: Exception) {
                ErrorManager.logErrorWithData(exception, "Failed to assign Iris pipeline!")
            }
        }
    }
}
