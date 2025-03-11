package at.hannibal2.skyhanni.utils.compat

//#if MC < 1.16
import org.lwjgl.input.Mouse
//#else
//$$ import org.lwjgl.glfw.GLFW
//$$ import net.minecraft.client.Minecraft
//#endif

object MouseCompat {
    //#if MC < 1.16
    fun isButtonDown(button: Int): Boolean = Mouse.isButtonDown(button)

    fun getX(): Int = Mouse.getX()
    fun getY(): Int = Mouse.getY()

    fun getEventButtonState(): Boolean = Mouse.getEventButtonState()
    fun getEventNanoseconds(): Long = Mouse.getEventNanoseconds()

    fun getEventX(): Int = Mouse.getEventX()
    fun getEventY(): Int = Mouse.getEventY()

    fun getEventDY(): Int = Mouse.getEventDY()
    fun getScrollDelta(): Int = Mouse.getEventDWheel()

    fun getEventButton(): Int = Mouse.getEventButton()
    //#else
    //$$ private var scrollDelta = 0.0
    //$$ private var eventDY = 0.0
    //$$
    //$$ private var lastButtonState = false
    //$$ private var lastEventNanoseconds: Long = 0
    //$$
    //$$ private var lastEventX: Int = 0
    //$$ private var lastEventY: Int = 0
    //$$
    //$$ private var lastCursorY = 0.0
    //$$ private var lastEventButton: Int = -1
    //$$
    //$$ private val windowHandle: Long by lazy {
    //$$     // Retrieve the GLFW window handle from the Minecraft client.
    //$$     Minecraft.getInstance().window.window
    //$$ }
    //$$
    //$$ init {
    //$$     // Initialize lastCursorY with the current cursor Y position.
    //$$     val xArray = DoubleArray(1)
    //$$     val yArray = DoubleArray(1)
    //$$     GLFW.glfwGetCursorPos(windowHandle, xArray, yArray)
    //$$     lastCursorY = yArray[0]
    //$$
    //$$     // Register scroll callback to capture scroll deltas.
    //$$     GLFW.glfwSetScrollCallback(windowHandle) { _, _, yoffset ->
    //$$         scrollDelta += yoffset
    //$$     }
    //$$
    //$$     // Register mouse button callback to capture button state and event data.
    //$$     GLFW.glfwSetMouseButtonCallback(windowHandle) { _, button, action, _ ->
    //$$         lastButtonState = (action == GLFW.GLFW_PRESS)
    //$$         lastEventButton = button
    //$$         lastEventNanoseconds = System.nanoTime()
    //$$         val posX = DoubleArray(1)
    //$$         val posY = DoubleArray(1)
    //$$         GLFW.glfwGetCursorPos(windowHandle, posX, posY)
    //$$         lastEventX = posX[0].toInt()
    //$$         lastEventY = posY[0].toInt()
    //$$     }
    //$$
    //$$     // Register cursor position callback to track vertical movement (simulate getEventDY).
    //$$     GLFW.glfwSetCursorPosCallback(windowHandle) { _, _, newY ->
    //$$         eventDY = newY - lastCursorY
    //$$         lastCursorY = newY
    //$$     }
    //$$ }
    //$$
    //$$ fun isButtonDown(button: Int): Boolean =
    //$$     GLFW.glfwGetMouseButton(windowHandle, button) == GLFW.GLFW_PRESS
    //$$
    //$$ fun getScrollDelta(): Double {
    //$$     val delta = scrollDelta
    //$$     scrollDelta = 0.0
    //$$     return delta
    //$$ }
    //$$
    //$$ fun getX(): Int {
    //$$     val posX = DoubleArray(1)
    //$$     val posY = DoubleArray(1)
    //$$     GLFW.glfwGetCursorPos(windowHandle, posX, posY)
    //$$     return posX[0].toInt()
    //$$ }
    //$$
    //$$ fun getY(): Int {
    //$$     val posX = DoubleArray(1)
    //$$     val posY = DoubleArray(1)
    //$$     GLFW.glfwGetCursorPos(windowHandle, posX, posY)
    //$$     return posY[0].toInt()
    //$$ }
    //$$
    //$$ fun getEventButtonState(): Boolean = lastButtonState
    //$$ fun getEventNanoseconds(): Long = lastEventNanoseconds
    //$$
    //$$ fun getEventX(): Int = lastEventX
    //$$ fun getEventY(): Int = lastEventY
    //$$
    //$$ fun getEventDY(): Double {
    //$$     val delta = eventDY
    //$$     eventDY = 0.0
    //$$     return delta
    //$$ }
    //$$
    //$$ fun getEventButton(): Int {
    //$$     val button = lastEventButton
    //$$     lastEventButton = -1
    //$$     return button
    //$$ }
    //#endif
}
