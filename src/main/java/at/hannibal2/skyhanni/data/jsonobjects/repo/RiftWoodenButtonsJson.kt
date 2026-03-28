package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import net.minecraft.world.phys.Vec3

data class RiftWoodenButtonsJson(
    @Expose val houses: Map<String, List<ButtonSpots>>
)

data class ButtonSpots(
    @Expose val position: Vec3,
    @Expose val buttons: List<Vec3>,
)
