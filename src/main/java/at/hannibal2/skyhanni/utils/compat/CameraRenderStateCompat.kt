package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

val CameraRenderState.position: Vec3 get() = pos
fun CameraRenderState.rotation(): Quaternionf = orientation
