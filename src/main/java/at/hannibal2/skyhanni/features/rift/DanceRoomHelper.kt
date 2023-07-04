package at.hannibal2.skyhanni.features.rift


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.jsonobjects.DanceRoomInstructionsJson
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DanceRoomHelper {

    private var display = emptyList<String>()
    private val config get() = SkyHanniMod.feature.rift.mirrorVerse.danceRoomHelper
    private var index = 0
    private var found = false
    private val danceRoom = AxisAlignedBB(-260.0, 32.0, -110.0, -267.0, 40.0, -102.0)
    private var inRoom = false
    private var instructions = emptyList<String>()

    fun update() {
        display = buildList {
            if (instructions.isEmpty()) {
                add("§cError fetching Dance Room Instructions!")
                add("§cTry §e/shreloadlocalrepo §cor §e/shupdaterepo")
            }
            for (line in instructions.withIndex()) {
                if (index == line.index) {
                    add("§7Now: ${line.value.format()}")
                } else if (index + 1 == line.index) {
                    add("§7Next: ${line.value.format()}")
                } else if ((index + 2..index + config.lineToShow).contains(line.index)) {
                    add("§7Later: ${line.value.format()}")
                }
            }
        }
    }

    private fun String.format() = split(" ").joinToString(" ") { it.firstLetterUppercase().addColor() }

    private fun String.addColor() = when (this) {
        "Move" -> "§e"
        "Stand" -> "§e"
        "Sneak" -> "§5"
        "Jump" -> "§b"
        "Punch" -> "§d"
        else -> "§f"
    } + this

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
            inRoom = danceRoom.isVecInside(Minecraft.getMinecraft().thePlayer.positionVector)
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