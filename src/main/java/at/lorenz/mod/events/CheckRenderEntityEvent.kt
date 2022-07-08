package at.lorenz.mod.events

import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
data class CheckRenderEntityEvent<T : Entity>(
    val entity: T,
    val camera: ICamera,
    val camX: Double,
    val camY: Double,
    val camZ: Double
) : LorenzEvent()