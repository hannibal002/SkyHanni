package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.ChatFormatting
import java.util.EnumMap

@SkyHanniModule
object SharkFishCounter {

    private enum class SharkType(
        val displayName: String,
        val color: LorenzColor,
    ) {
        NURSE("Nurse", LorenzColor.GREEN),
        BLUE("Blue", LorenzColor.BLUE),
        TIGER("Tiger", LorenzColor.DARK_PURPLE),
        GREAT_WHITE("Great White", LorenzColor.GOLD),
    }

    private val counterMap = object : EnumMap<SharkType, Int>(SharkType::class.java) {
        override fun clear() {
            SharkType.entries.forEach { this[it] = 0 }
        }
        init {
            this.clear()
        }
    }
    private val totalCount get() = counterMap.values.sum()

    private var display: Renderable? = null
    private var hasWaterRodInHand = false

    @HandleEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return

        val eventName = event.seaCreature.name.takeIf { it.contains("Shark") } ?: return
        val shark = SharkType.entries.find { shark ->
            eventName.contains(shark.displayName)
        } ?: return

        counterMap.addOrPut(shark, if (event.doubleHook) 2 else 1)
        val countString = counterMap.entries.joinToString(" ") { (shark, count) ->
            shark.color.getChatColor() + count
        }
        val separatedCount = totalCount.addSeparators()

        display = Renderable.text("§7Sharks caught: §e$separatedCount §7($countString§7)")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return

        if (event.isMod(10)) {
            hasWaterRodInHand = isWaterFishingRod()
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (event.message != "§b§lFISHING FESTIVAL §r§eThe festival has concluded! Time to dry off and repair your rods!") return
        val count = totalCount.takeIf { it != 0 } ?: return

        val (nurse, blue, tiger, great) = counterMap.entries.map { it.value }
        val total = count.addSeparators()
        val funnyComment = funnyComment(count)
        ChatUtils.chat {
            append("You caught $total ")
            appendWithColor("(", ChatFormatting.WHITE)
            appendWithColor("$nurse ", ChatFormatting.GREEN)
            appendWithColor("$blue ", ChatFormatting.GOLD)
            appendWithColor("$tiger ", ChatFormatting.DARK_PURPLE)
            appendWithColor("$great", ChatFormatting.GOLD)
            appendWithColor(") ", ChatFormatting.WHITE)
            append("sharks during this fishing festival. $funnyComment")
        }
        counterMap.clear()
        display = null
    }

    private fun funnyComment(count: Int): String = when {
        count < 50 -> "Well done!"
        count < 100 -> "Nice!"
        count < 150 -> "Really nice!"
        count < 200 -> "Super cool!"
        count < 250 -> "Mega cool!"
        count < 350 -> "Like a pro!"
        else -> "How???"
    }

    private fun isWaterFishingRod() = FishingApi.isFishing() && !FishingApi.holdingLavaRod

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return
        if (!hasWaterRodInHand) return

        val display = display ?: return
        SkyHanniMod.feature.fishing.sharkFishCounterPos.renderRenderable(display, posLabel = "Shark Fish Counter")
    }
}
