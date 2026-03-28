package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import net.minecraft.world.phys.Vec3

class RescueParkourJson(
    @Expose val mage: Map<String, List<Vec3>>,
    @Expose val barb: Map<String, List<Vec3>>,
)
