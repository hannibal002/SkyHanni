package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import net.minecraft.world.phys.Vec3

data class MetalDetectorChestsJson(
    @Expose val locations: List<Vec3>,
)
