package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import net.minecraft.world.phys.Vec3

data class EnigmaSoulsJson(
    @Expose val areas: Map<String, List<EnigmaPosition>>,
)

data class EnigmaPosition(
    @Expose val name: String,
    @Expose val position: Vec3,
)
