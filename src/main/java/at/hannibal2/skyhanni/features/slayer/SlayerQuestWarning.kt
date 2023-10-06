package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class SlayerQuestWarning {
    private val config get() = SkyHanniMod.feature.slayer
    private var needSlayerQuest = false
    private var lastWarning = 0L
    private var currentReason = ""
    private var dirtySidebar = false
    private var hasAutoSlayer = false

    //TODO add check if player has clicked on an item, before mobs around you gets damage

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!(LorenzUtils.inSkyBlock)) return

        val message = event.message

        //died
        if (message == "  §r§c§lSLAYER QUEST FAILED!") {
            needNewQuest("The old slayer quest has failed!")
        }
        if (message == "  §r§5§lSLAYER QUEST STARTED!") {
            needSlayerQuest = false
            hasAutoSlayer = true
            dirtySidebar = true
        }

        //no auto slayer
        if (message.matchRegex("   §r§5§l» §r§7Talk to Maddox to claim your (.+) Slayer XP!")) {
            needNewQuest("You have no Auto-Slayer active!")
        }
        if (message == "  §r§a§lSLAYER QUEST COMPLETE!") {
            needSlayerQuest = false
        }

        if (message == "§aYour Slayer Quest has been cancelled!") {
            needSlayerQuest = false
        }

        //TODO auto slayer disabled bc of no more money in bank or purse
    }

    private fun needNewQuest(reason: String) {
        currentReason = reason
        needSlayerQuest = true
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!(LorenzUtils.inSkyBlock)) return

        if (dirtySidebar && event.repeatSeconds(3)) {
            checkSidebar()
        }
    }

    private fun checkSidebar() {
        var loaded = false

        var slayerQuest = false
        var bossSlain = false
        var slayBoss = false
        var slayerTypeName = ""
        var nextIsType = false
        for (line in ScoreboardData.sidebarLinesFormatted) {
            if (nextIsType) {
                slayerTypeName = line.removeColor()
                nextIsType = false
            }
            if (line == "Slayer Quest") {
                slayerQuest = true
                nextIsType = true
            }
            if (line == "§aBoss slain!") {
                bossSlain = true
            }
            if (line == "§eSlay the boss!") {
                slayBoss = true
            }
            if (line == "§ewww.hypixel.net" || line == "§ewww.alpha.hypixel.net") {
                loaded = true
            }
        }

        if (loaded) {
            dirtySidebar = false
            if (slayerQuest && !needSlayerQuest) {
                if (bossSlain) {
                    if (!hasAutoSlayer) {
                        needNewQuest("You have no Auto-Slayer active!")
                        hasAutoSlayer = false
                    }
                } else if (slayBoss) {
                    needNewQuest("You probably switched the server during an active boss and now hypixel doesn't know what to do.")
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!needSlayerQuest) {
            dirtySidebar = true
        }
    }

    private fun tryWarn() {
        if (!needSlayerQuest) return
        warn("New Slayer Quest!", "Start a new slayer quest! $currentReason")
    }

    private fun warn(titleMessage: String, chatMessage: String) {
        if (!config.questWarning) return
        if (lastWarning + 10_000 > System.currentTimeMillis()) return

        lastWarning = System.currentTimeMillis()
        LorenzUtils.chat("§e[SkyHanni] $chatMessage")

        if (config.questWarningTitle) {
            TitleUtils.sendTitle("§e$titleMessage", 2.seconds)
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!(LorenzUtils.inSkyBlock)) return

        val entity = event.entity
        if (entity.getLorenzVec().distanceToPlayer() < 6 && isSlayerMob(entity)) {
            tryWarn()
        }
    }

    private fun isSlayerMob(entity: EntityLivingBase): Boolean {
        val slayerType = SlayerAPI.getSlayerTypeForCurrentArea() ?: return false

        val activeSlayer = SlayerAPI.getActiveSlayer()

        if (activeSlayer != null) {
            if (slayerType != activeSlayer) {
                val activeSlayerName = activeSlayer.displayName
                val slayerName = slayerType.displayName
                SlayerAPI.latestWrongAreaWarning = System.currentTimeMillis()
                warn(
                    "Wrong Slayer!",
                    "Wrong slayer selected! You have $activeSlayerName selected and you are in an $slayerName area!"
                )
            }
        }

        return slayerType.clazz.isInstance(entity)
    }

}