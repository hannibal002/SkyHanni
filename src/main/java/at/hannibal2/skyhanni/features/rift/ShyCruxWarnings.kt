package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ShyCruxWarnings {
    private val shyNames = arrayOf("I'm ugly! :(", "Eek!", "Don't look at me!", "Look away!")

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!RiftAPI.inRift() || !SkyHanniMod.feature.rift.crux.shyWarning) return
        if (event.isMod(2)) {
            checkForShy()
        }
    }

    private fun checkForShy() {
        val list = Minecraft.getMinecraft().theWorld?.getLoadedEntityList() ?: return
        if (list.any { it.name in shyNames && it.distanceToPlayer() < 8 }) {
            TitleUtils.sendTitle("§eLook away!", 150)
        }
    }
}