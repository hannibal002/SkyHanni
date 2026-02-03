package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

@SkyHanniModule
object BeaconPower {

    private val storage get() = ProfileStorageData.profileSpecific?.beaconPower
    private val config get() = SkyHanniMod.feature.gui

    private val group = RepoPattern.group("gui.beaconpower-no-color")

    // TODO add regex tests
    private val deactivatedPattern by group.pattern(
        "deactivated",
        "Beacon Deactivated - No Power Remaining",
    )
    private val timeRemainingPattern by group.pattern(
        "time",
        "Power Remaining: (?<time>.+)",
    )
    private val boostedStatPattern by group.pattern(
        "stat",
        "Current Stat: (?<stat>.+)",
    )
    private val noBoostedStatPattern by group.pattern(
        "nostat",
        "TODO",
    )

    private var expiryTime: SimpleTimeMark
        get() = storage?.beaconPowerExpiryTime ?: SimpleTimeMark.farPast()
        set(value) {
            storage?.beaconPowerExpiryTime = value
        }

    private var stat: Component?
        get() = storage?.boostedStat
        set(value) {
            storage?.boostedStat = value
        }

    private var display: Component = Component.empty()

    private const val BEACON_POWER_SLOT = 22
    private const val STATS_SLOT = 23

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (event.inventoryName != "Beacon") return
        val items = event.inventoryItems

        items[BEACON_POWER_SLOT]?.let { item ->
            item.getLoreComponent().forEach {
                if (deactivatedPattern.matches(it)) {
                    expiryTime = SimpleTimeMark.farPast()
                    return@let
                }
                timeRemainingPattern.matchMatcher(it) {
                    val duration = TimeUtils.getDuration(group("time"))
                    expiryTime = SimpleTimeMark.now() + duration
                    return@let
                }
            }
        }

        items[STATS_SLOT]?.let { item ->
            item.getLoreComponent().forEach {
                if (noBoostedStatPattern.matches(it)) {
                    stat = null
                    return@let
                }
                boostedStatPattern.matchMatcher(it) {
                    stat = it.replace("Current Stat: ", "")
                    return@let
                }
            }
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.beaconPowerPosition.renderRenderable(Renderable.text(display), posLabel = "Beacon Power")
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        display = drawDisplay()
    }

    private fun drawDisplay(): Component = componentBuilder {
        append("§eBeacon: ")
        if (expiryTime.isInPast()) {
            append("§cNot active")
        } else {
            append("§b${expiryTime.timeUntil().format(maxUnits = 2)}")
            if (config.beaconPowerStat) {
                append {
                    append(" (")
                    append(stat ?: Component.literal("§cNo stat"))
                    append(")")
                    withColor(ChatFormatting.GRAY)
                }
            }
        }
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = Component.empty()
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.beaconPower && !SkyBlockUtils.isBingoProfile
}
