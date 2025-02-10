package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ReflectionUtils.makeAccessible
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer
import kotlin.math.abs

@SkyHanniModule
object RenderCache {

    // -- FBO and caching variables --
    private var frameBufferId: Int = 0
    private var textureId: Int = 0
    private var fboWidth: Int = Display.getWidth()
    private var fboHeight: Int = Display.getHeight()
    private val width get() = Display.getWidth()
    private val height get() = Display.getHeight()
    private val useCache get() = SkyHanniMod.feature.misc.cacheRender
    private var updateCache = false

    init {
        // Initial FBO creation.
        val (fbId, texId) = createFBO(fboWidth, fboHeight)
        frameBufferId = fbId
        textureId = texId
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (Minecraft.getMinecraft().renderManager.fontRenderer == null) return
        if (useCache) {
            updateCache = true
        }
    }

    // -- FBO update --
    private fun updateFBOIfNeeded() {
        val newWidth = Display.getWidth()
        val newHeight = Display.getHeight()
        if (newWidth != fboWidth || newHeight != fboHeight) {
            // Free old resources:
            GL30.glDeleteFramebuffers(frameBufferId)
            GL11.glDeleteTextures(textureId)

            val (fbId, texId) = createFBO(newWidth, newHeight)
            frameBufferId = fbId
            textureId = texId
            fboWidth = newWidth
            fboHeight = newHeight
        }
    }

    private fun createFBO(width: Int, height: Int): Pair<Int, Int> {
        val fboID = GL30.glGenFramebuffers()
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID)

        val texID = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?,
        )
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
            GL11.GL_TEXTURE_2D, texID, 0,
        )

        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw RuntimeException("Framebuffer not complete")

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
        return fboID to texID
    }

    private fun renderToFBO(fboID: Int, width: Int, height: Int) {
        GlStateManager.pushMatrix()
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID)

        // Save current viewport.
        val viewportBuffer = BufferUtils.createIntBuffer(16)
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer)
        viewportBuffer.rewind()
        val viewport = IntArray(4) { viewportBuffer.get() }

        GL11.glViewport(0, 0, width, height)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

        // Set up orthographic projection.
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glOrtho(0.0, width.toDouble(), height.toDouble(), 0.0, 0.0, 1.0)
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()

        heavyRenderCall()

        // Restore matrices.
        GL11.glPopMatrix() // MODELVIEW
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)

        // Unbind the FBO.
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
        // Restore the previous viewport.
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
        GL11.glFlush() // Ensure rendering completes

        GlStateManager.popMatrix()
    }

    private fun heavyRenderCall() {
        GlStateManager.pushMatrix()
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GuiRenderEvent.GuiOverlayRenderEvent().post()
        GL11.glPopAttrib()
        GlStateManager.popMatrix()
    }

    // -- Frame guard using renderPartialTicks --
    private var lastPartialTicks = -1f
    private val timerField by lazy {
        Minecraft::class.java.getDeclaredField("timer").makeAccessible()
    }
    private val renderPartialTicksField by lazy {
        val timer = timerField.get(Minecraft.getMinecraft())
        timer.javaClass.getDeclaredField("renderPartialTicks").makeAccessible()
    }

    private fun getRenderPartialTicks(): Float {
        val timer = timerField.get(Minecraft.getMinecraft())
        return renderPartialTicksField.getFloat(timer)
    }

    // -- Debug GL state logging (active for 1 second after sneaking) --
    private const val DEBUG_GL_LOG_DURATION = 1000L
    private var debugGLLoggingStartTime: Long = -1L

    private fun startGLLogging() {
        debugGLLoggingStartTime = System.currentTimeMillis()
    }

    private fun logGLStateIfNeeded() {
        if (debugGLLoggingStartTime < 0L) return
        val currentTime = System.currentTimeMillis()
        if (currentTime - debugGLLoggingStartTime <= DEBUG_GL_LOG_DURATION) {
            logGLState()
        } else {
            debugGLLoggingStartTime = -1L
        }
    }

    private fun logGLState() {
        val viewportBuffer = BufferUtils.createIntBuffer(16)
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer)
        viewportBuffer.rewind()
        val viewport = IntArray(4) { viewportBuffer.get() }
        println("GL State - Viewport: ${viewport.joinToString(", ")}")
        println("GL State - Depth Test Enabled: ${GL11.glIsEnabled(GL11.GL_DEPTH_TEST)}")
        println("GL State - Blending Enabled: ${GL11.glIsEnabled(GL11.GL_BLEND)}")
        println("GL State - Matrix Mode: ${GL11.glGetInteger(GL11.GL_MATRIX_MODE)}")
    }

    fun onFrame() {
        if (!useCache) {
            heavyRenderCall()
            return
        }

        // Trigger debug logging for one second when the player is sneaking.
        val player = Minecraft.getMinecraft().thePlayer
        if (player != null && player.isSneaking && debugGLLoggingStartTime < 0L) {
            startGLLogging()
        }

        // Frame guard using renderPartialTicks.
        val currentPartialTicks = getRenderPartialTicks()
        if (abs(currentPartialTicks - lastPartialTicks) < 0.001f) return
        lastPartialTicks = currentPartialTicks

        // Update the FBO if display size changed.
        updateFBOIfNeeded()


        if (updateCache) {
            renderToFBO(frameBufferId, width, height)
            updateCache = false
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        drawCachedTexture(textureId, 0f, 0f, width.toFloat(), height.toFloat())
        GL11.glPopAttrib()

        // Log GL state if within the logging period.
        logGLStateIfNeeded()
    }

    private fun drawCachedTexture(texID: Int, x: Float, y: Float, w: Float, h: Float) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glTexCoord2f(0f, 0f)
        GL11.glVertex2f(x, y)
        GL11.glTexCoord2f(1f, 0f)
        GL11.glVertex2f(x + w, y)
        GL11.glTexCoord2f(1f, 1f)
        GL11.glVertex2f(x + w, y + h)
        GL11.glTexCoord2f(0f, 1f)
        GL11.glVertex2f(x, y + h)
        GL11.glEnd()
    }
}
