package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import io.github.moulberry.notenoughupdates.NEUOverlay
import io.github.moulberry.notenoughupdates.overlays.AuctionSearchOverlay
import io.github.moulberry.notenoughupdates.overlays.BazaarSearchOverlay
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import kotlin.concurrent.fixedRateTimer

class TrevorFeatures {
    private val trapperPattern = "\\[NPC] Trevor: You can find your (?<rarity>.*) animal near the (?<location>.*).".toPattern()
    private val talbotPatternAbove = "The target is around (?<height>.*) blocks above, at a (?<angle>.*) degrees angle!".toPattern()
    private val talbotPatternBelow = "The target is around (?<height>.*) blocks below, at a (?<angle>.*) degrees angle!".toPattern()
    private val locationPattern = "Zone: (?<zone>.*)".toPattern()
    private var timeUntilNextReady = 0
    private var trapperReady: Boolean = true
    private var currentStatus = TrapperStatus.READY
    private var currentLabel = "§2Ready"
    private var trapperID: Int = 56
    private var backupTrapperID: Int = 17
    private var timeLastWarped: Long = 0

    private val config get() = SkyHanniMod.feature.misc

    init {
        fixedRateTimer(name = "skyhanni-update-trapper", period = 1000L) {
            if (onFarmingIsland()) {
                updateTrapper()
                TrevorSolver.findMob()
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!onFarmingIsland()) return
        if (event.message == "§aReturn to the Trapper soon to get a new animal to hunt!") {
            TrevorSolver.resetLocation()
            if (config.trapperMobDiedMessage) {
                TitleUtils.sendTitle("§2Mob Died ", 5_000)
                SoundUtils.playBeepSound()
                trapperReady = true
                TrevorSolver.mobLocation = TrevorSolver.CurrentMobArea.NONE
            }
            if (timeUntilNextReady <= 0) {
                currentStatus = TrapperStatus.READY
                currentLabel = "§2Ready"
            } else {
                currentStatus = TrapperStatus.WAITING
                currentLabel = "§3$timeUntilNextReady seconds left"
            }
        }

        var matcher = trapperPattern.matcher(event.message.removeColor())
        if (matcher.matches()) {
            timeUntilNextReady = 61
            currentStatus = TrapperStatus.ACTIVE
            currentLabel = "§cActive Quest"
            trapperReady = false
        }

        matcher = talbotPatternAbove.matcher(event.message.removeColor())
        if (matcher.matches()) {
            val height = matcher.group("height").toInt()
            TrevorSolver.locateMob(height, true)
        }

        matcher = talbotPatternBelow.matcher(event.message.removeColor())
        if (matcher.matches()) {
            val height = matcher.group("height").toInt()
            TrevorSolver.locateMob(height,  false)
        }
    }

    private fun updateTrapper() {
        timeUntilNextReady -= 1
        if (trapperReady && timeUntilNextReady > 0) {
            currentLabel = "§3$timeUntilNextReady seconds left"
        }

        if (timeUntilNextReady == 0 && trapperReady) {
            TitleUtils.sendTitle("§2Trapper Ready ", 3_000)
            SoundUtils.playBeepSound()
            currentStatus = TrapperStatus.READY
            currentLabel = "§2Ready"
        }
        if (!onFarmingIsland()) return
        var found = false
        var active = false
        for (line in TabListData.getTabList()) {
            val formattedLine = line.removeColor().drop(1)
            if (formattedLine.startsWith("Time Left: ")) active = true
            if (TrevorSolver.CurrentMobArea.values().firstOrNull { it.location == formattedLine } != null) {
                TrevorSolver.mobLocation = TrevorSolver.CurrentMobArea.values().firstOrNull { it.location == formattedLine }!!
                found = true
            }
            val matcher = locationPattern.matcher(formattedLine)
            if (matcher.matches()) {
                val zone = matcher.group("zone")
                TrevorSolver.mobLocation = TrevorSolver.CurrentMobArea.values().firstOrNull { it.location == zone } ?: TrevorSolver.CurrentMobArea.NONE
                found = true
            }
            if (!found) TrevorSolver.mobLocation = TrevorSolver.CurrentMobArea.NONE
            if (active) {
                trapperReady = false
                currentStatus = TrapperStatus.ACTIVE
                currentLabel = "§cActive Quest"
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!onFarmingIsland()) return
        var entityTrapper = Minecraft.getMinecraft().theWorld.getEntityByID(trapperID)
        if (entityTrapper !is EntityLivingBase) entityTrapper = Minecraft.getMinecraft().theWorld.getEntityByID(backupTrapperID)
        if (entityTrapper is EntityLivingBase) {
            if (config.trapperTalkCooldown) {
                RenderLivingEntityHelper.setEntityColor(
                    entityTrapper,
                    currentStatus.color
                ) { config.trapperTalkCooldown }
                entityTrapper.getLorenzVec().let {
                    if (it.distanceToPlayer() < 15) {
                        val text = currentLabel
                        event.drawString(it.add(0.0, 2.23, 0.0), text)
                    }
                }
            }
        }

        if (config.trapperSolver) {
            var location = TrevorSolver.mobLocation.coordinates
            if (TrevorSolver.mobLocation == TrevorSolver.CurrentMobArea.NONE) return
            if (TrevorSolver.mobLocation == TrevorSolver.CurrentMobArea.FOUND) {
                location = TrevorSolver.mobCoordinates
                event.drawWaypointFilled(location.add(0, -1, 0), LorenzColor.GREEN.toColor(), true, true)
                event.drawDynamicText(location.add(0, 2, 0), TrevorSolver.mobLocation.location, 1.5)
            } else {
                event.drawWaypointFilled(location, LorenzColor.GOLD.toColor(), true, true)
                event.drawDynamicText(location.add(0, 1, 0), TrevorSolver.mobLocation.location, 1.5)
            }
        }

    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!config.warpToTrapper) return
        if (!Keyboard.getEventKeyState()) return
        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (config.keyBindWarpTrapper != key) return

        if (!LorenzUtils.inSkyBlock || LorenzUtils.inDungeons || LorenzUtils.inKuudraFight) return

        Minecraft.getMinecraft().currentScreen?.let {
            if (it !is GuiInventory && it !is GuiChest && it !is GuiEditSign) return
        }

        if (NEUOverlay.searchBarHasFocus) return
        if (AuctionSearchOverlay.shouldReplace()) return
        if (BazaarSearchOverlay.shouldReplace()) return
        if (InventoryUtils.inStorage()) return

        if (System.currentTimeMillis() - timeLastWarped < 3000) {
            if (System.currentTimeMillis() - timeLastWarped < 1000) return
            LorenzUtils.chat("§6Command on cooldown, wait a few seconds!")
            return
        }
        LorenzUtils.sendCommandToServer("warp trapper")
        timeLastWarped = System.currentTimeMillis()
    }

    @SubscribeEvent (priority = EventPriority.HIGHEST)
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
    fun onWorldChange(event: WorldEvent.Load) {
        TrevorSolver.resetLocation()
        currentStatus = TrapperStatus.READY
        currentLabel = "§2Ready"
    }

    enum class TrapperStatus(val color: Int) {
        READY(LorenzColor.DARK_GREEN.toColor().withAlpha(75)),
        WAITING( LorenzColor.DARK_AQUA.toColor().withAlpha(75)),
        ACTIVE(LorenzColor.DARK_RED.toColor().withAlpha(75)),
    }

    private fun onFarmingIsland() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.THE_FARMING_ISLANDS
    private fun inTrapperDen() = ScoreboardData.sidebarLinesFormatted.contains(" §7⏣ §bTrapper's Den")
}
