package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.animation.AnimatedSkullRecorder.getFrameTexture
import at.hannibal2.skyhanni.test.animation.AnimatedSkullRecorder.isPetTextureStand
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getPlayerEntities
import at.hannibal2.skyhanni.utils.MobUtils
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object AnimatedSkullRecorderHandler {

    private var lastServerTick = 0L

    @HandleEvent
    fun onServerTick(event: ServerTickEvent) {
        lastServerTick = event.tick
        val serverTick = lastServerTick
        val clientTick = ClientEvents.totalTicks
        val current = AnimatedSkullRecorder.state.takeIf { it.mode != AnimatedSkullRecorder.RecordingMode.NONE } ?: return
        if (Minecraft.getInstance().level == null) return AnimatedSkullRecorder.state.reset()
        when (current.mode) {
            AnimatedSkullRecorder.RecordingMode.NONE -> return
            AnimatedSkullRecorder.RecordingMode.HEAD -> {
                val frame = Minecraft.getInstance().player?.getItemBySlot(EquipmentSlot.HEAD)?.getFrameTexture()
                current.tracker.record(serverTick, clientTick, frame)
            }

            AnimatedSkullRecorder.RecordingMode.PET -> EntityUtils.getEntitiesNearby<ArmorStand>(32.0) {
                it.isPetTextureStand()
            }.forEach { stand ->
                val displayName = MobUtils.getArmorStandByRangeAll(stand, 2.0).firstOrNull {
                    it.cleanName().startsWith("[Lv")
                }?.name?.string ?: stand.name.string
                val recording = current.petRecordings.getOrPut(stand.id to displayName) {
                    AnimatedSkullRecorder.ArmorStandRecording(stand.id, displayName)
                }
                stand.getItemBySlot(EquipmentSlot.MAINHAND).getFrameTexture()?.let {
                    recording.tracker.record(serverTick, clientTick, it)
                }
            }

            AnimatedSkullRecorder.RecordingMode.PLAYER -> {
                val frame = getPlayerEntities().firstOrNull {
                    it.name.string.equals(current.trackedPlayer, ignoreCase = true)
                }?.getItemBySlot(EquipmentSlot.HEAD)?.getFrameTexture()
                current.tracker.record(serverTick, clientTick, frame)
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) = AnimatedSkullRecorderCommand.handleEvent(event)
}
