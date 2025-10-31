package at.hannibal2.hanni.features.itemabilities

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.features.itemability.CrownOfAvariceConfig.CrownOfAvariceLines
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.NeuItems.getItemStack
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.billion
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.RecalculatingValue
import at.hannibal2.hanni.utils.RenderDisplayHelper
import at.hannibal2.hanni.utils.RenderUtils.renderRenderables
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getCoinsOfAvarice
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.Stopwatch
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addHorizontalSpacer
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.inPartialHours
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.addLine
import at.hannibal2.hanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.hanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@HanniModule
object CrownOfAvariceCounter {

    private val config get() = HanniMod.feature.inventory.itemAbilities.crownOfAvarice

    private val internalName = "CROWN_OF_AVARICE".toInternalName()

    private var display: List<Renderable> = emptyList()
    private val MAX_AVARICE_COINS = 1.billion
    private var inventoryOpen = false
    private val isWearingCrown by RecalculatingValue(1.seconds) {
        InventoryUtils.getHelmet()?.getInternalNameOrNull() == internalName
    }

    private var totalCoins: Long? = null
    private var coinsEarned: Long = 0L
    private var sessionUptime: Stopwatch = Stopwatch()
    private val isSessionActive get(): Boolean = sessionUptime.getDuration() < config.sessionActiveTime.seconds
    private var coinsDifference: Long? = null

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isEnabled() && isWearingCrown },
            onRender = { renderDisplay() },
        )
    }

    fun renderDisplay() {
        val invCurrentlyOpen = InventoryUtils.inAnyInventory()
        if (inventoryOpen != invCurrentlyOpen) {
            inventoryOpen = invCurrentlyOpen
            update()
        }

        config.position.renderRenderables(display, posLabel = "Crown of Avarice Counter")
    }


    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!isEnabled()) return
        if (!isWearingCrown) return
        // No need to update if paused, we'll unpause with onInventoryUpdated
        if (sessionUptime.isPaused()) return
        update()
    }

    @HandleEvent
    fun onInventoryUpdated(event: OwnInventoryItemUpdateEvent) {
        if (!isEnabled() || event.slot != 5) return
        val item = event.itemStack
        if (item.getInternalNameOrNull() != internalName) return
        val coins = item.getCoinsOfAvarice() ?: return
        if (totalCoins == null) totalCoins = coins
        coinsDifference = coins - (totalCoins ?: 0)

        if (coinsDifference == 0L) return

        if ((coinsDifference ?: 0) < 0) {
            reset()
            totalCoins = coins
            return
        }

        sessionUptime.start() // does nothing if already un-paused
        sessionUptime.lap() // mark last added coins time for afk timeout
        coinsEarned += coinsDifference ?: 0
        totalCoins = coins

        update()
    }

    @HandleEvent(IslandChangeEvent::class)
    fun onIslandChange() {
        if (config.resetOnWorldChange) reset()
        totalCoins = InventoryUtils.getHelmet()?.getCoinsOfAvarice()
    }

    private fun update() {
        if (sessionUptime.getLapTime()?.let { it > config.afkTimeout.seconds } != false) {
            sessionUptime.pause(true)
        }
        display = buildDisplay()
    }

    private fun formatDisplay(lines: Map<CrownOfAvariceLines, Renderable>): List<Renderable> {
        val newList = mutableListOf<Renderable>()
        newList.addLine {
            addItemStack(internalName.getItemStack())
            val format = totalCoins?.let {
                if (config.shortFormat) it.shortFormat() else it.addSeparators()
            } ?: "0"
            addString("§6$format")
        }
        newList.addAll(config.text.mapNotNull { lines[it] })

        if (inventoryOpen) {
            newList.addLine {
                add(
                    if (coinsEarned == 0L) {
                        Renderable.text("§8[Reset session]")
                    } else {
                        Renderable.clickable(
                            text = "§c[Reset session]",
                            onLeftClick = ::reset,
                            tips = listOf("§eClick to reset the current session!"),
                        )
                    },
                )
                addHorizontalSpacer(3)
                add(
                    if (sessionUptime.isPaused()) {
                        Renderable.text("§8[Pause session]")
                    } else {
                        Renderable.clickable(
                            text = "§6[Pause session]",
                            onLeftClick = {
                                pauseSession()
                                update()
                            },
                            tips = listOf("§eClick to pause the current session!"),
                        )
                    },
                )
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
                } " + if (sessionUptime.isPaused()) "§c(PAUSED)" else "",
            )
        }
        lines[CrownOfAvariceLines.TIMEUNTILMAX] = Renderable.horizontal {
            val timeUntilMax = calculateTimeUntilMax()
            addString(
                "§aTime until Max: §b${if (isSessionActive) "Calculating..." else timeUntilMax} " +
                    if (sessionUptime.isPaused()) "§c(PAUSED)" else "",
            )
        }

        lines[CrownOfAvariceLines.COINDIFFERENCE] = Renderable.horizontal {
            val format = coinsDifference ?: "§cnever"
            addString("§aLast coins gained: §6$format")
        }

        lines[CrownOfAvariceLines.SESSIONCOINS] = Renderable.horizontal {
            addString("§aCoins this session: §6${coinsEarned.addSeparators()}")
        }

        lines[CrownOfAvariceLines.SESSIONTIME] = Renderable.horizontal {
            addString("§aSession Time: §b${sessionUptime.getDuration().format()}")
        }

        return formatDisplay(lines)
    }


    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enable

    private fun reset() {
        coinsEarned = 0L
        sessionUptime = Stopwatch()
        coinsDifference = 0L
        update()
    }

    private fun pauseSession() {
        sessionUptime.pause()
    }

    private fun calculateCoinsPerHour(): Double {
        val timeInHours = sessionUptime.getDuration().inPartialHours
        return if (timeInHours > 0) coinsEarned / timeInHours else 0.0
    }

    private fun calculateTimeUntilMax(): String {
        val coinsPerHour = calculateCoinsPerHour()
        if (coinsPerHour == 0.0) return "Forever..."
        val timeUntilMax = ((MAX_AVARICE_COINS - (totalCoins ?: 0)) / coinsPerHour).hours
        return timeUntilMax.format()
    }

}
