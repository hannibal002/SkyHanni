package at.hannibal2.skyhanni.features.rift


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import kotlinx.coroutines.*
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DanceRoomHelper {

    private var display = listOf<String>()
    private val config get() = SkyHanniMod.feature.rift.danceRoomHelper
    private var i = 0
    private var found = false
    private val danceRoom = AxisAlignedBB(-260.0, 32.0, -110.0, -267.0, 40.0, -102.0)
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
                if (i == line.index) {
                    add("§9§l>>> §c§l${line.value} §9§l<<<")
                } else if (config.compact && (i + 1..i + config.lineToShow).contains(line.index)) {
                    add("§e§l${line.value}")
                } else if (i < line.index) {
                    add(if (config.compact) "§7" else "§7§m" + line.value)
                } else {
                    add(if (config.compact) "§a" else "§7" + line.value)
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
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        update()
        if (event.isMod(20)) {
            inRoom = danceRoom.isVecInside(net.minecraft.client.Minecraft.getMinecraft().thePlayer.positionVector)
        }
    }

    @SubscribeEvent
    fun onSound(event: at.hannibal2.skyhanni.events.PlaySoundEvent) {
        if (!isEnabled()) return
        if (event.soundName == "random.burp" && event.volume == 0.8f) {
            i = 0
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
                i++
                delay(interval)
            }
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.enabled

}