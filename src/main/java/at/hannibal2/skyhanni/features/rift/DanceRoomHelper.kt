package at.hannibal2.skyhanni.features.rift


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.jsonobjects.DanceRoomInstructionsJson
import kotlinx.coroutines.*
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DanceRoomHelper {

    private var display = listOf<String>()
    private val config get() = SkyHanniMod.feature.rift.danceRoomHelper
    private var index = 0
    private var found = false
    private val danceRoom = AxisAlignedBB(-260.0, 32.0, -110.0, -267.0, 40.0, -102.0)
    private var inRoom = false
    private var instructions: List<String> = emptyList()

    fun update() {
        display = buildList {
            for (line in instructions.withIndex()) {
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
        config.position.renderStrings(
                display,
                config.extraSpace,
                posLabel = "Dance Room Helper"
        )
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        inRoom = false
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(10)) {
            inRoom = danceRoom.isVecInside(net.minecraft.client.Minecraft.getMinecraft().thePlayer.positionVector)
        }
        if (inRoom) {
            update()
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

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (RiftAPI.inRift() && config.hidePlayers) {
            val entity = event.entity
            if (entity is EntityOtherPlayerMP) {
                if (danceRoom.isVecInside(entity.getLorenzVec().toVec3())) {
                    event.isCanceled = true
                }
            }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        event.getConstant<DanceRoomInstructionsJson>("DanceRoomInstructions")?.let {
            instructions = it.instructions
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