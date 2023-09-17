package at.hannibal2.skyhanni.features.misc.tiarelay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TiaRelayHelper {
    private val config get() = SkyHanniMod.feature.inventory.helper.tiaRelay
    private var inInventory = false

    private var lastClickSlot = 0
    private var lastClickTime = 0L
    private var sounds = mutableMapOf<Int, Sound>()

    private var resultDisplay = mutableMapOf<Int, Int>()

    @SubscribeEvent
    fun onSoundPlay(event: PlaySoundEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val soundName = event.soundName

        if (config.tiaRelayMute) {
            if (soundName == "mob.wolf.whine") {
                event.isCanceled = true
            }
        }

        if (!config.soundHelper) return
        if (!inInventory) return

        val distance = event.distanceToPlayer
        if (distance >= 2) return

        if (lastClickSlot == 0) return
        val duration = System.currentTimeMillis() - lastClickTime
        if (duration > 1_000) return
        if (sounds.contains(lastClickSlot)) return

        sounds[lastClickSlot] = Sound(soundName, event.pitch)

        lastClickSlot = 0

        tryResult()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.soundHelper) return

        if (event.repeatSeconds(1)) {
            if (InventoryUtils.openInventoryName().contains("Network Relay")) {
                inInventory = true
            } else {
                inInventory = false
                sounds.clear()
                resultDisplay.clear()
            }
        }
    }

    private fun tryResult() {
        if (sounds.size < 4) return

        val name = sounds.values.first().name
        for (sound in sounds.toMutableMap()) {
            if (sound.value.name != name) {
                LorenzUtils.chat("§c[SkyHanni] Tia Relay Helper error: Too much background noise! Please try again.")
                sounds.clear()
                return
            }
        }

        val pitchMap = mutableMapOf<Int, Float>()
        for (sound in sounds) {
            pitchMap[sound.key] = sound.value.pitch
        }
        sounds.clear()
        resultDisplay.clear()

        var i = 1
        for (entry in pitchMap.sorted()) {
            resultDisplay[entry.key] = i
            i++
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.soundHelper) return
        if (!inInventory) return

        val slot = event.slot
        val stack = slot.stack

        val slotNumber = slot.slotNumber

        val position = resultDisplay.getOrDefault(slotNumber, null)
        if (position != null) {
            if (stack.getLore().any { it.contains("Done!") }) {
                resultDisplay.clear()
                return
            }
            event.stackTip = "#$position"
            return
        }

        if (!sounds.contains(slotNumber)) {
            if (stack.getLore().any { it.contains("Hear!") }) {
                event.stackTip = "Hear!"
                event.offsetX = 5
                event.offsetY = -5
                return
            }
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.soundHelper) return
        if (!inInventory) return

        // only listen to right clicks
        if (event.clickedButton != 1) return

        lastClickSlot = event.slotId
        lastClickTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.tiaRelayMute", "inventory.helper.tiaRelay.tiaRelayMute")
        event.move(2, "misc.tiaRelayHelper", "inventory.helper.tiaRelay.soundHelper")

        event.move(2, "misc.tiaRelayNextWaypoint", "inventory.helper.tiaRelay.nextWaypoint")
        event.move(2, "misc.tiaRelayAllWaypoints", "inventory.helper.tiaRelay.allWaypoints")
    }

    class Sound(val name: String, val pitch: Float)
}