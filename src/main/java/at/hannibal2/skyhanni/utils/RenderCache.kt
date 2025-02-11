package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer

@SkyHanniModule
object RenderCache {

    // ---------- FBO and Cache Variables ----------
    private var frameBufferId: Int = 0       // The offscreen framebuffer ID.
    private var textureId: Int = 0           // The texture attached to the FBO.
    private var fboWidth: Int = Display.getWidth()    // Initial FBO width.
    private var fboHeight: Int = Display.getHeight()  // Initial FBO height.
    private val width get() = Display.getWidth()       // Current display width.
    private val height get() = Display.getHeight()     // Current display height.

    // If caching is enabled, we use the cached overlay.
    private val useCache get() = SkyHanniMod.feature.misc.cacheRender

    // ---------- Tick Synchronization ----------
    // currentTick is incremented once per tick.
    private var currentTick: Long = 0
    // lastUpdateTick records the tick number when we last updated the FBO.
    private var lastUpdateTick: Long = -1

    // ---------- Initialization ----------
    init {
        // Create the initial FBO with current dimensions.
        createFBO(fboWidth, fboHeight).let { (fbId, texId) ->
            frameBufferId = fbId
            textureId = texId
            println("Init: Created FBO id=$frameBufferId, texture id=$textureId, dimensions=($fboWidth x $fboHeight)")
        }
    }

    // ---------- Tick Event Handler ----------
    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        // This event is fired once per game tick.
        currentTick++  // Increment our tick counter.
        // Only update if the fontRenderer is available.
        if (Minecraft.getMinecraft().renderManager.fontRenderer == null) return
        if (useCache) {
            println("onTick: currentTick set to $currentTick")
        }
    }

    // ---------- Frame Rendering ----------
    fun onFrame() {
        // If caching is disabled, perform the heavy render every frame.
        if (!useCache) {
            heavyRenderCall()
            return
        }

        // Check if the display size has changed.
        updateFBOIfNeeded()

        // If a new tick is detected (currentTick != lastUpdateTick),
        // update the FBO (i.e. re-render the overlay) once per tick.
        if (currentTick != lastUpdateTick) {
            println("onFrame: New tick detected (currentTick=$currentTick, lastUpdateTick=$lastUpdateTick). Rendering overlay to FBO.")
            renderToFBO(frameBufferId, width, height)
            lastUpdateTick = currentTick
        }

        // Always draw the cached overlay texture.
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Save current GL state.
        GL11.glDisable(GL11.GL_DEPTH_TEST)          // Disable depth testing for overlay.
        drawCachedTexture(textureId, 0f, 0f, width.toFloat(), height.toFloat())
        GL11.glPopAttrib()                          // Restore GL state.
    }

    // ---------- FBO Update on Display Resize ----------
    private fun updateFBOIfNeeded() {
        val newWidth = Display.getWidth()
        val newHeight = Display.getHeight()
        if (newWidth != fboWidth || newHeight != fboHeight) {
            // Delete the old FBO and texture.
            GL30.glDeleteFramebuffers(frameBufferId)
            GL11.glDeleteTextures(textureId)
            println("updateFBOIfNeeded: Display size changed from ($fboWidth x $fboHeight) to ($newWidth x $newHeight)")

            // Create a new FBO with updated dimensions.
            createFBO(newWidth, newHeight).let { (fbId, texId) ->
                frameBufferId = fbId
                textureId = texId
                fboWidth = newWidth
                fboHeight = newHeight
                println("updateFBOIfNeeded: New FBO id=$frameBufferId, texture id=$textureId")
            }
        }
    }

    // ---------- FBO Creation ----------
    private fun createFBO(width: Int, height: Int): Pair<Int, Int> {
        // Generate a new framebuffer.
        val fboID = GL30.glGenFramebuffers()
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID)

        // Generate a texture to attach to the FBO.
        val texID = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
        // Allocate texture storage with RGBA8 format.
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?
        )
        // Set texture filtering for scaling.
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

        // Attach the texture to the FBO as its color buffer.
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
            GL11.GL_TEXTURE_2D, texID, 0
        )

        // Check that the FBO is complete.
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw RuntimeException("createFBO: Framebuffer not complete")
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0) // Unbind the FBO.
        println("createFBO: FBO id=$fboID with texture id=$texID created for dimensions ($width x $height)")
        return fboID to texID
    }

    // ---------- Render to FBO ----------
    private fun renderToFBO(fboID: Int, width: Int, height: Int) {
        // Save the current transformation matrix.
        GlStateManager.pushMatrix()
        // Bind the FBO so that rendering commands draw into it.
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID)
        println("renderToFBO: Bound FBO id=$fboID")

        // Save the current viewport.
        val viewportBuffer = BufferUtils.createIntBuffer(16)
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer)
        viewportBuffer.rewind()
        val viewport = IntArray(4) { viewportBuffer.get() }
        println("renderToFBO: Original viewport = ${viewport.joinToString(", ")}")

        // Set the viewport to cover the entire FBO.
        GL11.glViewport(0, 0, width, height)
        // Clear the FBO’s color and depth buffers.
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

        // Set up a 2D orthographic projection.
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()   // Save the current projection matrix.
        GL11.glLoadIdentity() // Reset projection.
        GL11.glOrtho(0.0, width.toDouble(), height.toDouble(), 0.0, 0.0, 1.0)
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()   // Save the current modelview matrix.
        GL11.glLoadIdentity() // Reset modelview.
        println("renderToFBO: Set orthographic projection for dimensions ($width x $height)")

        // Perform the heavy overlay rendering.
        heavyRenderCall()

        // Restore the matrices.
        GL11.glPopMatrix() // Restore modelview.
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix() // Restore projection.
        GL11.glMatrixMode(GL11.GL_MODELVIEW)

        // Unbind the FBO to resume normal screen rendering.
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
        // Restore the original viewport.
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
        println("renderToFBO: Restored viewport to ${viewport.joinToString(", ")} and unbound FBO")
        GL11.glFinish() // Ensure all FBO rendering commands are completed.
        GlStateManager.popMatrix()
    }

    // ---------- Heavy Render Call ----------
    private fun heavyRenderCall() {
        // Save the current matrix and attribute states.
        GlStateManager.pushMatrix()
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        println("heavyRenderCall: Starting heavy render call.")
        // Post the overlay render event.
        GuiRenderEvent.GuiOverlayRenderEvent().post()
        println("heavyRenderCall: Finished heavy render call.")
        // Restore states.
        GL11.glPopAttrib()
        GlStateManager.popMatrix()
    }

    // ---------- Draw Cached Texture ----------
    private fun drawCachedTexture(texID: Int, x: Float, y: Float, w: Float, h: Float) {
        // Bind the cached texture.
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
        // Begin drawing a textured quad.
        GL11.glBegin(GL11.GL_QUADS)
        // Bottom-left
        GL11.glTexCoord2f(0f, 0f)
        GL11.glVertex2f(x, y)
        // Bottom-right
        GL11.glTexCoord2f(1f, 0f)
        GL11.glVertex2f(x + w, y)
        // Top-right
        GL11.glTexCoord2f(1f, 1f)
        GL11.glVertex2f(x + w, y + h)
        // Top-left
        GL11.glTexCoord2f(0f, 1f)
        GL11.glVertex2f(x, y + h)
        GL11.glEnd()
        println("drawCachedTexture: Rendered quad with texture id=$texID at ($x, $y, $w, $h)")
    }
}
