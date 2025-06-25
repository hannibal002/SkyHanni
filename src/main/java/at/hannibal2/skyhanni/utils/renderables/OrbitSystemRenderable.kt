package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import kotlin.math.cos
import kotlin.math.sin

enum class OrbitDirection { CLOCKWISE, COUNTER_CLOCKWISE }

// A renderable that has other renderables orbiting it, configurable.
class OrbitSystemRenderable(
    private val mainBody: Renderable,
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
    val subBodies: Collection<Renderable>,
) : Renderable {

    private val subBodyW = (subBodies.maxOfOrNull { it.width } ?: 0)
    private val subBodyH = (subBodies.maxOfOrNull { it.height } ?: 0)

    override val width: Int
        get() = (mainBody.width + (subBodyW * 2) + (subBodySpacing * 2))
    override val height: Int
        get() = (mainBody.height + (subBodyH * 2) + (subBodySpacing * 2))

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

        if (subBodies.isEmpty()) return

        val centerX = posX + width / 2f
        val centerY = posY + height / 2f
        val orbitRadius = (mainBody.width / 2f) + subBodySpacing + (subBodyW / 2f)

        val step = 360f / subBodies.size
        subBodies.forEachIndexed { index, subBody ->
            val angleDeg = currentAngle + step * index
            val radians = Math.toRadians(angleDeg.toDouble())
            val dx = (cos(radians) * orbitRadius).toFloat()
            val dy = (sin(radians) * orbitRadius).toFloat()

            // world‐space coords of the top-left of the scaled sub-body,
            // so that sub.render(0,0) (which draws at 0,0) ends up centered.
            val drawX = centerX + dx - (subBody.width) / 2f
            val drawY = centerY + dy - (subBody.height) / 2f

            val mainBodyHovered = mainBody.isHovered((posX + drawX).toInt(), (posY + drawY).toInt())
            val (fPosX, fPosY) = if (mainBodyHovered) {
                subBody.width + 1 to subBody.height + 1
            } else posX to posY

            DrawContextUtils.pushPop {
                DrawContextUtils.translate(drawX, drawY, 0f)
                subBody.render(fPosX, fPosY)
            }
        }
    }
}
