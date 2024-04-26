package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningAPI.getCold
import at.hannibal2.skyhanni.data.MiningAPI.inColdIsland
import at.hannibal2.skyhanni.data.MiningAPI.lastColdReset
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.DelayedRun.runDelayed
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MiningNotifications {

    private val ASCENSION_ROPE = "ASCENSION_ROPE".asInternalName().makePrimitiveStack(1)

    enum class MiningNotificationList(val str: String, val notification: String) {
        PICKAXE_ABILITY_READY("§aPickaxe Ability Ready", "§aTEMP ready"),
        PICKAXE_ABILITY_START("§aPickaxe Ability Use", "§aTEMP used"),
        PICKAXE_ABILITY_END("§aPickaxe Ability End", "§aTEMP ended"),
        SCRAP("§9Suspicious Scrap", "§9Suspicious Scrap"),
        GOLDEN_GOBLIN("§6Golden Goblin", "§6Golden Goblin"),
        DIAMOND_GOBLIN("§bDiamond Goblin", "§bDiamond Goblin"),
        COLD("§bCold", "§bCold");

        override fun toString() = str
    }

    enum class MiningAbilities(val type: String, val cooldown: Int) {
        MINING_SPEED("Mining Speed Boost", 120),
        PICKOBULUS("Pickobulus", 110),
        VEIN_SEEKER("Vein Seeker", 60),
        MANIAC_MINER("Maniac Miner", 59),
        GEMSTONE_INFUSION("Gemstone Infusion", 140),
        HAZARDOUS_MINER("Hazardous Miner", 140);

        companion object {
            fun findByType(input: String): MiningAbilities? {
                return entries.find { it.type == input }
            }
        }
    }

    private val patternGroup = RepoPattern.group("mining.notifications")
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
        "§6A §r§bDiamond Goblin §r§6has spawned!",
    )
    private val frostbitePattern by patternGroup.pattern(
        "cold.frostbite",
        "§9§lBRRR! §r§bYou're freezing! All you can think about is getting out of here to a warm campfire\\.\\.\\.",
    )
    private val abilityExpire by patternGroup.pattern(
        "pickaxeability.expire",
        "§cYour (?<type>[\\w ]+) has expired!",
    )
    private val abilityUse by patternGroup.pattern(
        "pickaxeability.start",
        "§aYou used your §r§6(?<type>[\\w ]+) §r§aPickaxe Ability!",
    )

    private val config get() = SkyHanniMod.feature.mining.notifications

    private var hasSentCold = false

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inMiningIsland()) return
        if (!config.enabled) return
        val message = event.message

        abilityUse.matchMatcher(event.message) {
            val type = MiningAbilities.findByType(group("type")) ?: return
            sendNotification(MiningNotificationList.PICKAXE_ABILITY_START, type.type)
            val currentServer = HypixelData.serverId
            runDelayed(type.cooldown.seconds) {
                if (HypixelData.serverId != currentServer) return@runDelayed
                sendNotification(MiningNotificationList.PICKAXE_ABILITY_READY, type.type)
            }
            return
        }
        abilityExpire.matchMatcher(event.message) {
            val type = MiningAbilities.findByType(group("type")) ?: return
            sendNotification(MiningNotificationList.PICKAXE_ABILITY_END, type.type)
            return
        }

        when {
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
        if (IslandType.MINESHAFT.isInIsland() && config.getAscensionRope && config.coldAmount == event.cold) {
            DelayedRun.runDelayed(0.5.seconds) {
                GetFromSackAPI.getFromChatMessageSackItems(ASCENSION_ROPE)
            }
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

    private fun sendNotification(type: MiningNotificationList, extra: String = "") {
        if (!config.notifications.contains(type)) return
        val notification = if (extra != "") {
            type.notification.replace("TEMP", extra)
        } else type.notification
        LorenzUtils.sendTitle(notification, 1500.milliseconds)
        if (config.playSound) SoundUtils.playPlingSound()
    }
}
