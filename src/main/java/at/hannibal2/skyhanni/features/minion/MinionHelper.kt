package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Mouse
import java.awt.Color

class MinionHelper {

    var lastClickedEntity: LorenzVec? = null
    var lastMinion: LorenzVec? = null
    var lastMinionOpened = 0L
    var minionInventoryOpen = false

    var lastCoinsRecived = 0L
    val minions = mutableMapOf<LorenzVec, Long>()

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for (minion in SkyHanniMod.feature.hidden.minions) {
            val vec = LorenzVec.decodeFromString(minion.key)
            minions[vec] = minion.value
        }
    }

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
                    lastClickedEntity = entity.getLorenzVec()
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
                event.drawWaypointFilled(loc.add(-0.5, 0.0, -0.5), color, true)
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (InventoryUtils.currentlyOpenInventory().contains("Minion")) {
            if (lastClickedEntity != null) {
                lastMinion = lastClickedEntity
                lastClickedEntity = null
                minionInventoryOpen = true
                lastMinionOpened = 0
            }
        } else {
            if (minionInventoryOpen) {
                minionInventoryOpen = false
                lastMinionOpened = System.currentTimeMillis()

                val duration = System.currentTimeMillis() - lastCoinsRecived
                if (duration < 2_000) {
                    val loc = lastMinion
                    if (loc != null) {
                        minions[loc] = System.currentTimeMillis()
                        SkyHanniMod.feature.hidden.minions[loc.encodeToString()] = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        lastClickedEntity = null
        lastMinion = null
        lastMinionOpened = 0L
        minionInventoryOpen = false
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyblock) return

        if (event.message.matchRegex("§aYou received §r§6(.*) coins§r§a!")) {
            lastCoinsRecived = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (!SkyHanniMod.feature.minions.emptiedTimeDisplay) return
        if (LorenzUtils.skyBlockIsland != "Private Island") return

        val playerLocation = LocationUtils.playerLocation()
        for (minion in minions) {
            val location = minion.key
            if (playerLocation.distance(location) < SkyHanniMod.feature.minions.emptiedTimeDistance) {
                val duration = System.currentTimeMillis() - minion.value
                val format = StringUtils.formatDuration(duration / 1000)

                val text = "§eLast emptied: $format"
                event.drawString(location.add(0.0, 2.0, 0.0), text, true)
            }
        }
    }
}