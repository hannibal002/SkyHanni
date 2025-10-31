package at.hannibal2.hanni.features.gui.quiver

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.features.combat.QuiverDisplayConfig.ShowWhen
import at.hannibal2.hanni.data.ArrowType
import at.hannibal2.hanni.data.QuiverApi
import at.hannibal2.hanni.data.QuiverApi.NONE_ARROW_TYPE
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.ProfileJoinEvent
import at.hannibal2.hanni.events.QuiverUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.hanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.RenderUtils
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

@HanniModule
object QuiverDisplay {

    private val config get() = HanniMod.feature.combat.quiverConfig.quiverDisplay

    private var display = emptyList<Renderable>()
    private var arrow: ArrowType? = null
    private var amount = QuiverApi.currentAmount
    private var hideAmount = false

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
        arrow = QuiverApi.currentArrow
        amount = QuiverApi.currentAmount
        updateDisplay()
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val arrow = arrow ?: return@buildList
        val itemStack = arrow.internalName.getItemStackOrNull() ?: ItemStack(Items.arrow)

        val rarity = itemStack.getItemRarityOrNull()?.chatColorCode ?: "§f"
        val arrowDisplayName =
            if (hideAmount || arrow == NONE_ARROW_TYPE) arrow.arrow else StringUtils.pluralize(amount, arrow.arrow)

        if (config.showIcon.get()) {
            addItemStack(itemStack, scale = 1.0)
        }
        if (!hideAmount) addString("§b${amount.addSeparators()}x")
        addString(" $rarity$arrowDisplayName")
    }

    @HandleEvent
    fun onQuiverUpdate(event: QuiverUpdateEvent) {
        arrow = event.currentArrow
        amount = event.currentAmount
        hideAmount = QuiverApi.wearingSkeletonMasterChestplate

        updateDisplay()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) updateDisplay()
        val whenToShow = config.whenToShow.get()
        if (whenToShow == ShowWhen.ALWAYS ||
            whenToShow == ShowWhen.ONLY_BOW_INVENTORY && QuiverApi.hasBowInInventory() ||
            whenToShow == ShowWhen.ONLY_BOW_HAND && QuiverApi.isHoldingBow()
        ) {
            config.quiverDisplayPos.renderRenderable(
                Renderable.horizontal(
                    display,
                    spacing = 1,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                ),
                posLabel = "Quiver Display",
            )
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            config.whenToShow,
            config.showIcon,
        ) {
            updateDisplay()
        }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
