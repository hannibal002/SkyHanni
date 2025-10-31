package at.hannibal2.hanni.compat

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.utils.InitFinishedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import com.mojang.blaze3d.pipeline.RenderPipeline
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.render.HanniRenderPipeline
import at.hannibal2.hanni.utils.system.PlatformUtils
import java.lang.reflect.Method

@HanniModule
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
            val irisInstanceClass = IRIS_INSTANCE!!.javaClass

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
        assignPipeline(HanniRenderPipeline.LINES(), IRIS_PROGRAM_LINES)
        assignPipeline(HanniRenderPipeline.LINES_XRAY(), IRIS_PROGRAM_LINES)
        assignPipeline(HanniRenderPipeline.FILLED(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.FILLED_XRAY(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.TRIANGLES(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.TRIANGLES_XRAY(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.TRIANGLE_FAN(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.TRIANGLE_FAN_XRAY(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.QUADS(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.QUADS_XRAY(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.ROUNDED_RECT(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.ROUNDED_TEXTURED_RECT(), IRIS_PROGRAMS_TEXTURED)
        assignPipeline(HanniRenderPipeline.ROUNDED_RECT_OUTLINE(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.CIRCLE(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.RADIAL_GRADIENT_CIRCLE(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.CHROMA_STANDARD(), IRIS_PROGRAM_BASIC)
        assignPipeline(HanniRenderPipeline.CHROMA_TEXT(), IRIS_PROGRAMS_TEXTURED)
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
