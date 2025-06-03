package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import kotlin.math.cos
import kotlin.math.sin

enum class OrbitDirection { CLOCKWISE, COUNTER_CLOCKWISE }

class OrbitSystemRenderable(
    private val mainBody: CircularRenderable,
    /**
     * How large subBodies should be in relation to the main renderable.
     */
    private val subBodyScale: Float = 0.4f,
    /**
     * Spacing between the main body and sub bodies.
     */
    private val subBodySpacing: Int = 1,
    /**
     * How fast along the orbit path sub, in degrees, bodies should move per cycle.
     * Set to 0 to keep orbits stationary.
     */
    private val orbitSpeed: Int = 10,
    private val orbitDirection: OrbitDirection = OrbitDirection.CLOCKWISE,
    vararg subBodies: CircularRenderable,
) : Renderable {
    private val qualifiedSubBodies: List<CircularRenderable> = listOf(*subBodies)

    private val subBodyW = (qualifiedSubBodies.maxOfOrNull { it.width } ?: 0) * subBodyScale
    private val subBodyH = (qualifiedSubBodies.maxOfOrNull { it.height } ?: 0) * subBodyScale

    override val width: Int
        get() = (mainBody.width + subBodyW + subBodySpacing).toInt()
    override val height: Int
        get() = (mainBody.height + subBodyH + subBodySpacing).toInt()

    override val horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
    override val verticalAlign = RenderUtils.VerticalAlignment.CENTER

    private var lastTime = SimpleTimeMark.now()
    private var currentAngle = 0f

    override fun render(posX: Int, posY: Int) {
        val now = SimpleTimeMark.now()
        val deltaSeconds = (now - lastTime).inPartialSeconds
        lastTime = now

        val dirFactor = if (orbitDirection == OrbitDirection.CLOCKWISE) 1 else -1
        currentAngle = (currentAngle + orbitSpeed * deltaSeconds * dirFactor).toFloat() % 360f
        mainBody.renderXYAligned(posX, posY, width, height)

        if (qualifiedSubBodies.isEmpty()) return

        val centerX = posX + width  / 2f
        val centerY = posY + height / 2f
        val orbitRadius = (mainBody.width / 2f) + subBodySpacing + (subBodyW / 2f)

        val step = 360f / qualifiedSubBodies.size
        qualifiedSubBodies.forEachIndexed { i, sub ->
            val angleDeg = currentAngle + step * i
            val radians  = Math.toRadians(angleDeg.toDouble())
            val dx = (cos(radians) * orbitRadius).toFloat()
            val dy = (sin(radians) * orbitRadius).toFloat()

            // world‐space coords of the top-left of the scaled sub-body,
            // so that sub.render(0,0) (which draws at 0,0) ends up centered.
            val drawX = centerX + dx - (sub.width * subBodyScale) / 2f
            val drawY = centerY + dy - (sub.height * subBodyScale) / 2f

            DrawContextUtils.pushPop {
                DrawContextUtils.translate(drawX, drawY, 0f)
                DrawContextUtils.scale(subBodyScale, subBodyScale, 1f)
                sub.render(0, 0)
            }
        }
    }
}
