package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.itemability.CrownOfAvariceConfig.CrownOfAvariceLines
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.billion
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getCoinsOfAvarice
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addHorizontalSpacer
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.inPartialHours
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.addLine
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CrownOfAvariceCounter {

    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.crownOfAvarice

    private val internalName = "CROWN_OF_AVARICE".toInternalName()

    private var display: List<Renderable> = emptyList()
    private val MAX_AVARICE_COINS = 1.billion
    private val MAX_AFK_TIME = 2.minutes
    private var inventoryOpen = false
    private val isWearingCrown by RecalculatingValue(1.seconds) {
        InventoryUtils.getHelmet()?.getInternalNameOrNull() == internalName
    }

    private var count: Long? = null
    private var coinsEarned: Long = 0L
    private var sessionUptime: Duration = 0.seconds
    private var lastCoinUpdate: SimpleTimeMark? = null
    private var isPaused: Boolean = false
    private val isSessionActive get(): Boolean = sessionUptime < config.sessionActiveTime.seconds
    private var coinsDifference: Long? = null

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isEnabled() && isWearingCrown },
            onRender = { renderDisplay(config.position) }
        )
    }

    fun renderDisplay(pos: Position) {
        val invCurrentlyOpen = Minecraft.getMinecraft().currentScreen?.let { it is GuiInventory || it is GuiChest } ?: false
        if (inventoryOpen != invCurrentlyOpen) {
            inventoryOpen = invCurrentlyOpen
            update()
        }

        pos.renderRenderables(display, posLabel = "Crown of Avarice Counter")
    }


    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!isWearingCrown) return
        if (!isSessionAFK()) sessionUptime += 1.seconds
        update()
    }

    @HandleEvent
    fun onInventoryUpdated(event: OwnInventoryItemUpdateEvent) {
        if (!isEnabled() || event.slot != 5) return
        val item = event.itemStack
        if (item.getInternalNameOrNull() != internalName) return
        val coins = item.getCoinsOfAvarice() ?: return
        if (count == null) count = coins
        coinsDifference = coins - (count ?: 0)

        if (coinsDifference == 0L) return

        if ((coinsDifference ?: 0) < 0) {
            reset()
            count = coins
            return
        }

        isPaused = false
        lastCoinUpdate = SimpleTimeMark.now()
        coinsEarned += coinsDifference ?: 0
        count = coins

        update()
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (config.resetOnWorldChange) reset()
        count = InventoryUtils.getHelmet()?.getCoinsOfAvarice()
    }

    private fun update() {
        display = buildDisplay()
    }

    private fun fmtDisplay(lines: MutableMap<CrownOfAvariceLines, Renderable>): List<Renderable> {
        val newList = mutableListOf<Renderable>()
        newList.addLine {
            addItemStack(internalName.getItemStack())
            addString("§6" + if (config.shortFormat) count?.shortFormat() else count?.addSeparators())
        }
        newList.addAll(config.text.mapNotNull { lines[it] })

        if (inventoryOpen) {
            newList.addLine {
                add(Renderable.clickable(text = "§c[Reset session]", onLeftClick = ::reset))
                addHorizontalSpacer(3)
                add(Renderable.clickable(text = "§6[Pause session]", onLeftClick = ::pauseSession))
            }
        }
        return newList
    }

    private fun buildDisplay(): List<Renderable> {

        val lines = mutableMapOf<CrownOfAvariceLines, Renderable>()
        lines[CrownOfAvariceLines.COINSPERHOUR] = Renderable.horizontal {
            val coinsPerHour = calculateCoinsPerHour().toLong()
            addString(
                "§aCoins Per Hour: §6${
                    if (isSessionActive) "Calculating..."
                    else if (config.shortFormatCPH) coinsPerHour.shortFormat() else coinsPerHour.addSeparators()
                } " + if (isSessionAFK()) "§c(PAUSED)" else "",
            )
        }
        lines[CrownOfAvariceLines.TIMEUNTILMAX] = Renderable.horizontal {
            val timeUntilMax = calculateTimeUntilMax()
            addString(
                "§aTime until Max: §6${if (isSessionActive) "Calculating..." else timeUntilMax} " +
                    if (isSessionAFK()) "§c(PAUSED)" else "",
            )
        }

        lines[CrownOfAvariceLines.COINDIFFERENCE] = Renderable.horizontal {
            addString("§aLast coins gained: §6$coinsDifference")
        }

        lines[CrownOfAvariceLines.SESSIONCOINS] = Renderable.horizontal {
            addString("§aCoins this session: §6${coinsEarned.addSeparators()}")
        }

        lines[CrownOfAvariceLines.SESSIONTIME] = Renderable.horizontal {
            addString("§aSession Time: §6${sessionUptime.format()}")
        }

        return fmtDisplay(lines)
    }


    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enable

    private fun reset() {
        isPaused = false
        coinsEarned = 0L
        sessionUptime = 0.seconds
        lastCoinUpdate = SimpleTimeMark.now()
        coinsDifference = 0L
    }

    private fun pauseSession() {
        isPaused = true
    }


    private fun calculateCoinsPerHour(): Double {
        val timeInHours = sessionUptime.inPartialHours
        return if (timeInHours > 0) coinsEarned / timeInHours else 0.0
    }

    private fun isSessionAFK() = lastCoinUpdate?.passedSince()?.let { it > MAX_AFK_TIME || isPaused } ?: false

    private fun calculateTimeUntilMax(): String {
        val coinsPerHour = calculateCoinsPerHour()
        if (coinsPerHour == 0.0) return "Forever..."
        val timeUntilMax = ((MAX_AVARICE_COINS - (count ?: 0)) / coinsPerHour).hours
        return timeUntilMax.format()
    }

}
