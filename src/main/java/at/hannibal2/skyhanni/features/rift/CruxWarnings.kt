package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CruxWarnings {
    private val shyNames = arrayOf("I'm ugly! :(", "Eek!", "Don't look at me!", "Look away!")

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (event.isMod(2)) {
            checkForShy()
        }
    }

    private fun checkForShy() {
        try {
            if (!(RiftAPI.inRift() || !SkyHanniMod.feature.rift.crux.shyWarning)) return
            val list = Minecraft.getMinecraft().theWorld?.getLoadedEntityList() ?: return
            if (list.any { it.name in shyNames && it.distanceToPlayer() < 8 }) {
                TitleUtils.sendTitle("§eLook away!", 250)
            }
        } catch (e: Throwable) {
            CopyErrorCommand.logError(e, "Check for Shy failed")
        }
    }
}