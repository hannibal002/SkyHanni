package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.ExtraOperators.minus
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

    protected fun mouseDiff(mouse: Pair<Int, Int>) = scrollValue.getMousePositionDiff(mouse)

    fun asInt() = scroll.toInt()
    fun asDouble() = scroll

    protected fun coerceInLimit() =
        if (maxValue < minValue) {
            scroll = minValue
        } else {
            scroll = scroll.coerceIn(minValue, maxValue)
        }

    protected fun isMouseEventValid(): Boolean = scrollValue.isMouseEventValid()

    abstract fun update(isValid: Boolean, mouse: Pair<Int, Int>? = null)

    companion object {

        class Vertical(
            scrollValue: ScrollValue,
            minHeight: Number,
            maxHeight: Number,
            private val velocityDrag: Double,
            private val velocityScroll: Double = velocityDrag * 2.5,
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
            override fun update(isValid: Boolean, mouse: Pair<Int, Int>?) {
                if (maxValue < minValue) return
                if (!isValid || !isMouseEventValid()) return
                if (dragScrollMouseButton != null && mouse != null && Mouse.isButtonDown(dragScrollMouseButton)) {
                    val diff = mouseDiff(mouse)
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
    }

    fun getMousePositionDiff(newMouse: Pair<Int, Int>): Pair<Int, Int> {
        val diff = mousePosition - newMouse
        mousePosition = newMouse
        return diff
    }

    fun isMouseEventValid(): Boolean {
        val mouseEvent = Mouse.getEventNanoseconds()
        val mouseEventsValid = mouseEvent - mouseEventTime > 20L
        mouseEventTime = mouseEvent
        return mouseEventsValid
    }

}
