package at.hannibal2.skyhanni.features.rift.area.livingcave

import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.features.rift.everywhere.RiftAPI
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LivingCaveLivingMetalHelper {
    private val config get() = RiftAPI.config.area.livingCaveConfig.livingCaveLivingMetalConfig

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        val location = event.location
        val old = event.old
        val new = event.new
        val distanceToPlayer = location.distanceToPlayer()

        if (distanceToPlayer < 10) {
            if (old == "lapis_ore" || new == "lapis_ore") {
                println("block change: $old -> $new")
            }
//            if (old == "wool") return
//            if (new == "wool") return
//            if (old == "lapis_block") return
//            if (new == "lapis_block") return
//            if (old == "stained_glass" && new == "stone") return
//            if (old == "stone" && new == "stained_glass") return
//            if (old == "stained_glass" && new == "stained_hardened_clay") return
//            println("block change: $old -> $new")
        }
    }
}
