package at.hannibal2.skyhanni.features.misc.limbo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.features.commands.LimboCommands
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class LimboTimeTracker {
    private val storage get() = ProfileStorageData.playerSpecific?.limbo
    private val config get() = SkyHanniMod.feature.misc

    private var limboJoinTime = SimpleTimeMark.farPast()
    private var inLimbo = false
    private var inFakeLimbo = false
    private var shownPB = false
    private var oldPB: Duration = 0.seconds
    private var userLuck: Float = 0.0F
    private val userLuckMultiplier = 0.000810185F
    private val fireMultiplier = 1.01F
    private var onFire = false

    private val bedwarsLobbyLimbo = AxisAlignedBB(-662.0, 43.0, -76.0, -619.0, 86.0, -27.0)

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message == "§cYou are AFK. Move around to return from AFK." || event.message == "§cYou were spawned in Limbo.") {
            limboJoinTime = SimpleTimeMark.now()
            inLimbo = true
            LimboCommands.enterLimbo(limboJoinTime)
            onFire = Minecraft.getMinecraft().thePlayer.isBurning
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val playtime = storage?.playtime ?: 0
        val pb = storage?.personalBest ?: 0
        if (playtime < pb) {
            storage?.playtime = pb
            ChatUtils.debug("Setting limboPlaytime = limboTimePB, since limboPlaytime was lower.")
        }
    }

    @SubscribeEvent
    fun catchPlaytime(event: MessageSendToServerEvent) {
        if (event.message.startsWith("/playtime") && inLimbo) {
            event.isCanceled
            LimboCommands.printPlaytime(true)
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        val personalBest = storage?.personalBest ?: 0
        if (inLimbo && !shownPB && limboJoinTime.passedSince() >= personalBest.seconds && personalBest != 0) {
            shownPB = true
            oldPB = personalBest.seconds
            ChatUtils.chat("§d§lPERSONAL BEST§f! You've surpassed your previous record of §e$oldPB§f!")
            ChatUtils.chat("§fKeep it up!")
        }
        val lobbyName: String? = HypixelData.locrawData?.get("lobbyname")?.asString
        if (lobbyName.toString().startsWith("bedwarslobby")) {
            if (bedwarsLobbyLimbo.isPlayerInside()) {
                if (inFakeLimbo) return
                limboJoinTime = SimpleTimeMark.now()
                inLimbo = true
                LimboCommands.enterLimbo(limboJoinTime)
                inFakeLimbo = true
            }
            else {
                if (inLimbo) {
                    leaveLimbo()
                    inFakeLimbo = false
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!inLimbo) return
        leaveLimbo()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inLimbo) return
        if (LorenzUtils.inSkyBlock) {
            leaveLimbo()
            return
        }
        val duration = limboJoinTime.passedSince().format()
        config.showTimeInLimboPosition.renderString("§eIn limbo since §b$duration", posLabel = "Limbo Time Tracker")
    }

    private fun leaveLimbo() {
        inLimbo = false
        if (!isEnabled()) return
        val passedSince = limboJoinTime.passedSince()
        val duration = passedSince.format()
        val currentPB = config.limboTimePB.seconds
        val oldLuck = storage?.userLuck ?: 0f
        if (passedSince > currentPB) {
            oldPB = currentPB
            config.limboTimePB = passedSince.toInt(DurationUnit.SECONDS)
            userLuck = (config.limboTimePB * userLuckMultiplier).round(2)
            if (onFire) userLuck *= fireMultiplier
            ChatUtils.chat("§fYou were in Limbo for §e$duration§f! §d§lPERSONAL BEST§r§f!")
            ChatUtils.chat("§fYour previous Personal Best was §e$oldPB.")
        } else ChatUtils.chat("§fYou were in Limbo for §e$duration§f.")
        if (userLuck > oldLuck) {
            if (onFire) {
                ChatUtils.chat("§fYour §aPersonal Bests§f perk is now granting you §a+${userLuck.round(2)}§c✴ §aSkyHanni User Luck§f! ")
            } else {
                ChatUtils.chat("§fYour §aPersonal Bests§f perk is now granting you §a+${userLuck.round(2)}✴ SkyHanni User Luck§f!")
            }
            storage?.userLuck = userLuck
        }
        storage?.playtime = storage?.playtime?.plus(passedSince.toInt(DurationUnit.SECONDS)) ?: 0
        onFire = false
        shownPB = false
    }

//     @SubscribeEvent
//     fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
//         event.move(-1, oldPath = "config.limboTimePB", newPath = "storage.personalBest")
//     }

    fun isEnabled() = config.showTimeInLimbo
}
