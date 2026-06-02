//? if >= 26.1 {
package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.state.level.CameraRenderState
import org.joml.Quaternionf
import net.minecraft.world.phys.Vec3

val CameraRenderState.position: Vec3 get() = pos
fun CameraRenderState.rotation(): Quaternionf = orientation
fun Camera.getRenderState(): CameraRenderState = CameraRenderState().apply {
    val deltaTracker = Minecraft.getInstance().deltaTracker
    val partialTicks = this@getRenderState.getCameraEntityPartialTicks(deltaTracker)
    //~ if < 26.1 'extractRenderState' -> 'render'
    this@getRenderState.extractRenderState(this, partialTicks)
}
//?}
