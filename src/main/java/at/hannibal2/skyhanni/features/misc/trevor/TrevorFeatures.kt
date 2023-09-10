package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.seconds


class TrevorFeatures {
    private val trapperPattern =
        "\\[NPC] Trevor: You can find your (?<rarity>.*) animal near the (?<location>.*).".toPattern()
    private val talbotPatternAbove =
        "The target is around (?<height>.*) blocks above, at a (?<angle>.*) degrees angle!".toPattern()
    private val talbotPatternBelow =
        "The target is around (?<height>.*) blocks below, at a (?<angle>.*) degrees angle!".toPattern()
    private val locationPattern = "Zone: (?<zone>.*)".toPattern()
    private var timeUntilNextReady = 0
    private var trapperReady: Boolean = true
    private var currentStatus = TrapperStatus.READY
    private var currentLabel = "§2Ready"
    private var trapperID: Int = 56
    private var backupTrapperID: Int = 17
    private var timeLastWarped: Long = 0

    private val config get() = SkyHanniMod.feature.misc.trevorTheTrapper

    init {
        fixedRateTimer(name = "skyhanni-update-trapper", period = 1000L) {
            Minecraft.getMinecraft().addScheduledTask {
                try {
                    if (config.trapperSolver) {
                        if (onFarmingIsland()) {
                            updateTrapper()
                            TrevorSolver.findMob()
                        }
                    }
                } catch (error: Throwable) {
                    CopyErrorCommand.logError(error, "Encountered an error when updating the trapper solver")
                }
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!onFarmingIsland()) return
        if (event.message == "§aReturn to the Trapper soon to get a new animal to hunt!") {
            TrevorSolver.resetLocation()
            if (config.trapperMobDiedMessage) {
                TitleUtils.sendTitle("§2Mob Died ", 5.seconds)
                SoundUtils.playBeepSound()
            }
            trapperReady = true
            TrevorSolver.mobLocation = CurrentMobArea.NONE
            if (timeUntilNextReady <= 0) {
                currentStatus = TrapperStatus.READY
                currentLabel = "§2Ready"
            } else {
                currentStatus = TrapperStatus.WAITING
                currentLabel = if (timeUntilNextReady == 1) "§31 second left" else "§3$timeUntilNextReady seconds left"
            }
            TrevorSolver.mobLocation = CurrentMobArea.NONE
        }

        var matcher = trapperPattern.matcher(event.message.removeColor())
        if (matcher.matches()) {
            timeUntilNextReady = if (GardenCropSpeed.finneganPerkActive()) 16 else 21
            currentStatus = TrapperStatus.ACTIVE
            currentLabel = "§cActive Quest"
            trapperReady = false
        }

        matcher = talbotPatternAbove.matcher(event.message.removeColor())
        if (matcher.matches()) {
            val height = matcher.group("height").toInt()
            TrevorSolver.findMobHeight(height, true)
        }

        matcher = talbotPatternBelow.matcher(event.message.removeColor())
        if (matcher.matches()) {
            val height = matcher.group("height").toInt()
            TrevorSolver.findMobHeight(height, false)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!config.trapperCooldownGui) return
        if (!onFarmingIsland()) return

        val cooldownMessage = if (timeUntilNextReady <= 0) "Trapper Ready"
        else if (timeUntilNextReady == 1) "1 second left"
        else "$timeUntilNextReady seconds left"

        config.trapperCooldownPos.renderString(
            "${currentStatus.colorCode}Trapper Cooldown: $cooldownMessage",
            posLabel = "Trapper Cooldown GUI"
        )
    }


    private fun updateTrapper() {
        timeUntilNextReady -= 1
        if (trapperReady && timeUntilNextReady > 0) {
            currentStatus = TrapperStatus.WAITING
            currentLabel = if (timeUntilNextReady == 1) "§31 second left" else "§3$timeUntilNextReady seconds left"
        }

        if (timeUntilNextReady <= 0 && trapperReady) {
            if (timeUntilNextReady == 0) {
                TitleUtils.sendTitle("§2Trapper Ready", 3.seconds)
                SoundUtils.playBeepSound()
            }
            currentStatus = TrapperStatus.READY
            currentLabel = "§2Ready"
        }

        var found = false
        var active = false
        val previousLocation = TrevorSolver.mobLocation
        for (line in TabListData.getTabList()) {
            val formattedLine = line.removeColor().drop(1)
            if (formattedLine.startsWith("Time Left: ")) {
                trapperReady = false
                currentStatus = TrapperStatus.ACTIVE
                currentLabel = "§cActive Quest"
                active = true
            }

            CurrentMobArea.entries.firstOrNull { it.location == formattedLine }?.let {
                TrevorSolver.mobLocation = it
                found = true
            }
            locationPattern.matchMatcher(formattedLine) {
                val zone = group("zone")
                TrevorSolver.mobLocation = CurrentMobArea.entries.firstOrNull { it.location == zone }
                    ?: CurrentMobArea.NONE
                found = true
            }
        }
        if (!found) TrevorSolver.mobLocation = CurrentMobArea.NONE
        if (!active) {
            trapperReady = true
        }
        if (TrevorSolver.mobCoordinates != LorenzVec(0.0, 0.0, 0.0) && active) {
            TrevorSolver.mobLocation = previousLocation
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!onFarmingIsland()) return
        var entityTrapper = Minecraft.getMinecraft().theWorld.getEntityByID(trapperID)
        if (entityTrapper !is EntityLivingBase) entityTrapper =
            Minecraft.getMinecraft().theWorld.getEntityByID(backupTrapperID)
        if (entityTrapper is EntityLivingBase) {
            if (config.trapperTalkCooldown) {
                RenderLivingEntityHelper.setEntityColor(
                    entityTrapper,
                    currentStatus.color
                ) { config.trapperTalkCooldown }
                entityTrapper.getLorenzVec().let {
                    if (it.distanceToPlayer() < 15) {
                        event.drawString(it.add(0.0, 2.23, 0.0), currentLabel)
                    }
                }
            }
        }

        if (config.trapperSolver) {
            var location = TrevorSolver.mobLocation.coordinates
            if (TrevorSolver.mobLocation == CurrentMobArea.NONE) return
            if (TrevorSolver.averageHeight != 0.0) {
                location = LorenzVec(location.x, TrevorSolver.averageHeight, location.z)
            }
            if (TrevorSolver.mobLocation == CurrentMobArea.FOUND) {
                location = TrevorSolver.mobCoordinates
                event.drawWaypointFilled(location.add(0, -2, 0), LorenzColor.GREEN.toColor(), true, true)
                event.drawDynamicText(location.add(0, 1, 0), TrevorSolver.mobLocation.location, 1.5)
            } else {
                event.drawWaypointFilled(location, LorenzColor.GOLD.toColor(), true, true)
                event.drawDynamicText(location.add(0, 1, 0), TrevorSolver.mobLocation.location, 1.5)
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!config.warpToTrapper) return
        if (!onFarmingIsland()) return
        if (!Keyboard.getEventKeyState()) return
        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (config.keyBindWarpTrapper != key) return

        Minecraft.getMinecraft().currentScreen?.let {
            if (it !is GuiInventory && it !is GuiChest && it !is GuiEditSign) return
        }
        if (NEUItems.neuHasFocus()) return

        if (System.currentTimeMillis() - timeLastWarped < 3000) return
        LorenzUtils.sendCommandToServer("warp trapper")
        timeLastWarped = System.currentTimeMillis()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!inTrapperDen()) return
        if (!config.trapperTalkCooldown) return
        val entity = event.entity
        if (entity is EntityArmorStand) {
            if (entity.name == "§e§lCLICK") {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        TrevorSolver.resetLocation()
        currentStatus = TrapperStatus.READY
        currentLabel = "§2Ready"
    }

    enum class TrapperStatus(val color: Int, val colorCode: String) {
        READY(LorenzColor.DARK_GREEN.toColor().withAlpha(75), "§2"),
        WAITING(LorenzColor.DARK_AQUA.toColor().withAlpha(75), "§3"),
        ACTIVE(LorenzColor.DARK_RED.toColor().withAlpha(75), "§4"),
    }

    private fun onFarmingIsland() =
        LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.THE_FARMING_ISLANDS

    private fun inTrapperDen() = ScoreboardData.sidebarLinesFormatted.contains(" §7⏣ §bTrapper's Den")
}
