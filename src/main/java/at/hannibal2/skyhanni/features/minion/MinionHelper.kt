package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Mouse
import java.awt.Color

class MinionHelper {

    var lastLocation: LorenzVec? = null
    var lastMinion: LorenzVec? = null
    var lastMinionOpened = 0L
    var minionInventoryOpen = false

    @SubscribeEvent
    fun onClick(event: InputEvent.MouseInputEvent) {
        if (!LorenzUtils.inSkyblock) return

        val minecraft = Minecraft.getMinecraft()
        val buttonState = Mouse.getEventButtonState()
        val eventButton = Mouse.getEventButton()

        if (buttonState) {
            if (eventButton == 1) {
                val entity = minecraft.pointedEntity
                if (entity != null) {
                    lastLocation = entity.getLorenzVec().add(-0.5, 0.0, -0.5)
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (!SkyHanniMod.feature.minions.lastOpenedMinionDisplay) return

        val special = SkyHanniMod.feature.minions.lastOpenedMinionColor
        val color = Color(SpecialColour.specialToChromaRGB(special), true)

        val loc = lastMinion
        if (loc != null) {
            val time = SkyHanniMod.feature.minions.lastOpenedMinionTime * 1_000
            if (lastMinionOpened + time > System.currentTimeMillis()) {
                event.drawWaypointFilled(loc, color)
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (InventoryUtils.currentlyOpenInventory().contains("Minion")) {
            if (lastLocation != null) {
                lastMinion = lastLocation
                lastLocation = null
                minionInventoryOpen = true
                lastMinionOpened = 0
            }
        } else {
            if (minionInventoryOpen) {
                minionInventoryOpen = false
                lastMinionOpened = System.currentTimeMillis()
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        lastLocation = null
        lastMinion = null
        lastMinionOpened = 0L
        minionInventoryOpen = false
    }
}