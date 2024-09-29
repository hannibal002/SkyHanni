package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getCoinsOfAvarice
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CrownOfAvariceCounter {

    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.crownOfAvarice

    private val internalName = "CROWN_OF_AVARICE".asInternalName()

    private var render: Renderable? = null

    private var count: Long = 0L
    private var coinsEarned: Long = 0L
    private var timeSession: SimpleTimeMark = SimpleTimeMark(0)

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if(!isEnabled()) return
        render?.let { config.position.renderRenderable(it, posLabel = "Crown of Avarice Counter") }
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: OwnInventoryItemUpdateEvent) {
        if (!isEnabled() || event.slot != 5) return
        val item = InventoryUtils.getHelmet() ?: return
        if (item.getInternalNameOrNull() != internalName) return
        coinsEarned += calculateCoinDifference()
        count = item.getCoinsOfAvarice() ?: return
        val coinsPerHour = calculateCoinsPerHour().toLong().addSeparators()
        val timeUntilMax = calculateTimeUntilMax()
        render = Renderable.verticalContainer(
            listOf(
                Renderable.horizontalContainer(
                    listOf(
                        Renderable.itemStack(internalName.getItemStack()),
                        Renderable.string("§6" + if (config.shortFormat) count.shortFormat() else count.addSeparators()),
                    ),
                ),
                Renderable.string("§aCoins Per Hour: §6$coinsPerHour"),
                Renderable.string("§aTime until Max: §6$timeUntilMax")
            ),
        )
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent){
        if(!isEnabled()) return
        timeSession = timeSession.plus(1.seconds)
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent){
        coinsEarned = 0L
        timeSession = SimpleTimeMark.farPast()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enable

    private fun calculateCoinsPerHour() : Double {
        var timeInHours = (timeSession.toMillis()/1000.0)/3600.0
        val coinsPerHour = if (timeInHours > 0) coinsEarned / timeInHours else 0.0
        return coinsPerHour
    }

    private fun calculateTimeUntilMax() : String {
        val coinsPerHour = calculateCoinsPerHour()
        val maxAvarice = 1000000000.0
        val timeUntilMax = ((maxAvarice-count)/coinsPerHour).hours
        return timeUntilMax.format()
    }

    private fun calculateCoinDifference() : Long{
        val item = InventoryUtils.getHelmet() ?: return 0L
        val coinDifference = (item.getCoinsOfAvarice() ?: 0L) - count
        return coinDifference
    }
}
