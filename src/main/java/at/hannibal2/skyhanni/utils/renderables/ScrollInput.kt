package at.hannibal2.skyhanni.utils.renderables

import io.github.moulberry.notenoughupdates.util.Utils
import org.lwjgl.input.Mouse

abstract class ScrollInput(
    private val scrollValue: ScrollValue,
    protected val minValue: Double,
    protected val maxValue: Double,
    protected val dragScrollMouseButton: Int?,
    protected val inverseDrag: Boolean,
    startValue: Double?,
) {

    init {
        scrollValue.init(startValue ?: minValue)
        coerceInLimit()
    }

    protected var scroll
        set(value) {
            scrollValue.setValue(value)
        }
        get() = scrollValue.getValue()

    protected val mouseDiff get() = scrollValue.getMousePositionDiff()

    fun asInt() = scroll.toInt()
    fun asDouble() = scroll

    protected fun coerceInLimit() =
        if (maxValue < minValue) {
            scroll = minValue
        } else {
            scroll = scroll.coerceIn(minValue, maxValue)
        }

    protected fun isMouseEventValid(): Boolean = scrollValue.isMouseEventValid()

    abstract fun update(isValid: Boolean)

    companion object {

        class Vertical(
            scrollValue: ScrollValue,
            minHeight: Number,
            maxHeight: Number,
            val velocityDrag: Double,
            val velocityScroll: Double = velocityDrag * 2.5,
            dragScrollMouseButton: Int?,
            inverseDrag: Boolean = true,
            startValue: Double? = null,
        ) : ScrollInput(
            scrollValue,
            minHeight.toDouble(),
            maxHeight.toDouble(),
            dragScrollMouseButton,
            inverseDrag,
            startValue
        ) {
            override fun update(isValid: Boolean) {
                if (maxValue < minValue) return
                if (!isValid || !isMouseEventValid()) return
                if (dragScrollMouseButton != null && Mouse.isButtonDown(dragScrollMouseButton)) {
                    val diff = mouseDiff
                    scroll += diff.second * velocityDrag * if (inverseDrag) 1 else -1
                }
                val deltaWheel = Mouse.getEventDWheel()
                scroll += -deltaWheel.coerceIn(-1, 1) * velocityScroll
                coerceInLimit()
            }

        }
    }
}

class ScrollValue {
    var field: Double? = null

    var mousePosition: Pair<Int, Int> = Pair(0, 0)

    private var mouseEventTime = 0L

    fun getValue(): Double =
        field ?: throw IllegalStateException("ScrollValue should be initialized before get.")

    fun setValue(value: Double) {
        field = value
    }

    fun init(value: Double) {
        if (field != null) return
        field = value
        mousePosition = mousePosition()
    }

    private fun mousePosition() = Utils.getMouseX() to Utils.getMouseY()

    fun getMousePositionDiff(): Pair<Int, Int> {
        val new = mousePosition()
        val diff = mousePosition - new
        mousePosition = new
        return diff
    }

    fun isMouseEventValid(): Boolean {
        val mouseEvent = Mouse.getEventNanoseconds()
        val mouseEventsValid = mouseEvent - mouseEventTime > 20L
        mouseEventTime = mouseEvent
        return mouseEventsValid
    }

}

private operator fun Pair<Int, Int>.minus(new: Pair<Int, Int>): Pair<Int, Int> =
    Pair(this.first - new.first, this.second - new.second)
