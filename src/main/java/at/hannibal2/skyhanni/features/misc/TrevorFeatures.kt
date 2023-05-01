package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.NEUOverlay
import io.github.moulberry.notenoughupdates.overlays.AuctionSearchOverlay
import io.github.moulberry.notenoughupdates.overlays.BazaarSearchOverlay
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import kotlin.concurrent.fixedRateTimer

class TrevorFeatures {
    private val trapperPattern = "\\[NPC] Trevor: You can find your (?<rarity>.*) animal near the (?<location>.*).".toPattern()
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
            updateTrapper()
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!onFarmingIsland()) return

        if (event.message == "§aReturn to the Trapper soon to get a new animal to hunt!") {
            if (config.trapperMobDiedMessage) {
                TitleUtils.sendTitle("§2Mob Died ", 5_000)
                SoundUtils.playBeepSound()
                trapperReady = true
            }
            if (timeUntilNextReady <= 0) {
                currentStatus = TrapperStatus.READY
                currentLabel = "§2Ready"
            } else {
                currentStatus = TrapperStatus.WAITING
                currentLabel = "§3$timeUntilNextReady seconds left"
            }
        }

        val matcher = trapperPattern.matcher(event.message.removeColor())
        if (matcher.matches()) {
            timeUntilNextReady = 61
            currentStatus = TrapperStatus.ACTIVE
            currentLabel = "§cActive Quest"
            trapperReady = false
        }
    }

    private fun updateTrapper() {
        if (!onFarmingIsland()) return
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
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!onFarmingIsland() or !inTrapperDen()) return
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

        // only work every 3 seconds
        if (System.currentTimeMillis() - timeLastWarped < 3000) {
            if (System.currentTimeMillis() - timeLastWarped < 1000) return
            //spams the chat if you click it between 1-5 seconds, but I also like the functionality of being able to hold the key and the command will run once the cooldown is up
            LorenzUtils.chat("§6Command on cooldown, wait a few seconds!")
            return
        }
        LorenzUtils.sendCommandToServer("warp trapper")
        timeLastWarped = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (config.trapperTalkCooldown && currentStatus == TrapperStatus.READY) {
            if (event.tabList.any { it.startsWith(" §r§fTime Left: §r§a")}) {
                trapperReady = false
                currentStatus = TrapperStatus.ACTIVE
                currentLabel = "§cActive Quest"
            }
        }
    }

    enum class TrapperStatus(val color: Int) {
        READY(LorenzColor.DARK_GREEN.toColor().withAlpha(5)),
        WAITING( LorenzColor.DARK_AQUA.toColor().withAlpha(75)),
        ACTIVE(LorenzColor.DARK_RED.toColor().withAlpha(5)),
    }

    private fun onFarmingIsland() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.THE_FARMING_ISLANDS
    private fun inTrapperDen() = ScoreboardData.sidebarLinesFormatted.contains(" §7⏣ §bTrapper's Den")
}
