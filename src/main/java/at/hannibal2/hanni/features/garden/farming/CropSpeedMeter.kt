package at.hannibal2.hanni.features.garden.farming

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.data.GardenCropMilestones.getCounter
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.garden.farming.CropClickEvent
import at.hannibal2.hanni.events.garden.farming.CropMilestoneUpdateEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.features.garden.CropType
import at.hannibal2.hanni.features.garden.GardenApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.roundTo
import at.hannibal2.hanni.utils.RenderUtils.renderStrings

@HanniModule
object CropSpeedMeter {

    private var display = emptyList<String>()
    private var currentCrop: CropType? = null
    private var currentBlocks = 0
    private var snapshot = emptyList<String>()

    var enabled = false
    private var startCrops = mapOf<CropType, Long>()

    @HandleEvent
    fun onCropClick(event: CropClickEvent) {
        if (!isEnabled()) return
        if (startCrops.isEmpty()) return

        val crop = event.crop
        if (currentCrop != crop) {
            currentCrop = crop
            currentBlocks = 0
            snapshot = emptyList()
        }
        breakBlock()
    }

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(15)) return

        updateDisplay()
    }

    private fun updateDisplay() {
        display = renderDisplay()
    }

    private fun renderDisplay(): MutableList<String> {
        val list = mutableListOf<String>()
        list.add("§7Crop Speed Meter")
        if (startCrops.isEmpty()) {
            list.add("§cOpen §e/cropmilestones §cto start!")
            return list
        }

        if (currentCrop == null) {
            list.add("§cStart breaking blocks!")
            return list
        }
        currentCrop?.let {
            list.add(" §7Current ${it.cropName} counter: §e${currentBlocks.addSeparators()}")
        }

        if (snapshot.isNotEmpty()) {
            list += snapshot
        } else {
            list.add("§cOpen §e/cropmilestones §cagain to calculate!")
        }

        return list
    }

    @HandleEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        if (!isEnabled()) return
        val counters = mutableMapOf<CropType, Long>()
        for (cropType in CropType.entries) {
            counters[cropType] = cropType.getCounter()
        }
        if (startCrops.isEmpty()) {
            startCrops = counters
            currentCrop = null
            snapshot = emptyList()
        } else {
            currentCrop?.let {
                val crops = it.getCounter() - (startCrops[it] ?: 0L)
                val blocks = currentBlocks
                val cropsPerBlocks = (crops.toDouble() / blocks.toDouble()).roundTo(3)

                val list = mutableListOf<String>()
                list.add("")
                list.add("§6Calculation results")
                list.add(" §7Crops collected: " + crops.addSeparators())
                list.add(" §7Blocks broken: " + blocks.addSeparators())
                list.add(" §7Crops per Block: " + cropsPerBlocks.addSeparators())

                val baseDrops = it.baseDrops
                val farmingFortune = (cropsPerBlocks * 100 / baseDrops).roundTo(3)

                list.add(" §7Calculated farming Fortune: §e" + farmingFortune.addSeparators())
                list.add("§cOpen /cropmilestones again to recalculate!")

                snapshot = list
                updateDisplay()
            }
        }
    }

    private fun breakBlock() {
        currentBlocks++
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        GardenApi.config.cropSpeedMeterPos.renderStrings(display, posLabel = "Crop Speed Meter")
    }

    fun isEnabled() = enabled && GardenApi.inGarden()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcropspeedmeter") {
            description = "Debugs how many crops you collect over time"
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                enabled = !enabled
                ChatUtils.chat("Crop Speed Meter " + if (enabled) "§aEnabled" else "§cDisabled")
                startCrops = emptyMap()
            }
        }
    }
}
