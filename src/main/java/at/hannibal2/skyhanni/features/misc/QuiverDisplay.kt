package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ArrowType
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.data.QuiverAPI.NONE_ARROW_TYPE
import at.hannibal2.skyhanni.data.QuiverAPI.arrowAmount
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.DungeonEnterEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.QuiverUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class QuiverDisplay {

    private val config get() = SkyHanniMod.feature.misc.quiverDisplay

    private var display = emptyList<Renderable>()
    private var arrow = QuiverAPI.currentArrow
    private var amount = 0
    private var hideAmount = false
    private var arrowsUsedInRun = mutableListOf<ArrowType>()
    private var arrowsToAlert = mutableListOf<String>()
    private var inInstance = false

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
        updateDisplay()
    }

    @SubscribeEvent
    fun onInstanceEnter(event: DungeonEnterEvent) {
        arrowsUsedInRun = mutableListOf()
        arrowsToAlert = mutableListOf()
        inInstance = true
    }

    @SubscribeEvent
    fun onWorldSwitch(event: LorenzWorldChangeEvent) {
        arrowsUsedInRun = mutableListOf()
        arrowsToAlert = mutableListOf()
        inInstance = false
    }

    @SubscribeEvent
    fun onInstanceComplete(event: DungeonCompleteEvent) {
        if (!config.reminderAfterRun) return
        if (arrowsUsedInRun.isEmpty()) return
        for (arrow in arrowsUsedInRun) {
            if ((arrowAmount[arrow.internalName] ?: return) <= config.lowQuiverAmount) {
                arrowsToAlert.add(arrow.arrow)
            }
        }
        if (arrowsToAlert.isNotEmpty()) instanceAlert()
    }

    private fun instanceAlert() {
        DelayedRun.runNextTick {
            TitleManager.sendTitle("§cLow on arrows!", 5.seconds, 3.6, 7f)
            ChatUtils.chat("Low on ${StringUtils.createCommaSeparatedList(arrowsToAlert)}!")
            SoundUtils.repeatSound(100, 30, SoundUtils.plingSound)
        }
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val arrow = arrow ?: return@buildList
        val itemStack = NEUItems.getItemStackOrNull(arrow.internalName.asString()) ?: ItemStack(Items.arrow)

        val rarity = itemStack.getItemRarityOrNull()?.chatColorCode ?: "§f"
        val arrowDisplayName = if (hideAmount || arrow == NONE_ARROW_TYPE) arrow.arrow else StringUtils.pluralize(amount, arrow.arrow)

        if (config.showIcon) {
            add(Renderable.itemStack(itemStack,1.68))
        }
        if (!hideAmount) {
            add(Renderable.string(" §b${amount}x"))
        }
        add(Renderable.string(" $rarity$arrowDisplayName"))
    }

    @SubscribeEvent
    fun onQuiverUpdate(event: QuiverUpdateEvent) {
        arrow = event.currentArrow
        amount = event.currentAmount
        hideAmount = event.hideAmount
        updateDisplay()
        if (inInstance) {
            if (!arrowsUsedInRun.contains(arrow)) arrowsUsedInRun.add(arrow ?: return)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyWithBow && !QuiverAPI.hasBowInInventory()) {
            if (display.isNotEmpty()) display = emptyList()
        } else {
            updateDisplay()
            if (display.isEmpty()) return
        }
        config.quiverDisplayPos.renderStringsAndItems(listOf(display), posLabel = "Quiver Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
