package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.features.commands.LimboCommands
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class LimboTimeTracker {
    private val config get() = SkyHanniMod.feature.misc

    private var limboJoinTime = SimpleTimeMark.farPast()
    private var inLimbo = false
    private var inFakeLimbo = false
    private var shownPB = false
    private var oldPB: Duration = 0.seconds
    private var userLuck: Double = 0.0
    private val userLuckMultiplier = 0.000810185

    private val bedwarsLobbyLimbo = AxisAlignedBB(-662.0, 43.0, -76.0, -619.0, 86.0, -27.0)

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message == "§cYou are AFK. Move around to return from AFK." || event.message == "§cYou were spawned in Limbo.") {
            limboJoinTime = SimpleTimeMark.now()
            inLimbo = true
            LimboCommands.enterLimbo(limboJoinTime)
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        if (config.limboPlaytime < config.limboTimePB) {
            config.limboPlaytime = config.limboTimePB
            LorenzUtils.debug("Setting limboPlaytime = limboTimePB, since limboPlaytime was lower.")
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
        if (inLimbo && !shownPB && limboJoinTime.passedSince() >= config.limboTimePB.seconds && config.limboTimePB != 0) {
            shownPB = true
            oldPB = config.limboTimePB.seconds
            LorenzUtils.chat("§d§lPERSONAL BEST§f! You've surpassed your previous record of §e$oldPB§f!")
            LorenzUtils.chat("§fKeep it up!")
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
        if (passedSince > currentPB) {
            oldPB = currentPB
            config.limboTimePB = passedSince.toInt(DurationUnit.SECONDS)
            userLuck = config.limboTimePB * userLuckMultiplier
            if (passedSince.toInt(DurationUnit.SECONDS) == config.limboTimePB) { //need to come up with a good message for this
                LorenzUtils.chat("§fYou were in Limbo for §e$duration§f! §d§lPERSONAL BEST§r§f!")
                LorenzUtils.chat("§fYour previous Personal Best was §e$oldPB.")
                LorenzUtils.chat("§fYour §aPersonal Bests§f perk is now granting you §a+${userLuck.round(2)}✴ SkyHanni User Luck§f!")
            } else {
                LorenzUtils.chat("§fYou were in Limbo for §e$duration§f! §d§lPERSONAL BEST§r§f!")
                LorenzUtils.chat("§fYour previous Personal Best was §e$oldPB.")
                LorenzUtils.chat("§fYour §aPersonal Bests§f perk is now granting you §a+${userLuck.round(2)}✴ SkyHanni User Luck§f!")
            }
        } else LorenzUtils.chat("§fYou were in Limbo for §e$duration§f.")
        config.limboPlaytime += passedSince.toInt(DurationUnit.SECONDS)
        shownPB = false
    }

    fun isEnabled() = config.showTimeInLimbo
}
/* TODO: add playtime to this
chest name: 'Detailed /playtime'
index: 4
internalName:NONE
display name: '§aPlaytime Breakdown'
minecraft id: 'minecraft:clock'
lore:
 '§b866.3 hours §7on Private Island'
 '§b530.1 hours §7on Garden'
 '§b490.9 hours §7on Dungeon'
 '§b391.5 hours §7on Crystal Hollows'
 '§b358.8 hours §7on Dwarven Mines'
 '§b352 hours §7on Hub'
 '§b228.9 hours §7on Dungeon Hub'
 '§b206.8 hours §7on The End'
 '§b105.4 hours §7on Crimson Isle'
 '§b72.2 hours §7on Spider's Den'
 '§b50.3 hours §7on Jerry's Workshop'
 '§b49 hours §7on The Park'
 '§b32.9 hours §7on The Farming Islands'
 '§b30 hours §7on The Rift'
 '§b11.2 hours §7on Deep Caverns'
 '§b2.3 hours §7on Gold Mine'
 '§b2.3 hours §7on Kuudra'
 '§b1.3 hours §7on Mushroom Desert'
 '§a59 minutes §7on Dark Auction'
 '§7Totalling §b3,783.22 hours §7on this profile!'

getTagCompound
  display:
    Name: "§aPlaytime Breakdown"
 */
