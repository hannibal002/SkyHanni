package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeStorage
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

//? if >= 26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState
//?} else {
/*import net.minecraft.client.Camera
*///?}

@PrimaryFunction("onRenderWorld")
class SkyHanniRenderWorldEvent(
    val matrices: PoseStack,
    //? if >= 26.1 {
    val cameraState: CameraRenderState,
    //?} else {
    /*camera: Camera,
    *///?}
    //? if >= 26.2 {
    val submitNodeStorage: SubmitNodeStorage,
    //?} else {
    /*val bufferSource: MultiBufferSource.BufferSource,
    *///?}
    val partialTicks: Float,
    var isCurrentlyDeferring: Boolean = true,
) : SkyHanniEvent() {

    //~ if < 26.1 'cameraState.pos' -> 'camera.position'
    val cameraPos: Vec3 = cameraState.pos

    //~ if < 26.1 'cameraState.orientation' -> 'camera.rotation()'
    val cameraRotation: Quaternionf = cameraState.orientation

    //? if >= 26.2 {
    internal var skyHanniTextSubmitOrder = 0
    //?}
}
