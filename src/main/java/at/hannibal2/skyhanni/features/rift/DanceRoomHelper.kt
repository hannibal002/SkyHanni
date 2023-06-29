package at.hannibal2.skyhanni.features.rift


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import kotlinx.coroutines.*
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DanceRoomHelper {

    private var display = listOf<String>()
    private val config get() = SkyHanniMod.feature.rift.danceRoomHelper
    private var index = 0
    private var found = false
    val danceRoom = AxisAlignedBB(-260.0, 32.0, -110.0, -267.0, 40.0, -102.0)
    private var inRoom = false
    private val instruction = mutableListOf(
            "move",
            "move",
            "move",
            "move",
            "move",
            "sneak",
            "stand",
            "sneak",
            "stand",
            "sneak",
            "stand",
            "sneak",
            "stand",
            "sneak jump",
            "stand jump",
            "sneak",
            "stand",
            "sneak jump",
            "stand jump",
            "sneak",
            "stand",
            "sneak jump",
            "stand jump",
            "sneak",
            "stand",
            "sneak jump",
            "stand jump",
            "sneak",
            "stand",
            "sneak jump",
            "stand jump",
            "sneak",
            "stand",
            "sneak jump punch",
            "stand jump punch",
            "sneak punch",
            "stand punch",
            "sneak jump punch",
            "stand jump punch",
            "sneak punch",
            "stand punch",
            "sneak jump punch",
            "stand jump punch",
            "sneak punch",
            "stand punch",
            "sneak jump punch",
            "stand jump punch",
            "sneak punch",
            "stand punch",
    ).withIndex()

    fun update() {
        display = buildList {
            for (line in instruction) {
                if (index == line.index) {
                    add("§9§l>>> §c§l${line.value.uppercase()} §9§l<<<")
                } else if (index + 1 == line.index) {
                    add("§e§l${line.value.uppercase()}")
                } else if ((index + 2..index + config.lineToShow).contains(line.index)) {
                    add("§7${line.value.uppercase()}")
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (inRoom) {
            config.position.renderStrings(display,
                    extraSpace = -2,
                    posLabel = "Dance Room Helper")
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        inRoom = false
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        update()
        if (event.isMod(10)) {
            inRoom = danceRoom.isVecInside(net.minecraft.client.Minecraft.getMinecraft().thePlayer.positionVector)
        }
    }

    @SubscribeEvent
    fun onSound(event: at.hannibal2.skyhanni.events.PlaySoundEvent) {
        if (!isEnabled() && !inRoom) return
        if (event.soundName == "random.burp" && event.volume == 0.8f) {
            index = 0
            found = false
            update()
        }
        if (event.soundName == "note.bassattack" && event.pitch == 0.6984127f && event.volume == 1.0f && !found) {
            found = true
            start(2000)
            update()
        }
    }

    fun start(interval: Long): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (NonCancellable.isActive && found) {
                index++
                delay(interval)
            }
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.enabled

}