package at.hannibal2.skyhanni.compat

import com.mojang.blaze3d.pipeline.RenderPipeline
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import java.lang.reflect.Method

object IrisCompat {

    private val isIrisLoaded by lazy { PlatformUtils.isModInstalled("iris") }

    private var IRIS_INSTANCE: Any? = null
    private var IRIS_ASSIGN_PIPELINE_METHOD: Method? = null
    private var IRIS_PROGRAM_BASIC: Enum<*>? = null
    private var IRIS_PROGRAM_LINES: Enum<*>? = null
    private var IRIS_PROGRAMS_TEXTURED: Enum<*>? = null

    init {
        initialize()
    }

    private fun initialize() {
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
            ErrorManager.logErrorWithData(exception, "Failed to initialize iris compat!")
        }
    }

    fun assignPipelines() {
        assignPipeline(SkyHanniRenderPipeline.LINES(), lines())
        assignPipeline(SkyHanniRenderPipeline.LINES_XRAY(), lines())
        assignPipeline(SkyHanniRenderPipeline.FILLED(), basic())
        assignPipeline(SkyHanniRenderPipeline.FILLED_XRAY(), basic())
        assignPipeline(SkyHanniRenderPipeline.TRIANGLES(), basic())
        assignPipeline(SkyHanniRenderPipeline.TRIANGLES_XRAY(), basic())
        assignPipeline(SkyHanniRenderPipeline.TRIANGLE_FAN(), basic())
        assignPipeline(SkyHanniRenderPipeline.TRIANGLE_FAN_XRAY(), basic())
        assignPipeline(SkyHanniRenderPipeline.QUADS(), basic())
        assignPipeline(SkyHanniRenderPipeline.QUADS_XRAY(), basic())
        assignPipeline(SkyHanniRenderPipeline.ROUNDED_RECT(), basic())
        assignPipeline(SkyHanniRenderPipeline.ROUNDED_TEXTURED_RECT(), textured())
        assignPipeline(SkyHanniRenderPipeline.ROUNDED_RECT_OUTLINE(), basic())
        assignPipeline(SkyHanniRenderPipeline.CIRCLE(), basic())
        assignPipeline(SkyHanniRenderPipeline.RADIAL_GRADIENT_CIRCLE(), basic())
        assignPipeline(SkyHanniRenderPipeline.CHROMA_STANDARD(), basic())
        assignPipeline(SkyHanniRenderPipeline.CHROMA_TEXT(), textured())
    }

    fun assignPipeline(pipeline: RenderPipeline, enumValue: Any?) {
        if (!isIrisLoaded) return
        IRIS_ASSIGN_PIPELINE_METHOD?.let { method ->
            try {
                method.invoke(IRIS_INSTANCE, pipeline, enumValue)
            } catch (exception: Exception) {
                ErrorManager.logErrorWithData(exception, "Failed to assign Iris pipeline!")
            }
        }
    }

    fun basic(): Enum<*>? = IRIS_PROGRAM_BASIC

    fun lines(): Enum<*>? = IRIS_PROGRAM_LINES

    fun textured(): Enum<*>? = IRIS_PROGRAMS_TEXTURED
}
