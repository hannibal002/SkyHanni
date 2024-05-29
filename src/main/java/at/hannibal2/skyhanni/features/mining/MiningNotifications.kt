package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningAPI.getCold
import at.hannibal2.skyhanni.data.MiningAPI.inColdIsland
import at.hannibal2.skyhanni.data.MiningAPI.lastColdReset
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.runDelayed
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object MiningNotifications {

    private val ASCENSION_ROPE = "ASCENSION_ROPE".asInternalName().makePrimitiveStack(1)

    enum class MiningNotificationList(val str: String, val notification: String) {
        MINESHAFT_SPAWN("§bGlacite Mineshaft", "§bMineshaft"),
        SCRAP("§9Suspicious Scrap", "§9Suspicious Scrap"),
        GOLDEN_GOBLIN("§6Golden Goblin", "§6Golden Goblin"),
        DIAMOND_GOBLIN("§bDiamond Goblin", "§bDiamond Goblin"),
        COLD("§bCold", "§bCold");

        override fun toString() = str
    }

    private val patternGroup = RepoPattern.group("mining.notifications")
    private val mineshaftSpawn by patternGroup.pattern(
        "mineshaft.spawn",
        "§5§lWOW! §r§aYou found a §r§bGlacite Mineshaft §r§aportal!"
    )
    private val scrapDrop by patternGroup.pattern(
        "scrapdrop",
        "§6§lEXCAVATOR! §r§fYou found a §r§9Suspicious Scrap§r§f!"
    )
    val goldenGoblinSpawn by patternGroup.pattern(
        "goblin.goldspawn",
        "§6A Golden Goblin has spawned!"
    )
    val diamondGoblinSpawn by patternGroup.pattern(
        "goblin.diamondspawn",
        "§6A §r§bDiamond Goblin §r§6has spawned!"
    )
    private val frostbitePattern by patternGroup.pattern(
        "cold.frostbite",
        "§9§lBRRR! §r§bYou're freezing! All you can think about is getting out of here to a warm campfire\\.\\.\\."
    )

    private val config get() = SkyHanniMod.feature.mining.notifications

    private var hasSentCold = false

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inMiningIsland()) return
        if (!config.enabled) return
        val message = event.message
        when {
            mineshaftSpawn.matches(message) -> sendNotification(MiningNotificationList.MINESHAFT_SPAWN)
            scrapDrop.matches(message) -> sendNotification(MiningNotificationList.SCRAP)
            goldenGoblinSpawn.matches(message) -> sendNotification(MiningNotificationList.GOLDEN_GOBLIN)
            diamondGoblinSpawn.matches(message) -> sendNotification(MiningNotificationList.DIAMOND_GOBLIN)
            frostbitePattern.matches(message) -> {
                if (IslandType.MINESHAFT.isInIsland() && config.getAscensionRope) {
                    runDelayed(0.5.seconds) {
                        GetFromSackAPI.getFromChatMessageSackItems(ASCENSION_ROPE)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onColdUpdate(event: ColdUpdateEvent) {
        if (!inColdIsland()) return
        if (!config.enabled) return
        if (lastColdReset.passedSince() < 1.seconds) return

        if (event.cold >= config.coldThreshold.get() && !hasSentCold) {
            hasSentCold = true
            sendNotification(MiningNotificationList.COLD)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        hasSentCold = false
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.coldThreshold) {
            if (getCold() != config.coldThreshold.get()) hasSentCold = false
        }
    }

    private fun sendNotification(type: MiningNotificationList) {
        if (!config.notifications.contains(type)) return
        LorenzUtils.sendTitle(type.notification, 1500.milliseconds)
        if (config.playSound) SoundUtils.playPlingSound()
    }
}
