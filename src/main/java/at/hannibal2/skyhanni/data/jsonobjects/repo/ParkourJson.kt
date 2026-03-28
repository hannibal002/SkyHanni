package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import net.minecraft.world.phys.Vec3

data class ParkourJson(
    @Expose val locations: List<Vec3>,
    @Expose val shortCuts: List<ParkourShortCut> = emptyList(),
)

data class ParkourShortCut(
    @Expose val from: Int,
    @Expose val to: Int,
)
