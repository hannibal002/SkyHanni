package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getLivingMetalProgress
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LivingMetalSuitProgress {

    private val config get() = SkyHanniMod.feature.rift.livingMetalSuitProgress
    private var display = emptyList<List<Any>>()
    private val progressMap = linkedMapOf<ItemStack, Int>()
    private var totalProgress = 0.0

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderStringsAndItems(
                display,
                posLabel = "Living Metal Armor Progress"
        )
    }

    private fun update() {
        display = drawDisplay()
    }

    fun drawDisplay(): List<List<Any>> = buildList {
        val newDisplay = mutableListOf<List<Any>>()
        var piecesMaxed = 0
        var isMaxed = false
        var percent = 0

        for ((_, progress) in progressMap)
            if (progress >= 100) piecesMaxed++
        if (piecesMaxed == 4)
            isMaxed = true

        if (!config.compactWhenMaxed && isMaxed) isMaxed = false

        if (progressMap.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Living Metal Suit Progress: ${if (isMaxed) "§a§lMAXED!" else "§a$totalProgress%"}")
            if (!isMaxed)
                for ((stack, progress) in progressMap.entries.reversed()) {
                    totalProgress += progress
                    newDisplay.add(buildList {
                        add("  §7- ")
                        add(stack)
                        add("${stack.displayName}: ")
                        if (progress == -1) {
                            add("§cStart upgrading it!")
                        } else {
                            add(drawProgressBar(progress, 100))
                            add(" §b$progress%")
                            percent += progress
                        }
                    })
                }
        }
        totalProgress = ((percent.toDouble() / 400) * 100).roundToPrecision(1)
        return newDisplay
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(20)) return
        val armors = InventoryUtils.getArmor()
        progressMap.clear()
        for (armor in armors) {
            armor?.let {
                var progress = it.getLivingMetalProgress() ?: -1
                if (progress > 100) progress = 100
                progressMap.put(it, progress)
            }
        }
        update()
    }

    private fun drawProgressBar(progress: Int, total: Int): String {
        val progressBarLength = 20
        val filledLength = (progress.toFloat() / total * progressBarLength).toInt()

        val green = "§a"
        val grey = "§7"
        val reset = "§f"

        val progressBar = StringBuilder()
        progressBar.append(green)
        repeat(filledLength) { progressBar.append("|") }

        progressBar.append(grey)
        repeat(progressBarLength - filledLength) { progressBar.append("|") }

        progressBar.append(reset)
        return progressBar.toString()
    }

    fun isEnabled() = RiftAPI.inRift() && config.enabled
}